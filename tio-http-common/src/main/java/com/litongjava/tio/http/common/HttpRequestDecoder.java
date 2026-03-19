package com.litongjava.tio.http.common;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.litongjava.constants.ServerConfigKeys;
import com.litongjava.model.sys.SysConst;
import com.litongjava.tio.core.ChannelCloseCode;
import com.litongjava.tio.core.ChannelContext;
import com.litongjava.tio.core.Node;
import com.litongjava.tio.core.Tio;
import com.litongjava.tio.core.exception.TioDecodeException;
import com.litongjava.tio.core.utils.IpBlacklistUtils;
import com.litongjava.tio.http.common.HttpConst.RequestBodyFormat;
import com.litongjava.tio.http.common.utils.HttpIpUtils;
import com.litongjava.tio.http.common.utils.HttpParseUtils;
import com.litongjava.tio.utils.environment.EnvUtils;
import com.litongjava.tio.utils.hutool.StrUtil;

public class HttpRequestDecoder {

  private static Logger log = LoggerFactory.getLogger(HttpRequestDecoder.class);
  
  // 头部，最多有多少字节
  public static final int MAX_LENGTH_OF_HEADER = 20480;

  // 头部，每行最大的字节数
  public static final int MAX_LENGTH_OF_HEADERLINE = 8192;

  // 请求行的最大长度
  public static final int MAX_LENGTH_OF_REQUESTLINE = 8192;

  // 是否打印请求体
  public static boolean PRINT_PACKET = EnvUtils.getBoolean(ServerConfigKeys.SERVER_HTTP_REQUEST_PRINT_PACKET, false);

  /**
   * 解码方法
   * 
   * @param buffer
   * @param limit
   * @param position
   * @param readableLength
   * @param channelContext
   * @param httpConfig
   * @return
   * @throws TioDecodeException
   */
  public static HttpRequest decode(ByteBuffer buffer, int limit, int position, int readableLength, ChannelContext channelContext,
      HttpConfig httpConfig) throws TioDecodeException {
    if (PRINT_PACKET) {
      buffer.mark();
      String request = StandardCharsets.UTF_8.decode(buffer).toString();
      buffer.reset();
      log.info("request:{}", request);
    }
    RequestLine firstLine = null;

    // request line start
    firstLine = parseRequestLine(buffer, channelContext);
    if (firstLine == null) {
      return null;
    }
    Map<String, String> headers = new HashMap<>();
    int contentLength = 0;
    byte[] bodyBytes = null;
    // request line end

    // request header start
    boolean headerCompleted = parseHeader(buffer, headers, 0, httpConfig);
    // 不论 GET 还是 POST，都 return null
    if (!headerCompleted) {
      return null;
    }
    String contentLengthStr = headers.get(RequestHeaderKey.Content_Length);

    if (StrUtil.isBlank(contentLengthStr)) {
      contentLength = 0;
    } else {
      contentLength = Integer.parseInt(contentLengthStr);
      if (contentLength > httpConfig.getMaxLengthOfPostBody()) {
        String message = "post body length is too big[" + contentLength + "], max length is " + httpConfig.getMaxLengthOfPostBody()
            + " byte";
        log.error(message);
        HttpResponse httpResponse = new HttpResponse();
        httpResponse.setStatus(413);
        httpResponse.body(message);
        Tio.bSend(channelContext, httpResponse);
        Tio.close(channelContext, "Payload Too Large");
      }
    }

    int headerLength = (buffer.position() - position);
    int allNeedLength = headerLength + contentLength; // 这个packet所需要的字节长度(含头部和体部)

    int notReceivedLength = allNeedLength - readableLength; // 尚未接收到的数据长度
    if (notReceivedLength > 0) {
      // 重置 position，使得下次 decode 时能从头开始读取已接收的数据
      if (notReceivedLength > channelContext.getReadBufferSize()) {
        channelContext.setReadBufferSize(notReceivedLength);
      }

      channelContext.setPacketNeededLength(allNeedLength);
      return null;
    }
    // request header end

    // request body start
    String realIp = HttpIpUtils.getRealIp(channelContext, httpConfig, headers);
    if (IpBlacklistUtils.isInBlacklist(channelContext.tioConfig, realIp)) {
      String message = "[" + realIp + "] in black list";
      log.warn("{}", message);

      // 直接关闭连接
      Tio.close(channelContext, null, message, ChannelCloseCode.IP_IN_BLACK_LIST);

      throw new TioDecodeException("[" + realIp + "] in black list");
    }
    if (httpConfig.checkHost) {
      if (!headers.containsKey(RequestHeaderKey.Host)) {
        throw new TioDecodeException("there is no host header");
      }
    }

    Node realNode = null;
    if (Objects.equals(realIp, channelContext.getClientNode().getHost())) {
      realNode = channelContext.getClientNode();
    } else {
      realNode = new Node(realIp, channelContext.getClientNode().getPort()); // realNode
      channelContext.setProxyClientNode(realNode);
    }

    HttpRequest httpRequest = new HttpRequest(realNode);
    httpRequest.setRequestLine(firstLine);
    httpRequest.setChannelContext(channelContext);
    httpRequest.setHttpConfig(httpConfig);
    httpRequest.setHeaders(headers);
    httpRequest.setContentLength(contentLength);

    String connection = headers.get(RequestHeaderKey.Connection);
    if (connection != null) {
      httpRequest.setConnection(connection.toLowerCase());
    }

    // 1.0
    String httpVersion = firstLine.getVersion();

    boolean keepAlive = true; // 默认值

    if ("1.1".equalsIgnoreCase(httpVersion)) {
      // 在 HTTP/1.1 中，默认保持连接，除非明确指定 'Connection: close'
      if ("close".equalsIgnoreCase(connection)) {
        keepAlive = false;
      }

    } else if ("1.0".equalsIgnoreCase(httpVersion)) {
      // 在 HTTP/1.0 中，默认关闭连接，除非明确指定 'Connection: keep-alive'
      if ("keep-alive".equalsIgnoreCase(connection)) {
        keepAlive = true;
      } else {
        keepAlive = false;
      }

    } else {
      // 对于其他 HTTP 版本，可以根据服务器策略进行处理
      // 这里默认保持连接，除非明确指定 'Connection: close'
      if ("close".equalsIgnoreCase(connection)) {
        keepAlive = false;
      }
    }

    // 设置 keepConnection 标志
    httpRequest.setKeepConnection(keepAlive);

    if (StrUtil.isNotBlank(firstLine.queryString)) {
      boolean decodeParams = decodeParams(httpRequest.getParams(), firstLine.queryString, httpRequest.getCharset(), channelContext);
      if (!decodeParams) {
        return null;
      }
    }

    if (contentLength > 0) {
      bodyBytes = new byte[contentLength];
      buffer.get(bodyBytes);
      httpRequest.setBody(bodyBytes);
      // 解析消息体
      parseBody(httpRequest, firstLine, bodyBytes, channelContext, httpConfig);
    } else {
    }
    return httpRequest;
  }

