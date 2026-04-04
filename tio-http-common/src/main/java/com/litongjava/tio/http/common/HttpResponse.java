package com.litongjava.tio.http.common;

import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

import com.litongjava.tio.consts.TioConst;
import com.litongjava.tio.consts.TioCoreConfigKeys;
import com.litongjava.tio.core.ChannelContext;
import com.litongjava.tio.core.Tio;
import com.litongjava.tio.http.common.stream.TioOutputStream;
import com.litongjava.tio.http.common.utils.HttpGzipUtils;
import com.litongjava.tio.http.common.utils.MimeTypeUtils;
import com.litongjava.tio.utils.environment.EnvUtils;
import com.litongjava.tio.utils.hutool.ClassUtil;
import com.litongjava.tio.utils.json.Json;

import nexus.io.model.sys.SysConst;

/**
 * @author tanyaowu
 */
public class HttpResponse extends HttpPacket {
  private final Logger log = LoggerFactory.getLogger(this.getClass());
  private final static boolean DIAGNOSTIC_LOG_ENABLED = EnvUtils.getBoolean(TioCoreConfigKeys.TIO_CORE_DIAGNOSTIC,
      false);
  private static final long serialVersionUID = -3512681144230291786L;
  public transient static final HttpResponse NULL_RESPONSE = new HttpResponse();
  /**
   * 服务器端用（因为服务器端可以直接枚举）
   */
  private HttpResponseStatus status = null;
  /**
   * 是否是静态资源 true: 静态资源
   */
  private boolean isStaticRes = false;

  /**
   * 是否后续返回流格式,如果是则在相应时不计算Content-Length
   */
  private transient boolean stream = false;
  /**
   * 是否需要改发改数据包,如果该数据包已经发送或者即将发送,可以在也业务中将该值设置为false,防止重复发送数据包
   */
  private transient boolean send = true;
  /**
   * 是否添加Content-Length
   */
  private transient boolean skipAddContentLength = false;
  private transient HttpRequest request = null;
  private transient List<Cookie> cookies = null;
  private Map<HeaderName, HeaderValue> headers = new HashMap<>();
  private int headerByteCount = 2;
  /**
   * 是否已经被gzip压缩过了，防止重复压缩
   */
  private boolean skipGzipped = false;
  private String charset = TioConst.UTF_8;
  /**
   * 忽略ip访问统计
   */
  private transient boolean skipIpStat = false;
  /**
   * 忽略token访问统计
   */
  private transient boolean skipTokenStat = false;

  private String version;

  public HttpResponse() {
    this.status = HttpResponseStatus.C200;
  }

  /**
   * @param request
   */
  public HttpResponse(HttpRequest request) {
    this();
    if (request == null) {
      return;
    }
    this.charset = request.getCharset();
    this.request = request;

    String version = request.requestLine.getVersion(); // "1.0" or "1.1"
    this.version = version;

    // 拿到客户端的 Connection 头（可能为 "keep-alive" / "close" / null）
    String conn = request.getConnection();

    // HTTP/1.0
    if (HttpConst.HttpVersion.V1_0.equals(version)) {
      // 只有在允许兼容 1.0 并且客户端显式要 keep-alive 时才开启
      if (request.httpConfig != null && request.httpConfig.compatible1_0 &&
      //
          HttpConst.RequestHeaderValue.Connection.keep_alive.equalsIgnoreCase(conn)) {
        addHeader(HeaderName.Connection, HeaderValue.Connection.keep_alive);
        addHeader(HeaderName.Keep_Alive, HeaderValue.Keep_Alive.TIMEOUT_10_MAX_20);
        setKeepConnection(true);
      } else {
        // 默认关闭
        addHeader(HeaderName.Connection, HeaderValue.Connection.close);
        setKeepConnection(false);
      }

      // HTTP/1.1（默认长连接，除非客户端要 close）
    } else if (HttpConst.HttpVersion.V1_1.equals(version)) {
      if (HttpConst.RequestHeaderValue.Connection.close.equalsIgnoreCase(conn)) {
        addHeader(HeaderName.Connection, HeaderValue.Connection.close);
        setKeepConnection(false);
      } else {
        addHeader(HeaderName.Connection, HeaderValue.Connection.keep_alive);
        setKeepConnection(true);
      }
      // 其它版本也按 close 处理
    } else {
      addHeader(HeaderName.Connection, HeaderValue.Connection.close);
      setKeepConnection(false);
    }
    if (DIAGNOSTIC_LOG_ENABLED) {
      log.info("keepConnection:{}", isKeepConnection());
    }

    // 200 状态设置
    this.status = HttpResponseStatus.C200.changeVersion(version);
  }

