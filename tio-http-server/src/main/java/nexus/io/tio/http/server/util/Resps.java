package nexus.io.tio.http.server.util;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.util.Date;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nexus.io.tio.http.common.HeaderName;
import nexus.io.tio.http.common.HeaderValue;
import nexus.io.tio.http.common.HttpConfig;
import nexus.io.tio.http.common.HttpRequest;
import nexus.io.tio.http.common.HttpResource;
import nexus.io.tio.http.common.HttpResponse;
import nexus.io.tio.http.common.HttpResponseStatus;
import nexus.io.tio.http.common.MimeType;
import nexus.io.tio.http.common.RequestHeaderKey;
import nexus.io.tio.http.common.RequestLine;
import nexus.io.tio.utils.environment.EnvUtils;
import nexus.io.tio.utils.hutool.ClassUtil;
import nexus.io.tio.utils.hutool.FileUtil;
import nexus.io.tio.utils.hutool.StrUtil;
import nexus.io.tio.utils.json.Json;

/**
 * @author tanyaowu 2017年6月29日 下午4:17:24
 */
public class Resps {
  private static final Logger log = LoggerFactory.getLogger(Resps.class);

  /** Office Open XML 的 Content-Type，MimeType 中暂无对应常量 */
  public static final String CONTENT_TYPE_EXCEL = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
  public static final String CONTENT_TYPE_EXCEL_XLS = "application/vnd.ms-excel";
  public static final String CONTENT_TYPE_DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
  public static final String CONTENT_TYPE_PPTX = "application/vnd.openxmlformats-officedocument.presentationml.presentation";
  
  /**
   * 构建css响应 Content-Type: text/css;charset=utf-8
   *
   * @param request
   * @param bodyString
   * @return
   */
  public static HttpResponse css(HttpRequest request, String bodyString) {
    String charset = request.getCharset();
    return css(request, bodyString, charset);
  }

  /**
   * 构建css响应 Content-Type: text/css;charset=utf-8
   *
   * @param request
   * @param bodyString
   * @param charset
   * @return
   */
  public static HttpResponse css(HttpRequest request, String bodyString, String charset) {
    HttpResponse ret = string(request, bodyString, charset, getMimeTypeStr(MimeType.TEXT_CSS_CSS, charset));
    return ret;
  }

  /**
   * 根据byte[]创建响应
   *
   * @param request
   * @param bodyBytes
   * @param extension 后缀，可以为空
   * @return
   */
  public static HttpResponse bytes(HttpRequest request, byte[] bodyBytes, String extension) {
    String contentType = null;
    // String extension = FilenameUtils.getExtension(filename);
    if (StrUtil.isNotBlank(extension)) {
      MimeType mimeType = MimeType.fromExtension(extension);
      if (mimeType != null) {
        contentType = mimeType.getType();
      } else {
        contentType = "application/octet-stream";
      }
    }
    return bytesWithContentType(request, bodyBytes, contentType);
  }

  /**
   * @param response
   * @param byteOne
   * @param extension
   * @return
   */
  public static HttpResponse bytes(HttpResponse response, Byte byteOne, String extension) {
    String contentType = null;
    if (StrUtil.isNotBlank(extension)) {
      MimeType mimeType = MimeType.fromExtension(extension);
      if (mimeType != null) {
        contentType = mimeType.getType();
      } else {
        contentType = "application/octet-stream";
      }
    }
    return bytesWithContentType(response, byteOne, contentType);

  }

  /**
   * @param response
   * @param bodyBytes
   * @param extension
   * @return
   */
  public static HttpResponse bytes(HttpResponse response, byte[] bodyBytes, String extension) {
    String contentType = null;
    // String extension = FilenameUtils.getExtension(filename);
    if (StrUtil.isNotBlank(extension)) {
      MimeType mimeType = MimeType.fromExtension(extension);
      if (mimeType != null) {
        contentType = mimeType.getType();
      } else {
        contentType = "application/octet-stream";
      }
    }
    return bytesWithContentType(response, bodyBytes, contentType);
  }

