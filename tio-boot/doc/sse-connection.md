# 流式响应（SSE）与连接关闭

## 结论

流式响应（`text/event-stream`）在客户端声明 `Connection: close` 时同样必须保持连接。框架已经通过
`HttpResponse#addServerSentEventsHeader()` 把这类响应标记为流式并置为长连接，但 `prepareConnection()`
过去仍会因为请求头里的 `close` 把连接改成关闭，导致传输层在写完响应头后立刻断链，后续所有分片被丢弃。

## 症状

- 浏览器或反向代理收到的只有响应头，正文一个字节都没有。
- 服务端日志出现 `cancel send data ... closed:true, removed:true`，即后续分片写到了已关闭的通道。
- 直连后端（客户端不发 `Connection: close`）一切正常，经过代理就完全无数据。
- 常见的触发者：开发服务器代理、nginx 默认的上游连接配置，它们都会向上游发送 `Connection: close`。

## 分片封装本身没有问题

用字节级抓包可以看到响应是标准的分块传输，头和终止块都齐全：

```text
HTTP/1.1 200 OK
transfer-encoding:chunked
content-type:text/event-stream;charset=utf-8
x-accel-buffering:no

47
event:error
data:{"message":"...","code":400}

0
```

因此问题不在分片，而在连接是否被提前关闭。

## 修改内容

`prepareConnection()` 在判断是否需要关闭连接时跳过「客户端要求关闭」这一项，前提是该响应已被标记为流式：

```java
/** Resolve headers and the transport close flag before queuing or encoding the response. */
public void prepareConnection() {
  // A streaming response (server-sent events) is still being written when its headers leave the
  // server, so the client's "Connection: close" cannot be honored yet: the transport would drop
  // the channel right after the headers and every later chunk would be lost. The application ends
  // such a response itself and closes the connection at that point, which still satisfies a client
  // that asked for close. An explicit "Connection: close" in the response headers keeps closing.
  boolean close = !isKeepConnection()
      || (!isStream() && request != null && containsConnectionOption(request.getConnection(), "close"));
  for (Entry<HeaderName, HeaderValue> header : headers.entrySet()) {
    if ("connection".equalsIgnoreCase(header.getKey().name)
        && containsConnectionOption(header.getValue().toString(), "close")) {
      close = true;
    }
  }
  if (close) {
    setKeepConnection(false);
    // ... 与原来一致：移除连接相关响应头并写入 Connection: close
  }
}
```

要点：

- 只有 `addServerSentEventsHeader()` 标记过的流式响应受影响，普通响应照旧遵守客户端的关闭要求。
- 响应里显式写入 `Connection: close` 时仍然关闭，应用始终可以覆盖这一行为。
- 流式响应结束时由应用关闭连接（`SseEmitter#closeChunkConnection`），客户端的关闭意图在那一刻得到满足。

## 回归测试

`tio-http-common` 的 `HttpStreamConnectionTest` 固定三种情况，避免以后被改回去：

| 用例 | 期望 |
| --- | --- |
| 请求带 `Connection: close`，响应是流式 | 保持长连接，响应头为 `Connection: keep-alive` |
| 请求带 `Connection: close`，响应是普通响应 | 关闭连接，响应头为 `Connection: close` |
| 请求带 `Connection: close`，流式响应自己声明 `Connection: close` | 关闭连接（应用显式意图优先） |

本机验证方式（模块的 surefire 会选中 TestNG，JUnit 用例改用 JUnitCore 直接跑）：

```bash
mvn -o -f t-io/pom.xml -pl tio-http-common test-compile
java -cp "tio-http-common/target/classes;tio-http-common/target/test-classes;<依赖>" \
  org.junit.runner.JUnitCore nexus.io.tio.http.common.HttpStreamConnectionTest
```

## 应用侧配合

即使框架已经修好，部署时仍有两点值得确认：

- 反向代理要让 SSE 走 HTTP/1.1 并保持上游长连接，例如 nginx 的 `proxy_http_version 1.1;` 与
  `proxy_set_header Connection "";`，同时保留 `proxy_buffering off;`（响应已带 `X-Accel-Buffering: no`）。
- 开发服务器代理若不发送 `Connection: close`，则无需依赖本修改也能正常工作。

## 验证方式

用同一个请求分别直连后端和经过代理，比较是否有正文：

```bash
# 直连，客户端要求关闭连接
curl -i -N -X POST "http://127.0.0.1:10060/api/application/chat_message/1" \
  -H "Authorization: Bearer <token>" -H "Connection: close" \
  -H "Content-Type: application/json" -d '{"message":"hi"}'

# 经过开发服务器代理
curl -i -N -X POST "http://127.0.0.1:5173/api/application/chat_message/1" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" -d '{"message":"hi"}'
```

两种情况都应先看到 `text/event-stream` 响应头，随后收到分片数据；修改前经代理的那次只有响应头。
