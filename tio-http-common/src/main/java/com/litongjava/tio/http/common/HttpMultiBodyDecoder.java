package com.litongjava.tio.http.common;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.litongjava.model.upload.UploadFile;
import com.litongjava.tio.core.ChannelContext;
import com.litongjava.tio.core.exception.LengthOverflowException;
import com.litongjava.tio.core.exception.TioDecodeException;
import com.litongjava.tio.core.utils.ByteBufferUtils;
import com.litongjava.tio.http.common.utils.HttpParseUtils;
import com.litongjava.tio.utils.hutool.StrUtil;

/**
 * 解析上传文件
 */
public class HttpMultiBodyDecoder {
  public static class Header {
    private String contentDisposition = "form-data";
    private String name = null;
    private String filename = null;
    private String contentType = null;

    private Map<String, String> map = new HashMap<>();

    public String getContentDisposition() {
      return contentDisposition;
    }

    public String getContentType() {
      return contentType;
    }

    public String getFilename() {
      return filename;
    }

    public Map<String, String> getMap() {
      return map;
    }

    public String getName() {
      return name;
    }

    public void setContentDisposition(String contentDisposition) {
      this.contentDisposition = contentDisposition;
    }

    public void setContentType(String contentType) {
      this.contentType = contentType;
    }

    public void setFilename(String filename) {
      this.filename = filename;
    }

    public void setMap(Map<String, String> map) {
      this.map = map;
    }

    public void setName(String name) {
      this.name = name;
    }
  }

  /**
   * Content-Disposition: form-data; name="uploadFile"; filename=""
   * Content-Type: application/octet-stream
   *
   * Content-Disposition: form-data; name="end"
   */
  public static interface MultiBodyHeaderKey {
    String Content_Disposition = "Content-Disposition".toLowerCase();
    String Content_Type = "Content-Type".toLowerCase();
  }

  public static enum Step {
    BOUNDARY, HEADER, BODY, END
  }

  private static Logger log = LoggerFactory.getLogger(HttpMultiBodyDecoder.class);

  /**
   * 
   * @param request
   * @param firstLine
   * @param bodyBytes
   * @param initboundary
   * @param channelContext
   * @param httpConfig
   * @throws TioDecodeException
   * @author tanyaowu
   */
  public static void decode(HttpRequest request, RequestLine firstLine, byte[] bodyBytes, String initboundary, ChannelContext channelContext, HttpConfig httpConfig) throws TioDecodeException {
    if (StrUtil.isBlank(initboundary)) {
      throw new TioDecodeException("boundary is null");
    }

    ByteBuffer buffer = ByteBuffer.wrap(bodyBytes);
    buffer.position(0);

    String boundary = "--" + initboundary;
    String endBoundary = boundary + "--";

    Step step = Step.BOUNDARY;
    try {
      label1: while (true) {
        if (step == Step.BOUNDARY) {
          String line = ByteBufferUtils.readLine(buffer, request.getCharset(), HttpConfig.MAX_LENGTH_OF_BOUNDARY);
          if (boundary.equals(line)) {
            step = Step.HEADER;
          } else if (endBoundary.equals(line)) // 结束了
          {
            break;
          } else {
            throw new TioDecodeException("line need:" + boundary + ", but is: " + line + "");
          }
        }

        Header multiBodyHeader = new Header();
        if (step == Step.HEADER) {
          List<String> lines = new ArrayList<>(2);
          label2: while (true) {
            String line = ByteBufferUtils.readLine(buffer, request.getCharset(), HttpConfig.MAX_LENGTH_OF_MULTI_HEADER);
            if ("".equals(line)) {
              break label2;
            } else {
              lines.add(line);
            }
          }

          parseHeader(lines, multiBodyHeader, channelContext);
          step = Step.BODY;
        }

        if (step == Step.BODY) {
          Step newParseStep = parseBody(multiBodyHeader, request, buffer, boundary, endBoundary, channelContext, httpConfig);
          step = newParseStep;

          if (step == Step.END) {
            break label1;
          }
        }

      }
    } catch (LengthOverflowException loe) {
      throw new TioDecodeException(loe);
    } catch (UnsupportedEncodingException e) {
      log.error(channelContext.toString(), e);
    }
  }

