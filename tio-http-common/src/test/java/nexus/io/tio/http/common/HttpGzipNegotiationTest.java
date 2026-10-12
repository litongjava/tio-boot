package nexus.io.tio.http.common;

import static org.junit.Assert.*;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import nexus.io.tio.http.common.utils.HttpGzipUtils;
import nexus.io.tio.utils.hutool.ZipUtil;

public class HttpGzipNegotiationTest {
  private HttpRequest request(String encoding) {
    HttpRequest request = new HttpRequest();
    if (encoding != null) {
      request.addHeader(RequestHeaderKey.Accept_Encoding, encoding);
    }
    return request;
  }

  private HttpResponse response(HttpRequest request) {
    HttpResponse response = new HttpResponse();
    response.setHttpRequestPacket(request);
    byte[] body = new byte[1024];
    Arrays.fill(body, (byte) 'a');
    response.setBody(body);
    response.addHeader(HeaderName.Content_Type, HeaderValue.from("application/json"));
    return response;
  }

  @DataProvider(name = "encodings")
  public Object[][] encodings() {
    return new Object[][] {
      {null, false}, {"", false}, {" ", false}, {"identity", false},
      {"br, deflate", false}, {"xgzip", false}, {"gzip", true},
      {"br, gzip;q=0.5", true}, {" GZip ; Q = 0.001 ", true},
      {"gzip;q=1.000", true}, {"gzip;q=0", false}, {"gzip;q=0.000", false},
      {"gzip;q=0, *;q=1", false}, {"*;q=1, gzip;q=0", false},
      {"*", true}, {"*;q=0", false}, {"*;q=0.5", true},
      {"gzip;q=0.5, *;q=0", true}, {"gzip;q=0, gzip", false},
      {"gzip, gzip;q=0", false}, {"gzip;q=NaN", false},
      {"gzip;q=abc", false}, {"gzip;q=1.1", false}, {"gzip;q=-1", false},
      {"gzip;q=0.0001", false}, {"gzip;q=", false}, {"gzip;q=0;q=1", false}
    };
  }

  @Test(dataProvider = "encodings")
  public void negotiatesBothCompressionEntryPoints(String encoding, boolean accepted) {
    HttpRequest request = request(encoding);
    assertEquals(accepted, request.getIsSupportGzip().booleanValue());
    for (boolean responseOnly : new boolean[] {false, true}) {
      HttpResponse response = response(request);
      byte[] original = response.getBody();
      if (responseOnly) {
        HttpGzipUtils.gzip(response);
      } else {
        HttpGzipUtils.gzip(request, response);
      }
      assertEquals("Accept-Encoding", response.getHeader(HeaderName.Vary).getValue());
      if (accepted) {
        assertEquals("gzip", response.getHeader(HeaderName.Content_Encoding).getValue());
        assertArrayEquals(original, ZipUtil.unGzip(response.getBody()));
      } else {
        assertNull(response.getHeader(HeaderName.Content_Encoding));
        assertSame(original, response.getBody());
      }
    }
  }

  @Test
  public void preservesAndDeduplicatesVary() {
    for (String encoding : new String[] {"identity", "gzip"}) {
      HttpResponse response = response(request(encoding));
      response.addHeader(HeaderName.Vary, HeaderValue.from("Origin"));
      HttpGzipUtils.gzip(response);
      response.mergeVary("accept-encoding");
      response.mergeVary("origin");
      assertEquals("Origin, Accept-Encoding", response.getHeader(HeaderName.Vary).getValue());
    }
    HttpResponse response = response(request("identity"));
    response.addHeader(HeaderName.Vary, HeaderValue.from("*"));
    HttpGzipUtils.gzip(response);
    assertEquals("*", response.getHeader(HeaderName.Vary).getValue());
  }

  @Test
  public void doesNotCompressWithoutRequestIncludingConstructor() {
    HttpResponse response = response(null);
    byte[] original = response.getBody();
    HttpGzipUtils.gzip(response);
    assertSame(original, response.getBody());
    assertNull(response.getHeader(HeaderName.Content_Encoding));
    HttpResponse constructed = new HttpResponse(Collections.<HeaderName, HeaderValue>emptyMap(), original);
    assertSame(original, constructed.getBody());
    assertNull(constructed.getHeader(HeaderName.Content_Encoding));
    HttpGzipUtils.gzip((HttpResponse) null);
  }

  @Test
  public void honorsExplicitDisableWithoutForcingUnsupportedEncoding() {
    HttpRequest request = request("gzip");
    request.setIsSupportGzip(false);
    assertFalse(request.getIsSupportGzip());
    request.setIsSupportGzip(null);
    assertTrue(request.getIsSupportGzip());
    request.addHeader(RequestHeaderKey.Accept_Encoding, "identity");
    request.setIsSupportGzip(true);
    assertFalse(request.getIsSupportGzip());
  }

  @Test
  public void retainsSizeMediaAndSkipGuards() {
    HttpResponse small = response(request("gzip"));
    small.setBody("small".getBytes(StandardCharsets.UTF_8));
    HttpGzipUtils.gzip(small);
    assertNull(small.getHeader(HeaderName.Content_Encoding));
    for (String mime : new String[] {"image/png", "audio/mpeg", "video/mp4"}) {
      HttpResponse binary = response(request("gzip"));
      binary.addHeader(HeaderName.Content_Type, HeaderValue.from(mime));
      HttpGzipUtils.gzip(binary);
      assertNull(binary.getHeader(HeaderName.Content_Encoding));
    }
    HttpResponse skipped = response(request("gzip"));
    skipped.setSkipGzipped(true);
    HttpGzipUtils.gzip(skipped);
    assertNull(skipped.getHeader(HeaderName.Content_Encoding));
    HttpResponse compressed = response(request("gzip"));
    HttpGzipUtils.gzip(compressed);
    byte[] once = compressed.getBody();
    HttpGzipUtils.gzip(compressed);
    assertSame(once, compressed.getBody());
  }
}
