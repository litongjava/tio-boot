package com.litongjava.tio.http.common;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.litongjava.constants.ServerConfigKeys;
import com.litongjava.model.sys.SysConst;
import com.litongjava.tio.core.ChannelContext;
import com.litongjava.tio.core.TioConfig;
import com.litongjava.tio.core.pool.BufferPoolUtils;
import com.litongjava.tio.http.common.utils.HttpDateTimer;
import com.litongjava.tio.http.common.utils.HttpGzipUtils;
import com.litongjava.tio.utils.environment.EnvUtils;
import com.litongjava.tio.utils.hutool.StrUtil;

/**
 * High-performance HttpResponseEncoder 关键思路：不修改 headers、不重复计算、所有长度先算准、一把写完。
 */
public class HttpResponseEncoder {
  private static final Logger log = LoggerFactory.getLogger(HttpResponseEncoder.class);
  // 常量与固定头部长度估算
  public static final int MAX_HEADER_LENGTH = 20480;
  private static final byte COLON = (byte) ':';
  private static final byte[] CRLF = SysConst.CR_LF;

  private static final int HEADER_SERVER_LENGTH = HeaderName.Server.bytes.length + HeaderValue.Server.TIO.bytes.length
      + 3; // ":" + CRLF
  private static final int HEADER_DATE_LENGTH_PREFIX = HeaderName.Date.bytes.length + 3; // ":" + CRLF
  @SuppressWarnings("unused")
  private static final int HEADER_FIXED_LENGTH = HEADER_SERVER_LENGTH + HEADER_DATE_LENGTH_PREFIX;

  private static final boolean showServer = EnvUtils
      .getBoolean(ServerConfigKeys.SERVER_HTTP_RESPONSE_HEANDER_SHOW_SERVER, true);

  /**
   * 普通/内存体编码
   */
  public static ByteBuffer encode(HttpResponse httpResponse, TioConfig tioConfig, ChannelContext channelContext) {
    // 文件体特殊通道：只写头部 + 由文件通道写 body
    final File fileBody = httpResponse.getFileBody();
    if (fileBody != null) {
      final long length = fileBody.length();
      return buildHeader(httpResponse, length);
    }

    Charset cs = Charset.forName(httpResponse.getCharset());
    byte[] body = httpResponse.body;
    int bodyLength = 0;

    // JSONP 包装（如需）
    final HttpRequest httpRequest = httpResponse.getHttpRequest();
    if (httpRequest != null) {
      final String jsonp = httpRequest.getParam(httpRequest.httpConfig.getJsonpParamName());
      if (StrUtil.isNotBlank(jsonp)) {
        final byte[] jsonpBytes = jsonp.getBytes(cs);
        final byte[] raw = (body != null) ? body : SysConst.NULL;
        final int len = jsonpBytes.length + 1 + raw.length + 1; // callback( + body + )
        final byte[] merged = new byte[len];
        int p = 0;
        System.arraycopy(jsonpBytes, 0, merged, p, jsonpBytes.length);
        p += jsonpBytes.length;
        merged[p++] = SysConst.LEFT_BRACKET;
        System.arraycopy(raw, 0, merged, p, raw.length);
        p += raw.length;
        merged[p] = SysConst.RIGHT_BRACKET;
        body = merged;
        httpResponse.setBody(merged);
      }
    }

    // gzip（如需）
    if (body != null) {
      try {
        HttpGzipUtils.gzip(httpRequest, httpResponse);
        body = httpResponse.body;
      } catch (Exception e) {
        log.error(e.toString(), e);
      }
      bodyLength = (body != null ? body.length : 0);
    }

    final HttpResponseStatus status = httpResponse.getStatus();
    final int respLineLength = status.responseLineBinary.length;

    // 读取已有 headers（不修改）
    final Map<HeaderName, HeaderValue> headers = httpResponse.getHeaders();

    // 是否需要写 Content-Length（不写入 headers Map，直接写出，避免二次遍历与对象创建）
    final boolean shouldAddContentLength = !httpResponse.isStream() && !httpResponse.isSkipAddContentLength();
    final byte[] contentLengthBytes = shouldAddContentLength ? asciiDigits(bodyLength) : null;

    // 预估 header 长度（不含响应行）
    int headerLength = httpResponse.getHeaderByteCount(); // 仅已有 headers 的字节数
    // + Server / Date 固定头
    final byte[] httpDateBytes = HttpDateTimer.httpDateValue.bytes;
    int fixed = (showServer ? HEADER_SERVER_LENGTH : 0) + HEADER_DATE_LENGTH_PREFIX + httpDateBytes.length;

    // + Content-Length（如果需要且未由外部写入）
    if (shouldAddContentLength) {
      headerLength += HeaderName.Content_Length.bytes.length + 1 /* : */ + contentLengthBytes.length + 2 /* CRLF */;
    }

    // + Cookies
    if (httpResponse.getCookies() != null) {
      for (Cookie cookie : httpResponse.getCookies()) {
        // 优先复用已有 bytes
        byte[] bs = cookie.getBytes();
        if (bs == null) {
          bs = cookie.toString().getBytes(cs);
          cookie.setBytes(bs);
        }
        headerLength += HeaderName.SET_COOKIE.bytes.length + 1 /* : */ + bs.length + 2 /* CRLF */;
      }
    }

    // + 头部结束 CRLF
    headerLength += fixed + 2;

    // 分配最终缓冲区
    ByteBuffer buf = BufferPoolUtils.allocate(TioConfig.WRITE_CHUNK_SIZE, respLineLength + headerLength + bodyLength);

    // 写响应行
    buf.put(status.responseLineBinary);

    // Server
    if (showServer) {
      buf.put(HeaderName.Server.bytes).put(COLON).put(HeaderValue.Server.TIO.bytes).put(CRLF);
    }

    // Date
    buf.put(HeaderName.Date.bytes).put(COLON).put(httpDateBytes).put(CRLF);

    // Content-Length（如需）
    if (shouldAddContentLength) {
      buf.put(HeaderName.Content_Length.bytes).put(COLON).put(contentLengthBytes).put(CRLF);
    }

    // 其它 headers（保持已有顺序）
    final Set<Entry<HeaderName, HeaderValue>> headerSet = headers.entrySet();
    for (Entry<HeaderName, HeaderValue> entry : headerSet) {
      buf.put(entry.getKey().bytes).put(COLON).put(entry.getValue().bytes).put(CRLF);
    }

    // Cookies
    if (httpResponse.getCookies() != null) {
      for (Cookie cookie : httpResponse.getCookies()) {
        buf.put(HeaderName.SET_COOKIE.bytes).put(COLON).put(cookie.getBytes()).put(CRLF);
      }
    }

    // 空行
    buf.put(CRLF);

    // 写 body
    if (bodyLength > 0) {
      buf.put(body);
    }

    buf.flip();
    return buf;
  }