  /**
   * 
   * @param responseHeaders
   * @param body
   */
  public HttpResponse(Map<HeaderName, HeaderValue> responseHeaders, byte[] body) {
    if (responseHeaders != null) {
      this.headers.putAll(responseHeaders);
    }
    this.status = HttpResponseStatus.C200;
    this.setBody(body);
    HttpGzipUtils.gzip(this);
  }

  public HttpResponse setSend(boolean b) {
    this.send = b;
    return this;
  }

  public boolean isSend() {
    return send;
  }

  /**
   * 支持跨域
   */
  public void crossDomain() {
    addHeader(HeaderName.Access_Control_Allow_Origin, HeaderValue.from("*"));
    addHeader(HeaderName.Access_Control_Allow_Headers, HeaderValue.from("x-requested-with,content-type"));
  }

  public static HttpResponse cloneResponse(HttpRequest request, HttpResponse response) {
    HttpResponse cloneResponse = new HttpResponse(request);
    cloneResponse.setStatus(response.getStatus());
    cloneResponse.setBody(response.getBody());
    cloneResponse.setSkipGzipped(response.isSkipGzipped());
    cloneResponse.addHeaders(response.getHeaders());

    if (cloneResponse.getCookies() != null) {
      cloneResponse.getCookies().clear();
    }
    return cloneResponse;
  }

  /**
   * <span style='color:red'>
   * <p style='color:red;font-size:12pt;'>
   * 警告：通过本方法获得Map<HeaderName, HeaderValue>对象后，请勿调用put(key, value)。
   * <p>
   * <p style='color:red;font-size:12pt;'>
   * 添加响应头只能通过HttpResponse.addHeader(HeaderName,
   * HeaderValue)或HttpResponse.addHeaders(Map<HeaderName, HeaderValue>
   * headers)方式添加
   * <p>
   * </span>
   * 
   * @return
   * @author tanyaowu
   */
  public Map<HeaderName, HeaderValue> getHeaders() {
    return headers;
  }

  public void setHeader(String name, String value) {
    this.addHeader(name, value);
  }

  public void setHeader(HeaderName key, String headeValue) {
    HeaderValue value = HeaderValue.from(headeValue);
    this.addHeader(key, value);
  }

  public HttpResponse addHeader(String name, String headeValue) {
    HeaderName key = HeaderName.from(name);
    HeaderValue value = HeaderValue.from(headeValue);
    this.addHeader(key, value);
    return this;
  }

  public void addHeader(HeaderName key, HeaderValue value) {
    headers.put(key, value);
    // 冒号和\r\n
    headerByteCount += (key.bytes.length + value.bytes.length + 3);
  }

  public void addHeaders(Map<HeaderName, HeaderValue> headers) {
    if (headers != null) {
      Set<Entry<HeaderName, HeaderValue>> set = headers.entrySet();
      for (Entry<HeaderName, HeaderValue> entry : set) {
        this.addHeader(entry.getKey(), entry.getValue());
      }
    }
  }

  /**
   * 获取"Content-Type"头部内容
   * 
   * @return
   * @author tanyaowu
   */
  public HeaderValue getContentType() {
    return this.headers.get(HeaderName.Content_Type);
  }

  public boolean addCookie(Cookie cookie) {
    if (cookies == null) {
      cookies = new ArrayList<>();
    }
    return cookies.add(cookie);
  }

