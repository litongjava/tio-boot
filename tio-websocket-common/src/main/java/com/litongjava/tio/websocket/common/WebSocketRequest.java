package com.litongjava.tio.websocket.common;

import java.io.UnsupportedEncodingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author tanyaowu
 * 2017年7月30日 上午10:09:46
 */
public class WebSocketRequest extends WebSocketPacket {
	private static final Logger log = LoggerFactory.getLogger(WebSocketRequest.class);

	private static final long serialVersionUID = -3361865570708714596L;

	public static WebSocketRequest fromText(String text, String charset) {
		WebSocketRequest wsRequest = new WebSocketRequest();
		try {
			wsRequest.setBody(text.getBytes(charset));
		} catch (UnsupportedEncodingException e) {
			log.error(e.toString(), e);
		}
		wsRequest.setWsEof(true);
		wsRequest.setWsOpcode(Opcode.TEXT);
		return wsRequest;
	}

	public static WebSocketRequest fromBytes(byte[] bytes) {
		WebSocketRequest wsRequest = new WebSocketRequest();
		wsRequest.setBody(bytes);
		wsRequest.setWsEof(true);
		wsRequest.setWsOpcode(Opcode.BINARY);
		return wsRequest;
	}
}