  /**
   * 根据文件创建响应
   *
   * @param request
   * @param fileOnServer
   * @return
   * @throws IOException
   */
  public static HttpResponse file(HttpRequest request, File fileOnServer) throws Exception {
    if (fileOnServer == null || !fileOnServer.exists()) {
      return request.httpConfig.getHttpRequestHandler().resp404(request, request.getRequestLine());
    }

    Date lastModified = new Date(fileOnServer.lastModified());
    HttpResponse ret = try304(request, lastModified.getTime());
    if (ret != null) {
      return ret;
    }

    byte[] bodyBytes = Files.readAllBytes(fileOnServer.toPath());
    String filename = fileOnServer.getName();
    String extension = FileUtil.extName(filename);
    ret = bytes(request, bodyBytes, extension);
    ret.setLastModified(HeaderValue.from(lastModified.getTime() + ""));
    return ret;
  }

  /**
   * @param request
   * @param path
   * @return
   * @throws Exception
   */
  public static HttpResponse file(HttpRequest request, String path) throws Exception {
    HttpResource httpResource = request.httpConfig.getResource(request, path);
    if (httpResource == null) {
      return null;
    } else {
      path = httpResource.getPath();
      File file = httpResource.getFile();
      if (file != null) {
        return file(request, file);
      }

      URL url = httpResource.getUrl();
      if (url != null) {
        byte[] bs = FileUtil.readBytes(url);
        return Resps.bytes(request, bs, FileUtil.extName(path));
      }
      return Resps.resp404(request);
    }
  }

  /**
   * @param request
   * @param requestLine
   * @param httpConfig
   * @return
   * @throws Exception
   */
  public static HttpResponse resp404(HttpRequest request, RequestLine requestLine, HttpConfig httpConfig)
      throws Exception {
    String file404 = httpConfig.getPage404();
    HttpResource httpResource = request.httpConfig.getResource(request, file404);
    if (httpResource != null) {
      file404 = httpResource.getPath();
      String charset = request.getCharset();
      HttpResponse ret = Resps.forward(request,
          file404 + "?tio_initpath=" + URLEncoder.encode(requestLine.getPathAndQuery(), charset));
      return ret;
    }
    HttpResponse ret = Resps.html(request, "404");
    ret.setStatus(HttpResponseStatus.C404);
    return ret;
  }

  public static HttpResponse resp404(HttpResponse response, RequestLine requestLine, HttpConfig httpConfig)
      throws Exception {
    String file404 = httpConfig.getPage404();
    HttpRequest request = response.getHttpRequest();
    HttpResource httpResource = httpConfig.getResource(request, file404);
    if (httpResource != null) {
      file404 = httpResource.getPath();
      String charset = response.getCharset();
      HttpResponse ret = Resps.forward(request,
          file404 + "?tio_initpath=" + URLEncoder.encode(requestLine.getPathAndQuery(), charset));
      return ret;
    }
    HttpResponse ret = Resps.html(response, "404");
    ret.setStatus(HttpResponseStatus.C404);
    return ret;
  }

  /**
   * @param request
   * @return
   * @throws Exception
   */
  public static HttpResponse resp404(HttpRequest request) throws Exception {
    return resp404(request, request.requestLine, request.httpConfig);
  }

  /**
   *
   * @param request
   * @param requestLine
   * @param httpConfig
   * @param throwable
   * @return
   * @throws Exception
   */
  public static HttpResponse resp500(HttpRequest request, RequestLine requestLine, HttpConfig httpConfig,
      Throwable throwable) throws Exception {
    String file500 = httpConfig.getPage500();
    HttpResource httpResource = request.httpConfig.getResource(request, file500);

    if (httpResource != null) {
      file500 = httpResource.getPath();
      HttpResponse ret = Resps.forward(request, file500 + "?tio_initpath=" + requestLine.getPathAndQuery());
      return ret;
    }

    HttpResponse ret = null;
    if (EnvUtils.getBoolean("http.response.showExceptionDetails", false)) {
      // 获取完整的堆栈跟踪
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      throwable.printStackTrace(pw);
      ret = Resps.txt(request, sw.toString());
    } else {
      ret = Resps.html(request, "500");
    }
    ret.setStatus(HttpResponseStatus.C500);
    return ret;
  }

  /**
   * @param request
   * @param throwable
   * @return
   * @throws Exception
   */
  public static HttpResponse resp500(HttpRequest request, Throwable throwable) throws Exception {
    return resp500(request, request.requestLine, request.httpConfig, throwable);
  }