  /**
   * 继续请求参数
   * 
   * @param params
   * @param queryString
   * @param charset
   * @param channelContext
   * @author tanyaowu
   * @throws TioDecodeException
   */
  public static boolean decodeParams(Map<String, Object[]> params, String queryString, String charset, ChannelContext channelContext)
      throws TioDecodeException {
    if (StrUtil.isBlank(queryString)) {
      return true;
    }

    String[] queryStrArray = queryString.split(SysConst.STR_AMP);
    for (String pair : queryStrArray) {
      String[] keyAndValueArray = pair.split(SysConst.STR_EQ, 2);
      String queryParamValue = null;
      if (keyAndValueArray.length == 2) {
        queryParamValue = keyAndValueArray[1];

      } else if (keyAndValueArray.length > 2) {
        String errorMsg = "Invalid query parameter format in query string, contain multi ==:" + queryString;
        log.error(errorMsg);

        HttpResponse httpResponse = new HttpResponse();
        httpResponse.setStatus(HttpResponseStatus.C400);
        httpResponse.setBody(errorMsg.getBytes(StandardCharsets.UTF_8));

        // 发送响应并关闭连接
        Tio.bSend(channelContext, httpResponse);
        Tio.close(channelContext, "Invalid query parameter format");
        return false;
      }

      String key = keyAndValueArray[0];
      String value;
      if (StrUtil.isBlank(queryParamValue)) {
        value = null;
      } else {
        try {
          value = URLDecoder.decode(queryParamValue, charset);
        } catch (UnsupportedEncodingException e) {
          throw new TioDecodeException(e);
        } catch (IllegalArgumentException e) {
          // 非法的 % 编码
          String errorMsg = "Invalid URL encoding in query parameter: " + queryParamValue;
          HttpResponse httpResponse = new HttpResponse();
          httpResponse.setStatus(HttpResponseStatus.C400);
          httpResponse.setBody(errorMsg.getBytes(StandardCharsets.UTF_8));

          Tio.bSend(channelContext, httpResponse);
          Tio.close(channelContext, "Invalid URL encoding");
          return false;
        }
      }

      Object[] existValue = params.get(key);
      if (existValue != null) {
        String[] newExistValue = new String[existValue.length + 1];
        System.arraycopy(existValue, 0, newExistValue, 0, existValue.length);
        newExistValue[newExistValue.length - 1] = value;
        params.put(key, newExistValue);
      } else {
        String[] newExistValue = new String[] { value };
        params.put(key, newExistValue);
      }
    }
    return true;
  }