  /**
   * @return the charset
   */
  public String getCharset() {
    return charset;
  }

  /**
   * @return the cookies
   */
  public List<Cookie> getCookies() {
    return cookies;
  }

  /**
   * @return the request
   */
  public HttpRequest getHttpRequest() {
    return request;
  }

  /**
   * @return the status
   */
  public HttpResponseStatus getStatus() {
    return status;
  }

  /**
   * @return the isStaticRes
   */
  public boolean isStaticRes() {
    return isStaticRes;
  }

  @Override
  public String logstr() {
    String str = null;
    if (request != null) {
      str = "reponse: requestID_" + request.getId() + "  " + request.getRequestLine().getPathAndQuery();
      str += SysConst.CRLF + this.getHeaderString();
    } else {
      str = "nresponse " + status.getHeaderText();
    }
    return str;
  }

  /**
   * @param charset the charset to set
   */
  public void setCharset(String charset) {
    this.charset = charset;
  }

  /**
   * @param cookies the cookies to set
   */
  public void setCookies(List<Cookie> cookies) {
    this.cookies = cookies;
  }

  /**
   * @param request the request to set
   */
  public void setHttpRequestPacket(HttpRequest request) {
    this.request = request;
  }

  /**
   * @param isStaticRes the isStaticRes to set
   */
  public void setStaticRes(boolean isStaticRes) {
    this.isStaticRes = isStaticRes;
  }

  public HttpResponse setStatus(int code) {
    switch (code) {
    case 100:
      this.status = HttpResponseStatus.C100;
      break;
    case 101:
      this.status = HttpResponseStatus.C101;
      break;
    case 200:
      this.status = HttpResponseStatus.C200;
      break;
    case 201:
      this.status = HttpResponseStatus.C201;
      break;
    case 202:
      this.status = HttpResponseStatus.C202;
      break;
    case 203:
      this.status = HttpResponseStatus.C203;
      break;
    case 204:
      this.status = HttpResponseStatus.C204;
      break;
    case 205:
      this.status = HttpResponseStatus.C205;
      break;
    case 206:
      this.status = HttpResponseStatus.C206;
      break;
    case 300:
      this.status = HttpResponseStatus.C300;
      break;
    case 301:
      this.status = HttpResponseStatus.C301;
      break;
    case 302:
      this.status = HttpResponseStatus.C302;
      break;
    case 303:
      this.status = HttpResponseStatus.C303;
      break;
    case 304:
      this.status = HttpResponseStatus.C304;
      break;
    case 305:
      this.status = HttpResponseStatus.C305;
      break;
    case 307:
      this.status = HttpResponseStatus.C307;
      break;
    case 400:
      this.status = HttpResponseStatus.C400;
      break;
    case 401:
      this.status = HttpResponseStatus.C401;
      break;
    case 403:
      this.status = HttpResponseStatus.C403;
      break;
    case 404:
      this.status = HttpResponseStatus.C404;
      break;
    case 405:
      this.status = HttpResponseStatus.C405;
      break;
    case 406:
      this.status = HttpResponseStatus.C406;
      break;
    case 407:
      this.status = HttpResponseStatus.C407;
      break;
    case 408:
      this.status = HttpResponseStatus.C408;
      break;
    case 409:
      this.status = HttpResponseStatus.C409;
      break;
    case 410:
      this.status = HttpResponseStatus.C410;
      break;
    case 411:
      this.status = HttpResponseStatus.C411;
      break;
    case 412:
      this.status = HttpResponseStatus.C412;
      break;
    case 413:
      this.status = HttpResponseStatus.C413;
      break;
    case 414:
      this.status = HttpResponseStatus.C414;
      break;
    case 416:
      this.status = HttpResponseStatus.C416;
      break;
    case 500:
      this.status = HttpResponseStatus.C500;
      break;
    case 501:
      this.status = HttpResponseStatus.C501;
      break;
    case 502:
      this.status = HttpResponseStatus.C502;
      break;
    case 503:
      this.status = HttpResponseStatus.C503;
      break;
    case 504:
      this.status = HttpResponseStatus.C504;
      break;
    case 505:
      this.status = HttpResponseStatus.C505;
      break;
    default:
      this.status = HttpResponseStatus.CUSTOM.build(version, code);
      break;
    }
    return this;
  }

