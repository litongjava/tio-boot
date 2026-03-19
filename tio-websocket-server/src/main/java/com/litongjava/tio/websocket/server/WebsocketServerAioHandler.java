package com.litongjava.tio.websocket.server;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.litongjava.aio.Packet;
import com.litongjava.tio.core.ChannelContext;
import com.litongjava.tio.core.Tio;
import com.litongjava.tio.core.TioConfig;
import com.litongjava.tio.core.exception.TioDecodeException;
import com.litongjava.tio.http.common.HeaderName;
import com.litongjava.tio.http.common.HeaderValue;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpRequestDecoder;
import com.litongjava.tio.http.common.HttpResponse;
import com.litongjava.tio.http.common.HttpResponseEncoder;
import com.litongjava.tio.http.common.HttpResponseStatus;
import com.litongjava.tio.http.common.RequestHeaderKey;
import com.litongjava.tio.server.intf.ServerAioHandler;
import com.litongjava.tio.utils.base64.Base64Utils;
import com.litongjava.tio.utils.digest.Sha1Utils;
import com.litongjava.tio.utils.hutool.StrUtil;
import com.litongjava.tio.websocket.common.Opcode;
import com.litongjava.tio.websocket.common.WebSocketRequest;
import com.litongjava.tio.websocket.common.WebSocketResponse;
import com.litongjava.tio.websocket.common.WebSocketServerDecoder;
import com.litongjava.tio.websocket.common.WebSocketServerEncoder;
import com.litongjava.tio.websocket.common.WebSocketSessionContext;
import com.litongjava.tio.websocket.server.handler.IWebSocketHandler;

public class WebsocketServerAioHandler implements ServerAioHandler {
  private static Logger log = LoggerFactory.getLogger(WebsocketServerAioHandler.class);
  /**
   * value: List<WsRequest>
   */
  private static final String NOT_FINAL_WEBSOCKET_PACKET_PARTS = "TIO_N_F_W_P_P";

  /**
   * SEC_WEBSOCKET_KEY后缀
   */
  private static final String SEC_WEBSOCKET_KEY_SUFFIX = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

  private static final byte[] SEC_WEBSOCKET_KEY_SUFFIX_BYTES = SEC_WEBSOCKET_KEY_SUFFIX.getBytes();

  private WebsocketServerConfig wsServerConfig;

  private IWebSocketHandler wsMsgHandler;

  /**
   * @param wsServerConfig
   * @param wsMsgHandler
   */
  public WebsocketServerAioHandler(WebsocketServerConfig wsServerConfig, IWebSocketHandler wsMsgHandler) {
    this.wsServerConfig = wsServerConfig;
    this.wsMsgHandler = wsMsgHandler;
  }

  @SuppressWarnings("unchecked")
  @Override
  public WebSocketRequest decode(ByteBuffer buffer, int limit, int position, int readableLength, ChannelContext channelContext) throws TioDecodeException {
    WebSocketSessionContext wsSessionContext = (WebSocketSessionContext) channelContext.get();
    // int initPosition = buffer.position();

    if (!wsSessionContext.isHandshaked()) {// 尚未握手
      HttpRequest request = HttpRequestDecoder.decode(buffer, limit, position, readableLength, channelContext, wsServerConfig);
      if (request == null) {
        return null;
      }

      HttpResponse httpResponse = upgradeWebSocketProtocol(request, channelContext);
      if (httpResponse == null) {
        throw new TioDecodeException("Failed to upgrade the HTTP protocol to the WebSocket protocol.");
      }

      wsSessionContext.setHandshakeRequest(request);
      wsSessionContext.setHandshakeResponse(httpResponse);

      WebSocketRequest wsRequestPacket = new WebSocketRequest();
      // wsRequestPacket.setHeaders(httpResponse.getHeaders());
      // wsRequestPacket.setBody(httpResponse.getBody());
      wsRequestPacket.setHandShake(true);

      return wsRequestPacket;
    }

    WebSocketRequest websocketPacket = WebSocketServerDecoder.decode(buffer, channelContext);

    if (websocketPacket != null) {
      if (!websocketPacket.isWsEof()) { // 数据包尚未完成
        List<WebSocketRequest> parts = (List<WebSocketRequest>) channelContext.getAttribute(NOT_FINAL_WEBSOCKET_PACKET_PARTS);
        if (parts == null) {
          parts = new ArrayList<>();
          channelContext.setAttribute(NOT_FINAL_WEBSOCKET_PACKET_PARTS, parts);
        }
        parts.add(websocketPacket);
      } else {
        List<WebSocketRequest> parts = (List<WebSocketRequest>) channelContext.getAttribute(NOT_FINAL_WEBSOCKET_PACKET_PARTS);
        if (parts != null) {
          channelContext.setAttribute(NOT_FINAL_WEBSOCKET_PACKET_PARTS, null);

          parts.add(websocketPacket);
          WebSocketRequest first = parts.get(0);
          websocketPacket.setWsOpcode(first.getWsOpcode());

          int allBodyLength = 0;
          for (WebSocketRequest wsRequest : parts) {
            allBodyLength += wsRequest.getBody().length;
          }

          byte[] allBody = new byte[allBodyLength];
          Integer index = 0;
          for (WebSocketRequest wsRequest : parts) {
            System.arraycopy(wsRequest.getBody(), 0, allBody, index, wsRequest.getBody().length);
            index += wsRequest.getBody().length;
          }
          websocketPacket.setBody(allBody);
        }

        HttpRequest handshakeRequest = wsSessionContext.getHandshakeRequest();
        if (websocketPacket.getWsOpcode() != Opcode.BINARY) {
          byte[] bodyBs = websocketPacket.getBody();
          if (bodyBs != null) {
            try {
              String text = new String(bodyBs, handshakeRequest.getCharset());
              websocketPacket.setWsBodyText(text);
            } catch (UnsupportedEncodingException e) {
              log.error(e.toString(), e);
            }
          }
        }
      }
    }

    return websocketPacket;
  }