  /**
   * 解析消息体
   * 
   * @param httpRequest
   * @param firstLine
   * @param bodyBytes
   * @param channelContext
   * @param httpConfig
   * @throws TioDecodeException
   */
  private static void parseBody(HttpRequest httpRequest, RequestLine firstLine, byte[] bodyBytes, ChannelContext channelContext,
      HttpConfig httpConfig) throws TioDecodeException {
    parseBodyFormat(httpRequest, httpRequest.getHeaders());
    RequestBodyFormat bodyFormat = httpRequest.getBodyFormat();

    httpRequest.setBody(bodyBytes);

    switch (bodyFormat) {
    case MULTIPART:
      if (log.isInfoEnabled()) {
        String bodyString = null;
        if (bodyBytes != null && bodyBytes.length > 0) {
          if (log.isDebugEnabled()) {
            try {
              bodyString = new String(bodyBytes, httpRequest.getCharset());
              if (bodyString.length() < 2048) {
                log.debug("{} multipart body value\r\n{}", channelContext, bodyString);
              } else {
                log.debug("{} multipart body value\r\n{}", channelContext, bodyString.substring(0, 2048));
              }
            } catch (UnsupportedEncodingException e) {
              log.error(channelContext.toString(), e);
            }
          }
        }
      }

      // 【multipart/form-data; boundary=----WebKitFormBoundaryuwYcfA2AIgxqIxA0】
      String contentType = httpRequest.getHeader(RequestHeaderKey.Content_Type);
      String initboundary = HttpParseUtils.getSubAttribute(contentType, "boundary");
      if (log.isDebugEnabled()) {
        log.debug("{}, initboundary:{}", channelContext, initboundary);
      }
      HttpMultiBodyDecoder.decode(httpRequest, firstLine, bodyBytes, initboundary, channelContext, httpConfig);
      break;

    default:
      String bodyString = null;
      if (bodyBytes != null && bodyBytes.length > 0) {
        try {
          bodyString = new String(bodyBytes, httpRequest.getCharset());
          httpRequest.setBodyString(bodyString);
          if (EnvUtils.getBoolean("tio.devMode", false)) {
            if (log.isInfoEnabled()) {
              log.info("{} body value\r\n{}", channelContext, bodyString);
            }
          }

        } catch (UnsupportedEncodingException e) {
          log.error(channelContext.toString(), e);
        }
      }

      if (bodyFormat == RequestBodyFormat.URLENCODED) {
        parseUrlencoded(httpRequest, firstLine, bodyBytes, bodyString, channelContext);
      }
      break;
    }
  }

