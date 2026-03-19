package demo;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import com.litongjava.tio.websocket.client.WebSocket;
import com.litongjava.tio.websocket.client.WebsocketClient;
import com.litongjava.tio.websocket.client.config.WebsocketClientConfig;
import com.litongjava.tio.websocket.client.event.CloseEvent;
import com.litongjava.tio.websocket.client.event.ErrorEvent;
import com.litongjava.tio.websocket.client.event.MessageEvent;
import com.litongjava.tio.websocket.client.event.OpenEvent;
import com.litongjava.tio.websocket.common.WebSocketPacket;

import io.reactivex.functions.Consumer;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

public class TioWebSocketDemo {

  public static void main(String[] args) throws Exception {
    Map<Long, Boolean> sent = new ConcurrentHashMap<>();
    int total = 1000;
    String uri = "ws://127.0.0.1:9326/hello";

    // 用于等待连接建立
    CountDownLatch openLatch = new CountDownLatch(1);
    // 用于等待所有消息接收完毕
    CountDownLatch latch = new CountDownLatch(total);

    // 定义接收消息的处理逻辑
    Consumer<? super List<Object>> onNext = new Consumer<List<Object>>() {
      @Override
      public void accept(List<Object> x) throws Exception {
        Boolean all = sent.values().stream().reduce(true, (p, c) -> p && c);
        if (all) {
          System.out.println("All sent success! ");
          // 不需要在这里减少计数，因为我们在接收消息的回调中已经处理了
        }
      }
    };

    // 创建一个用于消息处理的主题
    Subject<Object> complete = PublishSubject.create().toSerialized();
    // 订阅消息处理逻辑
    complete.buffer(total).subscribe(onNext);

    // 定义 WebSocket 事件处理函数
    java.util.function.Consumer<OpenEvent> onOpen = new java.util.function.Consumer<OpenEvent>() {
      @Override
      public void accept(OpenEvent e) {
        System.out.println("Connection opened");
        // 连接建立后，释放阻塞主线程的锁
        openLatch.countDown();
      }
    };

    java.util.function.Consumer<MessageEvent> onMessage = new java.util.function.Consumer<MessageEvent>() {
      @Override
      public void accept(MessageEvent e) {
        WebSocketPacket data = e.data;
        Long id = data.getId();
        String wsBodyText = data.getWsBodyText();
        sent.put(id, true);
        System.out.println("Received: " + wsBodyText);
        complete.onNext(id);
        latch.countDown(); // 每次接收到消息后减少计数
      }
    };

    java.util.function.Consumer<CloseEvent> onClose = new java.util.function.Consumer<CloseEvent>() {
      @Override
      public void accept(CloseEvent e) {
        System.out.printf("Connection closed: %d, %s, %s\n", e.code, e.reason, e.wasClean);
      }
    };

    java.util.function.Consumer<ErrorEvent> onError = new java.util.function.Consumer<ErrorEvent>() {
      @Override
      public void accept(ErrorEvent e) {
        System.out.println(String.format("Error occurred: %s", e.msg));
      }
    };

    java.util.function.Consumer<Throwable> onThrows = new java.util.function.Consumer<Throwable>() {
      @Override
      public void accept(Throwable t) {
        t.printStackTrace();
      }
    };

    // 配置 WebSocket 客户端
    WebsocketClientConfig wsClientConfig = new WebsocketClientConfig(onOpen, onMessage, onClose, onError, onThrows);

    // 创建 WebSocket 客户端
    WebsocketClient client = WebsocketClient.create(uri, wsClientConfig);

    // 连接到服务器
    WebSocket ws = client.connect();

    // 等待连接建立
    openLatch.await();

    // 在连接建立后发送消息
    for (int i = 0; i < total; i++) {
      ws.send("" + i);
      System.out.println("Sent: " + i);
    }

    // 等待所有消息接收完毕
    latch.await();
    System.out.println("All messages have been received.");
  }
}
