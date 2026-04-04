package nexus.io.tio.http.server.stat.token;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nexus.io.tio.http.common.HttpRequest;
import nexus.io.tio.http.server.session.HttpSessionUtils;

/**
 * @author tanyaowu
 */
public class DefaultTokenGetter implements TokenGetter {
  @SuppressWarnings("unused")
  private static Logger log = LoggerFactory.getLogger(DefaultTokenGetter.class);

  public static DefaultTokenGetter me = new DefaultTokenGetter();

  /**
   * 
   */
  protected DefaultTokenGetter() {
  }

  @Override
  public String getToken(HttpRequest request) {
    // HttpSession httpSession = request.getHttpSession();
    // if (httpSession != null) {
    // return httpSession.getId();
    // }
    // Cookie cookie = DefaultHttpRequestHandler.getSessionCookie(request, request.httpConfig);
    // if (cookie != null) {
    // log.error("token from cookie: {}", cookie.getValue());
    // return cookie.getValue();
    // }
    return HttpSessionUtils.getSessionId(request);
  }

}
