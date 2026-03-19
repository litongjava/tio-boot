# tio-websocket-client

## 1. 依赖添加
首先，您需要在项目的`pom.xml`文件中添加以下依赖，以使用`tio-websocket-client`库。

```xml
<dependency>
  <groupId>com.litongjava</groupId>
  <artifactId>tio-websocket-client</artifactId>
  <version>3.7.3.v20210706</version>
</dependency>
```

这个依赖是WebSocket客户端的核心，提供了建立连接、发送和接收消息等功能。

## 2. WebSocket客户端测试 

### 主要组件和流程:

1. **消息发送跟踪:** 使用`ConcurrentHashMap`来存储和跟踪每条消息的发送状态。
2. **消息确认机制:** 使用RxJava的`Subject`和`PublishSubject`来处理消息确认。当所有消息都确认发送后，会打印出“All sent success!”。
3. **WebSocket客户端配置:** 
    - `onOpen`：连接打开时的回调。
    - `onMessage`：接收到消息时的回调。更新消息状态，并打印接收到的消息。
    - `onClose`：连接关闭时的回调。
    - `onError`：出现错误时的回调。
    - `onThrows`：异常处理。
4. **连接建立:** 使用`WsClient.create`创建WebSocket客户端，并通过`connect`方法建立连接。
5. **消息发送:** 循环发送一定数量的消息，并打印发送状态。
```
package demo;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import com.litongjava.tio.websocket.client.WebSocket;
import com.litongjava.tio.websocket.client.WsClient;
import com.litongjava.tio.websocket.client.config.WsClientConfig;
import com.litongjava.tio.websocket.client.event.CloseEvent;
import com.litongjava.tio.websocket.client.event.ErrorEvent;
import com.litongjava.tio.websocket.client.event.MessageEvent;
import com.litongjava.tio.websocket.client.event.OpenEvent;
import com.litongjava.tio.websocket.common.WsPacket;

import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

public class TioWebSocketDemo {

  public static void main(String[] args) throws Exception {
    Map<Long, Boolean> sent = new ConcurrentHashMap<>();
    int total = 1000;
    String uri = "ws://localhost/hello";

    // onNext
    io.reactivex.functions.Consumer<? super List<Object>> onNext = x -> {
      Boolean all = sent.values().stream().reduce(true, (p, c) -> p && c);
      if (all) {
        System.out.println("All sent success! ");
      }
    };

    // complete
    Subject<Object> complete = PublishSubject.create().toSerialized();
    // subscribe
    complete.buffer(total).subscribe(onNext);

    // wsClientConfig
    Consumer<OpenEvent> onOpen = e -> System.out.println("opened");

    Consumer<MessageEvent> onMessage = e -> {
      WsPacket data = e.data;
      Long id = data.getId();
      String wsBodyText = data.getWsBodyText();
      sent.put(id, true);
      System.out.println("recv: " + wsBodyText);
      complete.onNext(id);
    };

    Consumer<CloseEvent> onClose = e -> System.out.printf("on close: %d, %s, %s\n", e.code, e.reason, e.wasClean);
    Consumer<ErrorEvent> onError = e -> System.out.println(String.format("on error: %s", e.msg));
    Consumer<Throwable> onThrows = Throwable::printStackTrace;

    // wsClientConfig
    WsClientConfig wsClientConfig = new WsClientConfig(onOpen, onMessage, onClose, onError, onThrows);

    // create
    WsClient echo = WsClient.create(uri, wsClientConfig);

    // connect
    WebSocket ws = echo.connect();

    // sent
    for (int i = 0; i < total; i++) {
      ws.send("" + i);
      System.out.println("sent: " + i);
    }
  }
}

```