  @Override
  public ByteBuffer encode(Packet packet, TioConfig tioConfig, ChannelContext channelContext) {
    WebSocketResponse wsResponse = (WebSocketResponse) packet;

    // 握手包
    if (wsResponse.isHandShake()) {
      WebSocketSessionContext imSessionContext = (WebSocketSessionContext) channelContext.get();
      HttpResponse handshakeResponse = imSessionContext.getHandshakeResponse();
      return HttpResponseEncoder.encode(handshakeResponse, tioConfig, channelContext);
    }

    ByteBuffer byteBuffer = WebSocketServerEncoder.encode(wsResponse, tioConfig, channelContext);
    return byteBuffer;
  }

  /** @return the httpConfig */
  public WebsocketServerConfig getHttpConfig() {
    return wsServerConfig;
  }

  private WebSocketResponse h(WebSocketRequest websocketPacket, byte[] bytes, Opcode opcode, ChannelContext channelContext) throws Exception {
    WebSocketResponse wsResponse = null;
    String charset = channelContext.getTioConfig().getCharset();
    if (opcode == Opcode.TEXT) {
      if (bytes == null || bytes.length == 0) {
        Tio.remove(channelContext, "Incorrect websocket packet, body is empty.");
        return null;
      }
      String text = new String(bytes, charset);
      Object retObj = wsMsgHandler.onText(websocketPacket, text, channelContext);
      String methodName = "onText";
      wsResponse = processRetObj(retObj, methodName, channelContext);
      return wsResponse;
    } else if (opcode == Opcode.BINARY) {
      if (bytes == null || bytes.length == 0) {
        Tio.remove(channelContext, "Incorrect websocket packet, body is empty.");
        return null;
      }
      Object retObj = wsMsgHandler.onBytes(websocketPacket, bytes, channelContext);
      String methodName = "onBytes";
      wsResponse = processRetObj(retObj, methodName, channelContext);
      return wsResponse;
    } else if (opcode == Opcode.PING || opcode == Opcode.PONG) {
      if (log.isDebugEnabled()) {
        log.debug("received" + opcode);
      }
      return null;
    } else if (opcode == Opcode.CLOSE) {
      Object retObj = wsMsgHandler.onClose(websocketPacket, bytes, channelContext);
      String methodName = "onClose";
      wsResponse = processRetObj(retObj, methodName, channelContext);
      return wsResponse;
    } else {
      Tio.remove(channelContext, "Incorrect websocket packet, incorrect Opcode");
      return null;
    }
  }

