package nexus.io.tio.http.common;

import nexus.io.model.body.RespBodyVo;
import org.testng.annotations.Test;
import static org.junit.Assert.*;

public class HttpResponseRespondTest {
  @Test public void delegatesWithoutDefaultingMessageOrHttpStatus() {
    final Object[] serialized = new Object[1];
    HttpResponse response = new HttpResponse() {
      @Override public HttpResponse setJson(Object value) { serialized[0] = value; return this; }
    };
    response.setStatus(202);
    RespBodyVo result = RespBodyVo.ok("data");
    assertSame(response, response.respond(result));
    assertSame(result, serialized[0]);
    assertNull(result.getMsg());
    assertEquals(202, response.getStatus().status);
    result.msg("business message");
    response.respond(result);
    assertEquals("business message", result.getMsg());
    response.setStatus(409).respond(RespBodyVo.fail("conflict"));
    assertEquals(409, response.getStatus().status);
  }
}
