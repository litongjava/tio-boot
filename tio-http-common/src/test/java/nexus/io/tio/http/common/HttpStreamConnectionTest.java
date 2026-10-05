package nexus.io.tio.http.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * 流式响应（SSE）必须忽略客户端在请求头里要求的 Connection: close：这类响应在写完响应头时还没结束，
 * 一旦按 close 处理，传输层会立刻断链，后续分片全部丢失。普通响应仍应遵守客户端要求。
 */
public class HttpStreamConnectionTest {

  private static final String KEEP_ALIVE = "keep-alive";
  private static final String CLOSE = "close";

  /** 构造一个 HTTP/1.1 且声明 Connection: close 的请求。 */
  private HttpRequest requestWithClose() {
    HttpRequest request = new HttpRequest();
    RequestLine requestLine = new RequestLine();
    requestLine.setMethod(HttpMethod.POST);
    requestLine.setPath("/chat");
    requestLine.setVersion("1.1");
    request.setRequestLine(requestLine);
    request.setConnection(CLOSE);
    return request;
  }

  @Test
  public void streamingResponseStaysOpenWhenClientRequestsClose() {
    HttpResponse response = new HttpResponse(requestWithClose());
    response.addServerSentEventsHeader();
    response.prepareConnection();
    assertTrue("流式响应必须保持连接，否则分片会写进已关闭的通道", response.isKeepConnection());
    assertEquals(KEEP_ALIVE, response.getHeader(HeaderName.Connection).toString());
  }

  @Test
  public void regularResponseStillClosesWhenClientRequestsClose() {
    HttpResponse response = new HttpResponse(requestWithClose());
    response.prepareConnection();
    assertFalse("普通响应仍要遵守客户端的关闭要求", response.isKeepConnection());
    assertEquals(CLOSE, response.getHeader(HeaderName.Connection).toString());
  }

  @Test
  public void streamingResponseClosesWhenItDeclaresCloseExplicitly() {
    HttpResponse response = new HttpResponse(requestWithClose());
    response.addServerSentEventsHeader();
    response.addHeader(HeaderName.Connection, HeaderValue.Connection.close);
    response.prepareConnection();
    assertFalse("响应头里显式声明 close 时，流式响应也要关闭", response.isKeepConnection());
    assertEquals(CLOSE, response.getHeader(HeaderName.Connection).toString());
  }
}