  /**
   * @param request
   * @param bodyBytes
   * @param contentType 形如:application/octet-stream等
   * @return
   */
  public static HttpResponse bytesWithContentType(HttpRequest request, byte[] bodyBytes, String contentType) {
    HttpResponse ret = new HttpResponse(request);
    ret.setBody(bodyBytes);

    if (StrUtil.isBlank(contentType)) {
      ret.addHeader(HeaderName.Content_Type, HeaderValue.Content_Type.DEFAULT_TYPE);
    } else {
      ret.addHeader(HeaderName.Content_Type, HeaderValue.Content_Type.from(contentType));
    }
    return ret;
  }

  public static HttpResponse imagePng(HttpResponse ret, byte[] bodyBytes) {
    ret.setBody(bodyBytes);
    ret.setContentType(MimeType.IMAGE_PNG_PNG.getType());
    return ret;
  }

  public static HttpResponse imageJpeg(HttpResponse ret, byte[] bodyBytes) {
    ret.setBody(bodyBytes);
    ret.setContentType(MimeType.IMAGE_JPEG_JPG.getType());
    return ret;
  }

  /**
   * @param response
   * @param byteOne
   * @param contentType
   * @return
   */
  public static HttpResponse bytesWithContentType(HttpResponse response, byte byteOne, String contentType) {
    response.setBody(byteOne);

    if (StrUtil.isBlank(contentType)) {
      response.addHeader(HeaderName.Content_Type, HeaderValue.Content_Type.DEFAULT_TYPE);
    } else {
      response.addHeader(HeaderName.Content_Type, HeaderValue.Content_Type.from(contentType));
    }
    return response;
  }

  public static HttpResponse bytesWithContentType(HttpResponse response, byte[] bodyBytes, String contentType) {
    response.setBody(bodyBytes);

    if (StrUtil.isBlank(contentType)) {
      response.addHeader(HeaderName.Content_Type, HeaderValue.Content_Type.DEFAULT_TYPE);
    } else {
      response.addHeader(HeaderName.Content_Type, HeaderValue.Content_Type.from(contentType));
    }
    return response;
  }

  /**
   * 按附件下载：设置 Content-Type 与 Content-Disposition（文件名按 UTF-8 URL 编码，支持中文名）
   *
   * @param response
   * @param bodyBytes
   * @param contentType
   * @param filename    为空时不加 Content-Disposition
   * @return
   */
  public static HttpResponse download(HttpResponse response, byte[] bodyBytes, String contentType, String filename) {
    bytesWithContentType(response, bodyBytes, contentType);
    return contentDisposition(response, filename);
  }

  /**
   * Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet（xlsx）
   */
  public static HttpResponse excel(HttpResponse response, byte[] bodyBytes) {
    return bytesWithContentType(response, bodyBytes, CONTENT_TYPE_EXCEL);
  }

  /**
   * 导出 Excel（xlsx）并按附件下载
   */
  public static HttpResponse excel(HttpResponse response, byte[] bodyBytes, String filename) {
    return download(response, bodyBytes, CONTENT_TYPE_EXCEL, filename);
  }

  /**
   * 导出 Excel（xls）并按附件下载
   */
  public static HttpResponse excelXls(HttpResponse response, byte[] bodyBytes, String filename) {
    return download(response, bodyBytes, CONTENT_TYPE_EXCEL_XLS, filename);
  }

  /**
   * 按扩展名输出图片（png、jpg、jpeg、gif、bmp、svg 等），识别不出时按二进制流输出
   */
  public static HttpResponse image(HttpResponse response, byte[] bodyBytes, String extension) {
    String contentType = null;
    if (StrUtil.isNotBlank(extension)) {
      MimeType mimeType = MimeType.fromExtension(extension);
      if (mimeType != null && mimeType.getType().startsWith("image/")) {
        contentType = mimeType.getType();
      }
    }
    if (contentType == null) {
      contentType = "application/octet-stream";
    }
    return bytesWithContentType(response, bodyBytes, contentType);
  }

  /**
   * 按附件下载图片，Content-Type 由扩展名决定
   */
  public static HttpResponse image(HttpResponse response, byte[] bodyBytes, String extension, String filename) {
    image(response, bodyBytes, extension);
    return contentDisposition(response, filename);
  }

  /**
   * Content-Type: application/pdf
   */
  public static HttpResponse pdf(HttpResponse response, byte[] bodyBytes) {
    return bytesWithContentType(response, bodyBytes, MimeType.APPLICATION_PDF_PDF.getType());
  }

  /**
   * 导出 PDF 并按附件下载
   */
  public static HttpResponse pdf(HttpResponse response, byte[] bodyBytes, String filename) {
    return download(response, bodyBytes, MimeType.APPLICATION_PDF_PDF.getType(), filename);
  }