  /**
   * Content-Type : application/x-www-form-urlencoded; charset=UTF-8 Content-Type
   * : application/x-www-form-urlencoded; charset=UTF-8 在 `HttpRequest`
   * 中，请求类型通常指的是请求正文（body）的内容类型（也称为 MIME 类型）。这些类型指定了请求正文的格式，以便服务器能够正确解析。除了您提到的
   * `application/x-www-form-urlencoded` (URLENCODED), `multipart/form-data`
   * (MULTIPART), 和 `text/plain` (TEXT)，还有其他几种常见的请求类型： 1.
   * **`application/json`**：用于发送 JSON 格式的数据。在现代的 Web API 中非常常见。 2.
   * **`application/xml` 或 `text/xml`**：用于发送 XML 格式的数据。 3.
   * **`application/javascript`**：用于发送 JavaScript 代码。 4.
   * **`application/octet-stream`**：用于发送二进制数据，比如文件上传。 5. **`text/html`**：用于发送 HTML
   * 格式的数据。 6. **`application/graphql`**：用于 GraphQL 请求。 7. **`image/png`,
   * `image/jpeg` 等**：用于发送特定类型的图像数据。
   * 
   * 这些是一些常见的请求类型。实际上，请求类型可以是任何值，但为了确保数据正确解析，通常使用标准的 MIME
   * 类型。不同的服务器和应用程序可能支持不同的请求类型。
   * 
   * @param httpRequest
   * @param headers
   * @author tanyaowu
   */
  public static void parseBodyFormat(HttpRequest httpRequest, Map<String, String> headers) {
    String contentType = headers.get(RequestHeaderKey.Content_Type);
    if (contentType != null) {
      contentType = contentType.toLowerCase();
    }

    if (contentType == null) {
      httpRequest.setBodyFormat(RequestBodyFormat.URLENCODED);
    } else if (isText(contentType)) {
      httpRequest.setBodyFormat(RequestBodyFormat.TEXT);
    } else if (contentType.startsWith(HttpConst.RequestHeaderValue.Content_Type.multipart_form_data)) {
      httpRequest.setBodyFormat(RequestBodyFormat.MULTIPART);
    } else {
      httpRequest.setBodyFormat(RequestBodyFormat.URLENCODED);
    }

    if (StrUtil.isNotBlank(contentType)) {
      String charset = HttpParseUtils.getSubAttribute(contentType, "charset");
      if (StrUtil.isNotBlank(charset)) {
        httpRequest.setCharset(charset);
      } else {
        httpRequest.setCharset(SysConst.DEFAULT_ENCODING);
      }
    }
  }

  /**
   * 请求类型是否为文本
   * 
   * @param contentType
   * @return
   */
  private static boolean isText(String contentType) {
    return contentType.startsWith(HttpConst.RequestHeaderValue.Content_Type.text_plain)
        //
        || contentType.startsWith(MimeType.APPLICATION_JSON.getType());
  }

  /**
   * 读取一行数据
   * 
   * @param buffer
   * @return
   */
  private static String readLine(ByteBuffer buffer) {
    // 记录当前起始位置
    int startPosition = buffer.position();
    // 搜索 CRLF 结束符
    while (buffer.hasRemaining()) {
      byte b = buffer.get();
      if (b == SysConst.CR) {
        if (buffer.hasRemaining()) {
          byte next = buffer.get();
          if (next == SysConst.LF) {
            // 找到 CRLF，计算这一行的长度
            int endPosition = buffer.position() - 2;
            int length = endPosition - startPosition;
            byte[] lineBytes = new byte[length];
            // 恢复到起始位置，读取这一行的字节
            buffer.position(startPosition);
            buffer.get(lineBytes);
            // 跳过 CRLF
            buffer.get(); // CR
            buffer.get(); // LF
            return new String(lineBytes, StandardCharsets.UTF_8);
          } else {
            // 如果 CR 后面不是 LF，则将指针回退一位
            buffer.position(buffer.position() - 1);
          }
        }
      }
    }
    // 没有找到完整的行，恢复位置
    buffer.position(startPosition);
    return null;
  }

  /**
   * 解析请求头
   * 
   * @param buffer
   * @param headers
   * @param hasReceivedHeaderLength
   * @param httpConfig
   * @return
   * @throws TioDecodeException
   */
  public static boolean parseHeader(ByteBuffer buffer, Map<String, String> headers, int hasReceivedHeaderLength, HttpConfig httpConfig)
      throws TioDecodeException {
    // 循环读取每一行 header
    while (true) {
      // 如果没有足够数据来读取一行，则返回 false
      String line = readLine(buffer);
      if (line == null) {
        return false;
      }
      // 如果读取到空行（即仅包含 CRLF），说明 header 结束
      if (line.trim().isEmpty()) {
        return true;
      }
      // 检查单行长度是否超出限制
      if (line.length() > MAX_LENGTH_OF_HEADERLINE) {
        throw new TioDecodeException("header line is too long, max length of header line is " + MAX_LENGTH_OF_HEADERLINE);
      }
      // 累计 header 总长度检查
      hasReceivedHeaderLength += line.getBytes(StandardCharsets.UTF_8).length + 2; // 加上 CRLF
      if (hasReceivedHeaderLength > MAX_LENGTH_OF_HEADER) {
        throw new TioDecodeException("header is too long, max length of header is " + MAX_LENGTH_OF_HEADER);
      }
      // 按照冒号分割 header 名和值
      int colonIndex = line.indexOf(':');
      if (colonIndex == -1) {
        // 如果没有冒号，则认为是无效的 header 行，可以选择抛出异常或跳过
        throw new TioDecodeException("Invalid header line: " + line);
      }
      String name = line.substring(0, colonIndex).trim().toLowerCase();
      String value = line.substring(colonIndex + 1).trim();
      headers.put(name, value);
    }
  }

