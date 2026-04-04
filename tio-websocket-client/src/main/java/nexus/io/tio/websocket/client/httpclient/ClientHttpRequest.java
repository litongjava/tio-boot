package nexus.io.tio.websocket.client.httpclient;

import nexus.io.tio.core.Node;
import nexus.io.tio.http.common.HttpMethod;
import nexus.io.tio.http.common.HttpRequest;
import nexus.io.tio.http.common.RequestLine;

/**
 * 临时写的httpclient，用于性能测试
 * 
 * @author tanyaowu
 *
 */
public class ClientHttpRequest extends HttpRequest {
  /**
   * 
   */
  private static final long serialVersionUID = -1997414964490639641L;

  public ClientHttpRequest(Node remote) {
    super(remote);
  }

  public static ClientHttpRequest get(String path, String queryString) {
    return new ClientHttpRequest(HttpMethod.GET, path, queryString);
  }

  public ClientHttpRequest(HttpMethod method, String path, String queryString) {
    super();
    RequestLine requestLine = new RequestLine();
    requestLine.setMethod(method);
    requestLine.setPath(path);
    requestLine.setQueryString(queryString);
    requestLine.setProtocol("HTTP");
    requestLine.setVersion("1.1");
    this.setRequestLine(requestLine);
  }

}