  /**
   * @param status the status to set
   */
  public HttpResponse setStatus(HttpResponseStatus status) {
    this.status = status;
    return this;
  }

  public HttpResponse setStatus(int status, String description) {
    HttpResponseStatus custom = HttpResponseStatus.CUSTOM.build(version, status, description);
    this.status = custom;
    return this;
  }

  public void setStatus(int status, String description, String headerText) {
    HttpResponseStatus custom = HttpResponseStatus.CUSTOM.build(version, status, description, headerText);
    this.status = custom;
  }

  public boolean isSkipGzipped() {
    return skipGzipped;
  }

  public void setSkipGzipped(boolean hasGzipped) {
    this.skipGzipped = hasGzipped;
  }

  public boolean isSkipIpStat() {
    return skipIpStat;
  }

  public void setSkipIpStat(boolean skipIpStat) {
    this.skipIpStat = skipIpStat;
  }

  public boolean isSkipTokenStat() {
    return skipTokenStat;
  }

  public void setSkipTokenStat(boolean skipTokenStat) {
    this.skipTokenStat = skipTokenStat;
  }

  public HeaderValue getLastModified() {
    return this.getHeader(HeaderName.Last_Modified);
  }

  /**
   * 
   * @param name 从HeaderName中找，或者HeaderName.from(name)
   * @return
   * @author tanyaowu
   */
  public HeaderValue getHeader(HeaderName name) {
    return headers.get(name);
  }

  public void setLastModified(HeaderValue lastModified) {
    if (lastModified != null) {
      this.addHeader(HeaderName.Last_Modified, lastModified);
    }
  }

  @Override
  public String toString() {
    return this.status.toString();
  }

  /**
   * @return the headerByteCount
   */
  public int getHeaderByteCount() {
    return headerByteCount;
  }

  public void setContentType(String contentType) {
    this.addHeader(HeaderName.Content_Type, HeaderValue.Content_Type.from(contentType));
  }

  public void setContentLength(long totalBytes) {
    this.addHeader(HeaderName.Content_Length, HeaderValue.from(String.valueOf(totalBytes)));
  }

  public void setContentDisposition(String value) {
    this.addHeader(HeaderName.Content_Disposition, HeaderValue.from(value));
  }

  public boolean isStream() {
    return stream;
  }

  public void setStream(boolean stream) {
    this.stream = stream;
  }

  public HttpResponse addServerSentEventsHeader(String charset) {
    this.setContentType("text/event-stream;charset=" + charset);
    this.addHeader(HeaderName.Connection, HeaderValue.from("keep-alive"));
    this.stream = true;
    this.keepConnection = true;
    return this;
  }

  public HttpResponse addServerSentEventsHeader() {
    return addServerSentEventsHeader("utf-8");
  }

  public void sendRedirect(String url) {
    setStatus(HttpResponseStatus.C302);
    addHeader(HeaderName.Location, HeaderValue.from(url));

  }

  public static HttpResponse string(String bodyString, String charset, String mimeTypeStr) {
    HttpResponse httpResponse = new HttpResponse();
    httpResponse.setString(bodyString, charset, mimeTypeStr);
    return httpResponse;
  }

  public HttpResponse setBodyString(String bodyString) {
    if (bodyString != null) {
      try {
        setBody(bodyString.getBytes(charset));
      } catch (UnsupportedEncodingException e) {
        throw new RuntimeException(e);
      }
    }
    this.addHeader(HeaderName.Content_Type, HeaderValue.Content_Type.TEXT_PLAIN_TXT);
    return this;
  }