  /**
   * Content-Type: application/vnd.openxmlformats-officedocument.wordprocessingml.document（docx）
   */
  public static HttpResponse docx(HttpResponse response, byte[] bodyBytes) {
    return bytesWithContentType(response, bodyBytes, CONTENT_TYPE_DOCX);
  }

  /**
   * 导出 docx 并按附件下载
   */
  public static HttpResponse docx(HttpResponse response, byte[] bodyBytes, String filename) {
    return download(response, bodyBytes, CONTENT_TYPE_DOCX, filename);
  }

  /**
   * Content-Type: application/msword（doc）
   */
  public static HttpResponse doc(HttpResponse response, byte[] bodyBytes) {
    return bytesWithContentType(response, bodyBytes, MimeType.APPLICATION_MSWORD_DOC.getType());
  }

  /**
   * 导出 doc 并按附件下载
   */
  public static HttpResponse doc(HttpResponse response, byte[] bodyBytes, String filename) {
    return download(response, bodyBytes, MimeType.APPLICATION_MSWORD_DOC.getType(), filename);
  }

  /**
   * Content-Type: application/vnd.openxmlformats-officedocument.presentationml.presentation（pptx）
   */
  public static HttpResponse pptx(HttpResponse response, byte[] bodyBytes) {
    return bytesWithContentType(response, bodyBytes, CONTENT_TYPE_PPTX);
  }

  /**
   * 导出 pptx 并按附件下载
   */
  public static HttpResponse pptx(HttpResponse response, byte[] bodyBytes, String filename) {
    return download(response, bodyBytes, CONTENT_TYPE_PPTX, filename);
  }

  /**
   * Content-Type: application/vnd.ms-powerpoint（ppt）
   */
  public static HttpResponse ppt(HttpResponse response, byte[] bodyBytes) {
    return bytesWithContentType(response, bodyBytes, MimeType.APPLICATION_MSPOWERPOINT_PPT.getType());
  }

  /**
   * 导出 ppt 并按附件下载
   */
  public static HttpResponse ppt(HttpResponse response, byte[] bodyBytes, String filename) {
    return download(response, bodyBytes, MimeType.APPLICATION_MSPOWERPOINT_PPT.getType(), filename);
  }

  /**
   * 追加 Content-Disposition 响应头，文件名按 UTF-8 URL 编码
   */
  public static HttpResponse contentDisposition(HttpResponse response, String filename) {
    if (StrUtil.isNotBlank(filename)) {
      try {
        String encodedFileName = URLEncoder.encode(filename, "UTF-8").replace("+", "%20");
        response.addHeader(HeaderName.Content_Disposition, HeaderValue.from("attachment;filename=" + encodedFileName));
      } catch (UnsupportedEncodingException e) {
        throw new RuntimeException(e);
      }
    }
    return response;
  }

  /**
   * @param request
   * @param bodyBytes
   * @param headers
   * @return
   */
  public static HttpResponse bytesWithHeaders(HttpRequest request, byte[] bodyBytes,
      Map<HeaderName, HeaderValue> headers) {
    HttpResponse ret = new HttpResponse(request);
    ret.setBody(bodyBytes);
    ret.addHeaders(headers);
    return ret;
  }

  /**
   * @param request
   * @param bodyString
   * @return
   */
  public static HttpResponse html(HttpRequest request, String bodyString) {
    String charset = request.getCharset();
    return html(request, bodyString, charset);
  }

  /**
   * @param response
   * @param bodyString
   * @return
   */
  public static HttpResponse html(HttpResponse response, String bodyString) {
    String charset = response.getCharset();
    return html(response, bodyString, charset);
  }

  public static HttpResponse svg(HttpResponse response, String bodyString) {
    String charset = response.getCharset();
    return svg(response, bodyString, charset);
  }

  public static HttpResponse xml(HttpResponse response, String bodyString) {
    String charset = response.getCharset();
    return xml(response, bodyString, charset);
  }

  /**
   * @param request
   * @param newPath
   * @return
   * @throws Exception
   */
  public static HttpResponse forward(HttpRequest request, String newPath) throws Exception {
    return request.forward(newPath);
  }

  /**
   * Content-Type: text/html;charset=utf-8
   *
   * @param request
   * @param bodyString
   * @param charset
   * @return
   */
  public static HttpResponse html(HttpRequest request, String bodyString, String charset) {
    HttpResponse ret = string(request, bodyString, charset, getMimeTypeStr(MimeType.TEXT_HTML_HTML, charset));
    return ret;
  }