  /**
   * 解析请求体
   * @param header
   * @param request
   * @param buffer
   * @param boundary
   * @param endBoundary
   * @param channelContext
   * @return
   * @throws UnsupportedEncodingException
   * @throws LengthOverflowException
   * @author tanyaowu
   * @param httpConfig 
   */
  public static Step parseBody(Header header, HttpRequest request, ByteBuffer buffer, String boundary, String endBoundary, ChannelContext channelContext, HttpConfig httpConfig)
      throws UnsupportedEncodingException, LengthOverflowException, TioDecodeException {
    int initPosition = buffer.position();

    while (buffer.hasRemaining()) {
      String line = ByteBufferUtils.readLine(buffer, request.getCharset(), httpConfig.getMaxLengthOfMultiBody());
      boolean isEndBoundary = endBoundary.equals(line);
      boolean isBoundary = boundary.equals(line);
      if (isBoundary || isEndBoundary) {
        int startIndex = initPosition;
        int endIndex = buffer.position() - line.getBytes().length - 2 - 2;
        int length = endIndex - startIndex;
        byte[] dst = new byte[length];

        System.arraycopy(buffer.array(), startIndex, dst, 0, length);
        String filename = header.getFilename();
        if (filename != null)// 该字段类型是file
        {
          if (StrUtil.isNotBlank(filename)) { //
            UploadFile uploadFile = new UploadFile();
            uploadFile.setName(filename.replaceAll("%", ""));
            uploadFile.setData(dst);
            uploadFile.setSize(dst.length);
            request.addParam(header.getName(), uploadFile);
          }
        } else { // 该字段是普通的key-value
          request.addParam(header.getName(), new String(dst, request.getCharset()));
        }
        if (isEndBoundary) {
          return Step.END;
        } else {
          return Step.HEADER;
        }
      }
    }
    log.error("文件上传，协议不对，step is null");
    throw new TioDecodeException("step is null");
  }

  /**
   * Content-Disposition: form-data; name="uploadFile"; filename=""
   * Content-Type: application/octet-stream
   *
   * Content-Disposition: form-data; name="end"
  
   * @param lines
   * @param header
   * @author tanyaowu
   */
  public static void parseHeader(List<String> lines, Header header, ChannelContext channelContext) throws TioDecodeException {
    if (lines == null || lines.size() == 0) {
      throw new TioDecodeException("multipart_form_data 格式不对，没有头部信息");
    }

    try {
      for (String line : lines) {
        String[] keyvalue = line.split(":");
        String key = StrUtil.trim(keyvalue[0]).toLowerCase();//
        String value = StrUtil.trim(keyvalue[1]);
        header.map.put(key, value);
      }

      String contentDisposition = header.map.get(MultiBodyHeaderKey.Content_Disposition);
      String name = HttpParseUtils.getSubAttribute(contentDisposition, "name");// .getPerprotyEqualValue(header.map, MultiBodyHeaderKey.Content_Disposition, "value");
      String filename = HttpParseUtils.getSubAttribute(contentDisposition, "filename");// HttpParseUtils.getPerprotyEqualValue(header.map, MultiBodyHeaderKey.Content_Disposition, "filename");
      String contentType = header.map.get(MultiBodyHeaderKey.Content_Type);// .HttpParseUtils.getPerprotyEqualValue(header.map, MultiBodyHeaderKey.Content_Type, "filename");

      header.setContentDisposition(contentDisposition);
      header.setName(name);
      header.setFilename(filename);
      header.setContentType(contentType);

    } catch (Throwable e) {
      log.error(channelContext.toString(), e);
      throw new TioDecodeException(e.toString());
    }
  }

}
