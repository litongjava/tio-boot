package com.litongjava.tio.http.common;

import java.io.Serializable;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import com.litongjava.aio.Packet;
import com.litongjava.model.sys.SysConst;

/**
 * @author tanyaowu
 */
public class HttpPacket extends Packet {
  private static final long serialVersionUID = 3903186670675671956L;
  private Map<String, Serializable> props = new HashMap<>();
  private transient Map<String, Object> localProps = new HashMap<>();

  protected byte[] body;
  private String headerString = SysConst.BLANK;

  /**
   * localProps
   * 
   * @param key
   * @param value
   */
  public void setLocalAttribute(String key, Object value) {
    localProps.put(key, value);
  }

  public Object getLocalAttribute(String key) {
    return localProps.get(key);
  }

  public void removeLocalAttribute(String key) {
    localProps.remove(key);
  }

  /**
   * 获取属性
   * 
   * @param key
   * @return
   * @author tanyaowu
   */
  public Object getAttribute(String key) {
    return props.get(key);
  }

  /**
   * 
   * @param key
   * @param defaultValue
   * @return
   * @author tanyaowu
   */
  public Object getAttribute(String key, Serializable defaultValue) {
    Serializable ret = props.get(key);
    if (ret == null) {
      return defaultValue;
    }
    return ret;
  }

  /**
   * 
   * @param key
   * @author tanyaowu
   */
  public void removeAttribute(String key) {
    props.remove(key);
  }

  /**
   * 设置属性
   * 
   * @param key
   * @param value
   * @author tanyaowu
   */
  public void setAttribute(String key, Serializable value) {
    props.put(key, value);
  }

  public void setAttribute(String key, Object value) {
    props.put(key, (Serializable) value);
  }

  /**
   * 
   * @return key set
   */
  public Enumeration<String> getAttributeNames() {
    return Collections.enumeration(props.keySet());
  }

  public HttpPacket() {

  }

  /**
   * @return the body
   */
  public byte[] getBody() {
    return body;
  }

  public void setBody(byte[] body) {
    this.body = body;
  }

  public void setBody(byte byteOne) {
    this.body = new byte[] { byteOne };
  }

  public String getHeaderString() {
    return headerString;
  }

  public void setHeaderString(String headerString) {
    this.headerString = headerString;
  }
}