  /**
   * 文件响应：仅构建头部（不把 Content-Length 放进 headers）
   */
  private static ByteBuffer buildHeader(HttpResponse httpResponse, long contentLength) {
    final HttpResponseStatus status = httpResponse.getStatus();
    final byte[] httpLine = status.responseLineBinary;
    final Map<HeaderName, HeaderValue> headers = httpResponse.getHeaders();
    final byte[] dateBytes = HttpDateTimer.httpDateValue.bytes;

    // Content-Length 数字字节（ASCII）
    final byte[] lengthBytes = asciiDigits(contentLength);

    int headerLen = 0;
    headerLen += httpLine.length;

    if (showServer) {
      headerLen += HEADER_SERVER_LENGTH;
    }
    headerLen += HEADER_DATE_LENGTH_PREFIX + dateBytes.length;

    // + Content-Length
    headerLen += HeaderName.Content_Length.bytes.length + 1 + lengthBytes.length + 2;

    // 其它 headers
    for (Entry<HeaderName, HeaderValue> e : headers.entrySet()) {
      headerLen += e.getKey().bytes.length + 1 + e.getValue().bytes.length + 2;
    }

    // Cookies
    if (httpResponse.getCookies() != null) {
      for (Cookie c : httpResponse.getCookies()) {
        byte[] cbytes = c.getBytes();
        if (cbytes == null) {
          // 尽量避免查字符集：文件响应通常无 cookie 字节缺失，这里兜底一次
          cbytes = c.toString().getBytes(Charset.forName(httpResponse.getCharset()));
          c.setBytes(cbytes);
        }
        headerLen += HeaderName.SET_COOKIE.bytes.length + 1 + cbytes.length + 2;
      }
    }

    // 头部结束空行
    headerLen += 2;

    final ByteBuffer buf = ByteBuffer.allocate(headerLen);
    buf.put(httpLine);

    if (showServer) {
      buf.put(HeaderName.Server.bytes).put(COLON).put(HeaderValue.Server.TIO.bytes).put(CRLF);
    }

    buf.put(HeaderName.Date.bytes).put(COLON).put(dateBytes).put(CRLF);

    // Content-Length（直接写，不改 headers）
    final boolean shouldAddContentLength = !httpResponse.isStream() && !httpResponse.isSkipAddContentLength();
    if (shouldAddContentLength) {
      buf.put(HeaderName.Content_Length.bytes).put(COLON).put(lengthBytes).put(CRLF);
    }

    for (Entry<HeaderName, HeaderValue> e : headers.entrySet()) {
      buf.put(e.getKey().bytes).put(COLON).put(e.getValue().bytes).put(CRLF);
    }

    if (httpResponse.getCookies() != null) {
      for (Cookie c : httpResponse.getCookies()) {
        buf.put(HeaderName.SET_COOKIE.bytes).put(COLON).put(c.getBytes()).put(CRLF);
      }
    }

    buf.put(CRLF);
    buf.flip();
    return buf;
  }

  /**
   * 将 long/int 转 ASCII 数字字节（无中间 String 对象）
   */
  private static byte[] asciiDigits(long v) {
    // 长度估计：最长 20 位（long），用栈式写法避免 String.valueOf()
    if (v == 0)
      return new byte[] { '0' };
    boolean neg = v < 0;
    long x = neg ? -v : v;

    // 预分配最大 20 + 可选 '-'，然后从末尾回填
    byte[] buf = new byte[21];
    int p = buf.length;

    while (x > 0) {
      long q = x / 10;
      int d = (int) (x - q * 10);
      buf[--p] = (byte) ('0' + d);
      x = q;
    }
    if (neg) {
      buf[--p] = (byte) '-';
    }
    int len = buf.length - p;
    byte[] out = new byte[len];
    System.arraycopy(buf, p, out, 0, len);
    return out;
  }
}