  /**
   * @param response
   * @param bodyString
   * @param charset
   * @return
   */
  public static HttpResponse html(HttpResponse response, String bodyString, String charset) {
    return string(response, bodyString, charset, getMimeTypeStr(MimeType.TEXT_HTML_HTML, charset));
  }

  public static HttpResponse svg(HttpResponse response, String bodyString, String charset) {
    return string(response, bodyString, charset, getMimeTypeStr(MimeType.IMAGE_SVG_SVG, charset));
  }

  public static HttpResponse xml(HttpResponse response, String bodyString, String charset) {
    return string(response, bodyString, charset, getMimeTypeStr(MimeType.APPLICATION_XML_XML, charset));
  }

  /**
   * Content-Type: application/javascript;charset=utf-8
   *
   * @param request
   * @param bodyString
   * @return
   */
  public static HttpResponse js(HttpRequest request, String bodyString) {
    String charset = request.getCharset();
    return js(request, bodyString, charset);
  }

  /**
   * Content-Type: application/javascript;charset=utf-8
   *
   * @param request
   * @param bodyString
   * @param charset
   * @return
   */
  public static HttpResponse js(HttpRequest request, String bodyString, String charset) {
    HttpResponse ret = string(request, bodyString, charset,
        getMimeTypeStr(MimeType.APPLICATION_JAVASCRIPT_JS, charset));
    return ret;
  }

  /**
   * Content-Type: application/json;charset=utf-8
   *
   * @param request
   * @param body
   * @return
   */
  public static HttpResponse json(HttpRequest request, Object body) {
    String charset = request.getCharset();
    return json(request, body, charset);
  }

  public static HttpResponse json(HttpResponse response, Object body) {
    String charset = response.getCharset();
    return json(response, body, charset);
  }

  /**
   * Content-Type: application/json;charset=utf-8
   *
   * @param request
   * @param body
   * @param charset
   * @return
   */
  public static HttpResponse json(HttpRequest request, Object body, String charset) {
    HttpResponse ret = null;
    if (body == null) {
      ret = string(request, "", charset, getMimeTypeStr(MimeType.APPLICATION_JSON, charset));
    } else {
      if (body.getClass() == String.class || ClassUtil.isBasicType(body.getClass())) {
        ret = string(request, body + "", charset, getMimeTypeStr(MimeType.APPLICATION_JSON, charset));
      } else {
        ret = string(request, Json.getJson().toJson(body), charset, getMimeTypeStr(MimeType.APPLICATION_JSON, charset));
      }
    }
    return ret;
  }

  public static HttpResponse json(HttpResponse response, Object body, String charset) {
    if (body == null) {
      response = string(response, "", charset, getMimeTypeStr(MimeType.APPLICATION_JSON, charset));
    } else {
      if (body.getClass() == String.class || ClassUtil.isBasicType(body.getClass())) {
        response = string(response, body + "", charset, getMimeTypeStr(MimeType.APPLICATION_JSON, charset));
      } else {
        response = string(response, Json.getJson().toJson(body), charset,
            getMimeTypeStr(MimeType.APPLICATION_JSON, charset));
      }
    }
    return response;
  }

  public static String getMimeTypeStr(MimeType mimeType, String charset) {
    if (charset == null) {
      return mimeType.getType();
    } else {
      return mimeType.getType() + ";charset=" + charset;
    }
  }

  /**
   * 重定向
   *
   * @param request
   * @param path
   * @return
   */
  public static HttpResponse redirect(HttpRequest request, String path) {
    return redirect(request, path, HttpResponseStatus.C302);
  }

  /**
   * 永久重定向
   *
   * @param request
   * @param path
   * @return
   */
  public static HttpResponse redirectForever(HttpRequest request, String path) {
    return redirect(request, path, HttpResponseStatus.C301);
  }

  /**
   * @param request
   * @param path
   * @param status
   * @return
   */
  public static HttpResponse redirect(HttpRequest request, String path, HttpResponseStatus status) {
    HttpResponse ret = new HttpResponse(request);
    ret.setStatus(status);
    ret.addHeader(HeaderName.Location, HeaderValue.from(path));
    return ret;
  }

