# HTTP 方法路由（2.1.6）

## 默认注册

从 2.1.6 开始，`DefaultHttpRequestRouter.add(path, handler)` 默认注册 GET、POST、PUT、DELETE 四种方法，不再匹配所有 HTTP 方法。精确路径、通配符和路径模板使用相同规则。

```java
HttpRequestRouter router = TioBootServer.me().getRequestRouter();
router.add("/api/items", itemsHandler);
router.add("/api/items/{id}", itemHandler);
```

HEAD 与 OPTIONS 默认由框架支持，无需额外注册。HEAD 自动使用 GET 处理器生成资源响应头，编码器不发送响应体；OPTIONS 自动生成方法列表，不执行默认业务处理器。PATCH 等其他方法需要显式注册。

## HEAD 的协议语义

根据 [RFC 9110 §9.3.2](https://www.rfc-editor.org/rfc/rfc9110.html#section-9.3.2)，HEAD 与 GET 的语义一致，但禁止发送响应内容。因此，框架不能简单地对任意 HEAD 返回空的 200：状态码、Content-Type、ETag 等取决于具体资源。默认复用 GET 是实现方式，协议不强制调用 GET 处理器。请求方法仍保持 HEAD，正常经过资源拦截器。

如果生成 GET 内容成本较高，可以注册 `router.head(path, handler)`，只查询并返回资源元数据。显式 HEAD 优先于 GET 回退。HEAD 的 Content-Length 若存在，必须是对应 GET 的内容长度；未知时可省略，不能仅因为 HEAD 无响应体就填 0。框架保留显式设置的长度，避免重复生成；文件响应也只发送头部。

仅注册 POST 的路径不会回退到 POST 来处理 HEAD，而是返回 405。GET/HEAD 应保持只读语义；登录等有状态变化的操作应使用 `post(...)`。

## 单独注册方法

```java
router.post("/api/login/account", loginHandler::account);
router.patch("/api/items/{id}", updateHandler);
router.options("/api/login/account", request -> {
  HttpResponse response = new HttpResponse(request);
  response.setStatus(204);
  CORSUtils.enableCORS(response);
  return response;
});
```

`router.options(path, handler)` 等价于 `router.add(HttpMethod.OPTIONS, path, handler)`。其他方法也可通过带 `HttpMethod` 的重载注册；带 metadata 的重载仍然可用。

`router.head(path, handler)` 等价于 `router.add(HttpMethod.HEAD, path, handler)`。显式 HEAD 处理器只需构造元数据响应，框架仍负责禁止发送响应体。

显式方法注册优先于默认注册，与注册先后顺序无关。重复调用 `add(path, handler)` 替换该路径的默认处理器，保留显式方法注册。重复显式注册同一方法和路径会抛出 `IllegalArgumentException`。

`allRoutes()` 将默认注册展示为四条带具体方法的定义。`find(path)` 和 `all()` 保留仅查看 `add(path, handler)` 注册的兼容行为；处理实际 HTTP 请求应使用 `match(request)` 或 `resolve(request)`。

## OPTIONS 与 CORS

Tio-Boot 的全局 CORS 开关仍默认关闭。在启动应用的 `app.properties` 中配置并重启：

```properties
server.http.response.cors.enable=true
```

HTTP OPTIONS 与浏览器的 CORS 授权是两个层面。依据 [RFC 9110 §9.3.7](https://www.rfc-editor.org/rfc/rfc9110.html#section-9.3.7)，OPTIONS 查询目标的通信选项，成功响应应提供适用的能力信息（例如 Allow），并不要求开启 CORS。

- 对内置方法路由已知的路径，未显式注册 OPTIONS 时，框架在业务拦截器之前直接返回 `204 + Allow`，与 CORS 开关无关。
- `OPTIONS *` 查询服务器整体能力，框架返回 204，Allow 汇总该方法路由器的注册方法（以及自动支持的 HEAD/OPTIONS）。这不是某个具体资源的方法清单。
- 匹配显式 OPTIONS 路由时，执行正常拦截器和该路由处理器。显式处理器可自行选择状态码、响应头和可选的能力说明响应体。
- 开启全局 CORS 后，自动 OPTIONS 响应额外添加 CORS 头，Access-Control-Allow-Methods 与该响应的 Allow 一致。关闭时不自动授权跨域；需要跨域的显式处理器可自行添加 CORS 头。
- 未知路径交给其余路由或静态资源处理流程；最终找不到资源则返回 404，不再因为打开 CORS 就无条件返回成功。其他不支持的方法返回 405 和 Allow。

OPTIONS 响应不允许作为普通 HTTP 缓存使用；CORS 的 Access-Control-Max-Age 是浏览器预检缓存的独立机制。编码器确保 204 不包含响应体、Content-Length 或 Transfer-Encoding；这是 [RFC 9110 §8.6](https://www.rfc-editor.org/rfc/rfc9110.html#section-8.6) 的要求。204 是框架选择的无内容成功状态，协议也允许有能力说明内容的其他成功响应。

自动 Allow 包含已注册的方法，以及 GET 对应的 HEAD 和框架支持的 OPTIONS；这不表示 OPTIONS 会执行 GET 或默认业务处理器。

## 从 2.1.5 迁移

将应用依赖的 tio-boot/tio-http-server 及关联 t-io 模块升级到 2.1.6。原来依赖 `add(path, handler)` 接收 PATCH 或定制 OPTIONS 的代码，需要补充对应的显式注册；普通 OPTIONS 能力查询无需注册。只使用 GET、POST、PUT、DELETE 的默认处理器无需改动。

此更改针对内置 `DefaultHttpRequestRouter`，不改变 Groovy、函数路由、控制器或第三方路由实现的注册规则。登录接口仍建议使用 `post(...)` 限制方法，并校验空请求体。