  @Override
  public void handler(Packet packet, ChannelContext channelContext) throws Exception {

    WebSocketRequest wsRequest = (WebSocketRequest) packet;

    if (wsRequest.isHandShake()) {// 是握手包
      WebSocketSessionContext wsSessionContext = (WebSocketSessionContext) channelContext.get();
      HttpRequest request = wsSessionContext.getHandshakeRequest();
      HttpResponse httpResponse = wsSessionContext.getHandshakeResponse();
      HttpResponse r = wsMsgHandler.handshake(request, httpResponse, channelContext);
      if (r == null) {
        Tio.remove(channelContext, "The business layer does not agree to handshake.");
        return;
      }
      wsSessionContext.setHandshakeResponse(r);

      WebSocketResponse wsResponse = new WebSocketResponse();
      wsResponse.setHandShake(true);
      if (wsResponse.isBlockSend()) {
        Tio.bSend(channelContext, wsResponse);
      } else {
        Tio.send(channelContext, wsResponse);
      }

      wsSessionContext.setHandshaked(true);

      wsMsgHandler.onAfterHandshaked(request, httpResponse, channelContext);
      return;
    }

    if (!wsRequest.isWsEof()) {
      return;
    }

    WebSocketResponse wsResponse = h(wsRequest, wsRequest.getBody(), wsRequest.getWsOpcode(), channelContext);

    if (wsResponse != null) {
      Tio.send(channelContext, wsResponse);
    }

    return;
  }

  private WebSocketResponse processRetObj(Object obj, String methodName, ChannelContext channelContext) throws Exception {
    String charset = channelContext.getTioConfig().getCharset();
    WebSocketResponse wsResponse = null;
    if (obj == null) {
      return null;
    } else {
      if (obj instanceof String) {
        String str = (String) obj;
        wsResponse = WebSocketResponse.fromText(str, charset);
        return wsResponse;
      } else if (obj instanceof byte[]) {
        wsResponse = WebSocketResponse.fromBytes((byte[]) obj);
        return wsResponse;
      } else if (obj instanceof WebSocketResponse) {
        return (WebSocketResponse) obj;
      } else if (obj instanceof ByteBuffer) {
        byte[] bs = ((ByteBuffer) obj).array();
        wsResponse = WebSocketResponse.fromBytes(bs);
        return wsResponse;
      } else {
        log.error("{} {}.{}()方法，只允许返回byte[]、ByteBuffer、WsResponse或null，但是程序返回了{}", channelContext, this.getClass().getName(), methodName, obj.getClass().getName());
        return null;
      }
    }
  }

  /** @param httpConfig the httpConfig to set */
  public void setHttpConfig(WebsocketServerConfig httpConfig) {
    this.wsServerConfig = httpConfig;
  }

  /**
   * 本方法改编自baseio: https://gitee.com/generallycloud/baseio<br>
   * 感谢开源作者的付出
   *
   * @param request
   * @param channelContext
   * @return
   * @author tanyaowu
   */
  public static HttpResponse upgradeWebSocketProtocol(HttpRequest request, ChannelContext channelContext) {
    Map<String, String> headers = request.getHeaders();

    String Sec_WebSocket_Key = headers.get(RequestHeaderKey.Sec_WebSocket_Key);

    if (StrUtil.isNotBlank(Sec_WebSocket_Key)) {
      byte[] Sec_WebSocket_Key_Bytes = null;
      try {
        Sec_WebSocket_Key_Bytes = Sec_WebSocket_Key.getBytes(request.getCharset());
      } catch (UnsupportedEncodingException e) {
        //				log.error(e.toString(), e);
        throw new RuntimeException(e);
      }
      byte[] allBs = new byte[Sec_WebSocket_Key_Bytes.length + SEC_WEBSOCKET_KEY_SUFFIX_BYTES.length];
      System.arraycopy(Sec_WebSocket_Key_Bytes, 0, allBs, 0, Sec_WebSocket_Key_Bytes.length);
      System.arraycopy(SEC_WEBSOCKET_KEY_SUFFIX_BYTES, 0, allBs, Sec_WebSocket_Key_Bytes.length, SEC_WEBSOCKET_KEY_SUFFIX_BYTES.length);

      //			String Sec_WebSocket_Key_Magic = Sec_WebSocket_Key + SEC_WEBSOCKET_KEY_SUFFIX_BYTES;
      byte[] key_array = Sha1Utils.digest(allBs);
      String acceptKey = Base64Utils.encodeToString(key_array);
      HttpResponse httpResponse = new HttpResponse(request);

      httpResponse.setStatus(HttpResponseStatus.C101);

      Map<HeaderName, HeaderValue> respHeaders = new HashMap<>();
      respHeaders.put(HeaderName.Connection, HeaderValue.Connection.Upgrade);
      respHeaders.put(HeaderName.Upgrade, HeaderValue.Upgrade.WebSocket);
      respHeaders.put(HeaderName.Sec_WebSocket_Accept, HeaderValue.from(acceptKey));
      httpResponse.addHeaders(respHeaders);
      return httpResponse;
    }
    return null;
  }
}
