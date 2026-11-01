package nexus.io.tio.boot.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import nexus.io.tio.http.common.HttpMethod;
import nexus.io.tio.http.common.HttpRequest;
import nexus.io.tio.http.common.RequestLine;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.Request.Builder;
import okhttp3.RequestBody;

/**
 * Converts a tio HttpRequest into an OkHttp request so it can be forwarded to
 * another server. Every HTTP method is supported and the body is forwarded as
 * the raw bytes received from the client, so JSON, forms, multipart uploads and
 * binary payloads reach the upstream unchanged.
 */
public class OkHttpRequestUtils {

  /**
   * Hop-by-hop headers (RFC 7230 section 6.1) plus the headers OkHttp computes
   * itself. They describe the client connection and must not be forwarded.
   */
  public static final Set<String> HOP_BY_HOP_HEADERS = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
      "connection", "keep-alive", "proxy-authenticate", "proxy-authorization", "proxy-connection", "te", "trailer",
      "transfer-encoding", "upgrade", "host", "content-length")));

  private static final byte[] EMPTY = new byte[0];

  /**
   * Builds a request to {@code prefix + pathAndQuery} of the incoming request.
   *
   * @param prefix      scheme, host and optional path prefix, e.g.
   *                    {@code http://127.0.0.1:8080}
   * @param httpRequest incoming request
   * @return OkHttp request
   */
  public static Request buildOkHttpRequest(String prefix, HttpRequest httpRequest) {
    RequestLine requestLine = httpRequest.getRequestLine();
    String targetUrl = prefix + requestLine.getPathAndQuery();
    return buildOkHttpRequestWithUrl(targetUrl, httpRequest);
  }

  /**
   * Builds a request to the given absolute url, copying method, headers and the
   * raw body of the incoming request.
   *
   * @param targetUrl   absolute url including query string
   * @param httpRequest incoming request
   * @return OkHttp request
   */
  public static Request buildOkHttpRequestWithUrl(String targetUrl, HttpRequest httpRequest) {
    Builder requestBuilder = new Request.Builder();
    requestBuilder.url(targetUrl);
    requestBuilder.headers(buildHeaders(httpRequest.getHeaders()));

    HttpMethod requestMethod = httpRequest.getRequestLine().getMethod();
    String method = requestMethod == null ? "GET" : requestMethod.name();
    requestBuilder.method(method, buildRequestBody(method, httpRequest));
    return requestBuilder.build();
  }

  /**
   * Copies request headers, skipping hop-by-hop headers.
   *
   * @param requestHeaders lower-cased request headers
   * @return OkHttp headers
   */
  public static Headers buildHeaders(Map<String, String> requestHeaders) {
    Headers.Builder builder = new Headers.Builder();
    if (requestHeaders == null) {
      return builder.build();
    }
    for (Map.Entry<String, String> e : requestHeaders.entrySet()) {
      String name = e.getKey();
      String value = e.getValue();
      if (name == null || value == null) {
        continue;
      }
      if (HOP_BY_HOP_HEADERS.contains(name.toLowerCase(Locale.ROOT))) {
        continue;
      }
      builder.addUnsafeNonAscii(name, value);
    }
    return builder.build();
  }

  /**
   * Builds the request body from the raw bytes of the incoming request.
   *
   * @param method      upper-case method name
   * @param httpRequest incoming request
   * @return body, or null for methods that must not carry one
   */
  private static RequestBody buildRequestBody(String method, HttpRequest httpRequest) {
    if ("GET".equals(method) || "HEAD".equals(method)) {
      return null;
    }
    byte[] body = httpRequest.getBodyBytes();
    String contentType = httpRequest.getHeader("content-type");
    MediaType mediaType = contentType == null ? null : MediaType.parse(contentType);
    if (body == null || body.length == 0) {
      if (requiresRequestBody(method)) {
        return RequestBody.create(EMPTY, mediaType);
      }
      return null;
    }
    return RequestBody.create(body, mediaType);
  }

  private static boolean requiresRequestBody(String method) {
    return "POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method);
  }
}