  /**
   * 用页面重定向
   *
   * @param request
   * @param path
   * @return
   * @author tanyaowu
   */
  public static HttpResponse redirectWithPage(HttpRequest request, String path) {
    StringBuilder sb = new StringBuilder(256);
    sb.append("<script>");
    sb.append("window.location.href='").append(path).append("'");
    sb.append("</script>");

    return Resps.html(request, sb.toString());

  }

  /**
   * 创建字符串输出
   *
   * @param request
   * @param bodyString
   * @param Content_Type
   * @return
   */
  public static HttpResponse string(HttpRequest request, String bodyString, String Content_Type) {
    String charset = request.getCharset();
    return string(request, bodyString, charset, Content_Type);
  }

  /**
   * 创建字符串输出
   *
   * @param request
   * @param bodyString
   * @param charset
   * @param mimeTypeStr
   * @return
   */
  public static HttpResponse string(HttpRequest request, String bodyString, String charset, String mimeTypeStr) {
    HttpResponse ret = new HttpResponse(request);

    if (bodyString != null) {
      if (charset == null) {
        ret.setBody(bodyString.getBytes());
      } else {
        try {
          ret.setBody(bodyString.getBytes(charset));
        } catch (UnsupportedEncodingException e) {
          log.error(e.toString(), e);
        }
      }
    }
    ret.addHeader(HeaderName.Content_Type, HeaderValue.Content_Type.from(mimeTypeStr));
    return ret;
  }

  public static HttpResponse string(HttpResponse response, String bodyString, String charset, String mimeTypeStr) {
    if (bodyString != null) {
      if (charset == null) {
        response.setBody(bodyString.getBytes());
      } else {
        try {
          response.setBody(bodyString.getBytes(charset));
        } catch (UnsupportedEncodingException e) {
          log.error(e.toString(), e);
        }
      }
    }
    response.addHeader(HeaderName.Content_Type, HeaderValue.Content_Type.from(mimeTypeStr));
    return response;
  }

  /**
   * 尝试返回304，这个会new一个HttpResponse返回
   *
   * @param request
   * @param lastModifiedOnServer 服务器中资源的lastModified
   * @return
   */
  public static HttpResponse try304(HttpRequest request, long lastModifiedOnServer) {
    String If_Modified_Since = request.getHeader(RequestHeaderKey.If_Modified_Since);// If-Modified-Since
    if (StrUtil.isNotBlank(If_Modified_Since)) {
      Long If_Modified_Since_Date = null;
      try {
        If_Modified_Since_Date = Long.parseLong(If_Modified_Since);

        if (lastModifiedOnServer <= If_Modified_Since_Date) {
          HttpResponse ret = new HttpResponse(request);
          ret.setStatus(HttpResponseStatus.C304);
          return ret;
        }
      } catch (NumberFormatException e) {
        log.warn("{}, {} is not an int，client:{}", request.getClientIp(), If_Modified_Since, request.getUserAgent());
        return null;
      }
    }

    return null;
  }

  /**
   * Content-Type: text/plain;charset=utf-8
   *
   * @param request
   * @param bodyString
   * @return
   */
  public static HttpResponse txt(HttpRequest request, String bodyString) {
    String charset = request.getCharset();
    return txt(request, bodyString, charset);
  }

  /**
   * @param reqponse
   * @param bodyString
   * @return
   */
  public static HttpResponse txt(HttpResponse response, String bodyString) {
    String charset = response.getCharset();
    return txt(response, bodyString, charset);
  }

  public static HttpResponse text(HttpResponse response, String bodyString) {
    String charset = response.getCharset();
    return txt(response, bodyString, charset);
  }

  /**
   * Content-Type: text/plain;charset=utf-8
   *
   * @param request
   * @param bodyString
   * @param charset
   */
  public static HttpResponse txt(HttpRequest request, String bodyString, String charset) {
    HttpResponse ret = string(request, bodyString, charset, getMimeTypeStr(MimeType.TEXT_PLAIN_TXT, charset));
    return ret;
  }

  public static HttpResponse txt(HttpResponse response, String bodyString, String charset) {
    return string(response, bodyString, charset, getMimeTypeStr(MimeType.TEXT_PLAIN_TXT, charset));
  }

  public static HttpResponse fail(HttpResponse response, String string) {
    response.setStatus(HttpResponseStatus.C400);
    response.body(string);
    return response;
  }

  public static HttpResponse error(HttpResponse response, String string) {
    response.setStatus(HttpResponseStatus.C500);
    response.body(string);
    return response;
  }

}