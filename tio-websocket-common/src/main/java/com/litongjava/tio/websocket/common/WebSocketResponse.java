package com.litongjava.tio.websocket.common;

import java.io.UnsupportedEncodingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.litongjava.tio.utils.json.JsonUtils;

/**
 *
 * @author tanyaowu
 * 2017年7月30日 上午10:09:59
 */
public class WebSocketResponse extends WebSocketPacket {
  private static Logger log = LoggerFactory.getLogger(WebSocketResponse.class);

  private static final long serialVersionUID = 963847148301021559L;

  public WebSocketResponse() {

  }

  /**
   * 不要疑惑,虽然传入的是自己类型,但是设置是Opcode.TEXT,如果需要纯自己数据,请调用fromBytes方法
   * @param jsonBytes
   */
  public WebSocketResponse(byte[] jsonBytes) {
    this.setBody(jsonBytes);
    this.setWsOpcode(Opcode.TEXT);
  }

  public WebSocketResponse(String text) {
    this.setBody(text.getBytes());
    this.setWsOpcode(Opcode.TEXT);
  }

  public static WebSocketResponse fromJson(Object data) {
    WebSocketResponse wsResponse = new WebSocketResponse();
    String text = JsonUtils.toJson(data);
    wsResponse.setBody(text.getBytes());
    wsResponse.setWsOpcode(Opcode.TEXT);
    return wsResponse;
  }

  public static WebSocketResponse fromText(String text) {
    WebSocketResponse wsResponse = new WebSocketResponse();
    wsResponse.setBody(text.getBytes());
    wsResponse.setWsOpcode(Opcode.TEXT);
    return wsResponse;
  }

  public static WebSocketResponse fromText(String text, String charset) {
    WebSocketResponse wsResponse = new WebSocketResponse();
    try {
      wsResponse.setBody(text.getBytes(charset));
    } catch (UnsupportedEncodingException e) {
      log.error(e.toString(), e);
    }
    wsResponse.setWsOpcode(Opcode.TEXT);
    return wsResponse;
  }

  public static WebSocketResponse fromBytes(byte[] bytes) {
    WebSocketResponse wsResponse = new WebSocketResponse();
    wsResponse.setBody(bytes);
    wsResponse.setWsOpcode(Opcode.BINARY);
    return wsResponse;
  }
}