  /**
   * parse request line(the first line)
   * 
   * @param line           GET /tio?value=tanyaowu HTTP/1.1
   * @param channelContext
   */
  public static RequestLine parseRequestLine(ByteBuffer buffer, ChannelContext channelContext) throws TioDecodeException {
    byte[] allbs;
    int offset; // 用来统一索引偏移

    // 兼容HeapByteBuffer和DirectByteBuffer
    if (buffer.hasArray()) {
      allbs = buffer.array();
      // 通常为0，但为了严谨，使用arrayOffset
      offset = buffer.arrayOffset();
    } else {
      buffer.mark();
      allbs = new byte[buffer.remaining()];
      buffer.get(allbs);
      buffer.reset();
      // 直接缓冲区时, buffer.position要从0开始计算，所以设offset为 -position
      offset = -buffer.position();
    }

    int startPos = buffer.position() + offset;

    String methodStr = null;
    String pathStr = null;
    String queryStr = null;
    String protocol = null;
    String version = null;

    int initPosition = buffer.position();

    while (buffer.hasRemaining()) {
      byte b = buffer.get();

      if (methodStr == null) {
        if (b == SysConst.SPACE) {
          int len = buffer.position() + offset - startPos - 1;
          methodStr = StrCache.get(allbs, startPos, len);
          startPos = buffer.position() + offset;
        } else if ((buffer.position() + offset - startPos) > 10) {
          return null; // method too long
        }
        continue;
      }

      if (pathStr == null) {
        if (b == SysConst.SPACE || b == SysConst.ASTERISK) {
          int len = buffer.position() + offset - startPos - 1;
          pathStr = StrCache.get(allbs, startPos, len);
          startPos = buffer.position() + offset;

          if (b == SysConst.SPACE) {
            queryStr = SysConst.BLANK;
          }
        }
        continue;
      }

      if (queryStr == null) {
        if (b == SysConst.SPACE) {
          int len = buffer.position() + offset - startPos - 1;
          queryStr = new String(allbs, startPos, len);
          startPos = buffer.position() + offset;
        }
        continue;
      }

      if (protocol == null) {
        if (b == SysConst.BACKSLASH) {
          int len = buffer.position() + offset - startPos - 1;
          protocol = StrCache.get(allbs, startPos, len);
          startPos = buffer.position() + offset;
        }
        continue;
      }

      if (version == null) {
        if (b == SysConst.LF) {
          byte lastByte = buffer.get(buffer.position() - 2);
          int len = buffer.position() + offset - startPos - 1;
          if (lastByte == SysConst.CR) {
            len -= 1;
          }

          version = StrCache.get(allbs, startPos, len);

          RequestLine requestLine = new RequestLine();
          HttpMethod method = HttpMethod.from(methodStr);
          if (method == null) {
            throw new TioDecodeException("Unsupported HTTP method: " + methodStr);
          }
          requestLine.setMethod(method);
          requestLine.setPath(pathStr);
          requestLine.setInitPath(pathStr);
          requestLine.setQueryString(queryStr);
          requestLine.setProtocol(protocol);
          requestLine.setVersion(version);

          return requestLine;
        }
        continue;
      }
    }

    if ((buffer.position() - initPosition) > MAX_LENGTH_OF_REQUESTLINE) {
      throw new TioDecodeException("request line is too long");
    }
    return null;
  }

  /**
   * 解析URLENCODED格式的消息体 形如： 【Content-Type : application/x-www-form-urlencoded;charset=UTF-8】
   * @throws TioDecodeException
   */
  private static void parseUrlencoded(HttpRequest httpRequest, RequestLine firstLine, byte[] bodyBytes, String bodyString,
      ChannelContext channelContext) throws TioDecodeException {
    decodeParams(httpRequest.getParams(), bodyString, httpRequest.getCharset(), channelContext);
  }
}