  public HttpResponse setBodyString(String bodyString, String charset) {
    if (bodyString != null) {
      try {
        setBody(bodyString.getBytes(charset));
      } catch (UnsupportedEncodingException e) {
        throw new RuntimeException(e);
      }
    }
    return this;
  }

  public HttpResponse setString(String bodyString, String charset, String mimeTypeStr) {
    if (bodyString != null) {
      if (charset == null) {
        setBody(bodyString.getBytes());
      } else {
        try {
          setBody(bodyString.getBytes(charset));
        } catch (UnsupportedEncodingException e) {
          throw new RuntimeException(e);
        }
      }
    }
    this.addHeader(HeaderName.Content_Type, HeaderValue.Content_Type.from(mimeTypeStr));
    return this;
  }

  public HttpResponse setJson(Object body) {
    String charset = this.getHttpRequest().getChannelContext().getTioConfig().getCharset();
    if (body == null) {
      return setString("", charset, MimeTypeUtils.getJson(charset));
    } else {
      if (body.getClass() == String.class || ClassUtil.isBasicType(body.getClass())) {
        return setString(body + "", charset, MimeTypeUtils.getJson(charset));
      } else {
        return setString(Json.getJson().toJson(body), charset, MimeTypeUtils.getJson(charset));
      }
    }
  }

  public HttpResponse setSkipAddContentLength(boolean b) {
    this.skipAddContentLength = b;
    return this;
  }

  public HttpResponse removeHeaders(String name) {
    headers.remove(HeaderName.from(name));
    return this;

  }

  public boolean isSkipAddContentLength() {
    return skipAddContentLength;
  }

  public void setAttachmentFilename(String downloadFilename) {
    this.setHeader(HeaderName.Content_Disposition, "attachment; filename=\"" + downloadFilename + "\"");
  }

  public HttpResponse status(int code) {
    return this.setStatus(code);
  }

  public HttpResponse status(int status, String description) {
    return this.setStatus(status, description);
  }

  public HttpResponse header(String key, String value) {
    return this.addHeader(key, value);
  }

  public HttpResponse fail(Object body) {
    this.setStatus(400);
    return setJson(body);
  }
  
  public HttpResponse fail(String body) {
    this.setStatus(400);
    return setBodyString(body);
  }

  public HttpResponse error(String body) {
    this.setStatus(HttpResponseStatus.C500);
    return setBodyString(body);
  }

  public static HttpResponse json(Object body) {
    String charset = Charset.defaultCharset().name();
    return json(body, charset);
  }

  public static HttpResponse json(Object body, String charset) {
    if (body == null) {
      return string("", charset, MimeTypeUtils.getJson(charset));
    } else {
      if (body.getClass() == String.class || ClassUtil.isBasicType(body.getClass())) {
        return string(body + "", charset, MimeTypeUtils.getJson(charset));
      } else {
        return string(Json.getJson().toJson(body), charset, MimeTypeUtils.getJson(charset));
      }
    }
  }

  public static HttpResponse json(HttpRequest request, Object body) {
    HttpResponse httpResponse = new HttpResponse(request);
    return httpResponse.setJson(body);
  }

  public HttpResponse setBody(String bodyString) {
    return this.setBodyString(bodyString);
  }
  
  public HttpResponse body(String bodyString) {
    return this.setBodyString(bodyString);
  }

  public HttpResponse body(Object obj) {
    return this.setJson(obj);
  }

  public HttpResponse body(byte[] payload) {
    this.setBody(payload);
    return this;
  }

  public HttpResponse ok(byte[] payload) {
    setBody(payload);
    return this;
  }

  public void disableGzip(boolean b) {
    this.setSkipAddContentLength(!b);
  }

  public OutputStream newOutputStream() {
    return newOutputStream(request.channelContext);
  }

  public OutputStream newOutputStream(ChannelContext ctx) {
    this.addHeader(HeaderName.Transfer_Encoding, HeaderValue.from("chunked"));
    this.setSkipAddContentLength(true);
    Tio.bSend(request.channelContext, this);
    this.setSend(false);
    return new TioOutputStream(ctx, true);
  }

}
