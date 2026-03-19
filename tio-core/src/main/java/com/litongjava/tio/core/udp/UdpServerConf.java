package com.litongjava.tio.core.udp;

import java.util.concurrent.ExecutorService;

import com.litongjava.tio.core.Node;
import com.litongjava.tio.core.udp.intf.UdpHandler;

/**
 * @author tanyaowu
 * 2017年7月5日 下午3:53:04
 */
public class UdpServerConf extends UdpConf {
  private UdpHandler udpHandler;
  private ExecutorService handlerExecutorService;

  private int readBufferSize = 1024 * 1024;

  public UdpServerConf(int port, UdpHandler udpHandler, int timeout) {
    super(timeout);
    this.udpHandler = udpHandler;
    this.setServerNode(new Node(null, port));
  }

  public UdpServerConf(int port, UdpHandler udpHandler, int timeout, ExecutorService handlerExecutorService) {
    super(timeout);
    this.udpHandler = udpHandler;
    this.handlerExecutorService = handlerExecutorService;
    this.setServerNode(new Node(null, port));
  }
  
  public UdpServerConf(int port, UdpHandler udpHandler, int timeout, ExecutorService handlerExecutorService,
      int readBufferSize) {
    super(timeout);
    this.udpHandler = udpHandler;
    this.handlerExecutorService = handlerExecutorService;
    this.readBufferSize = readBufferSize;
    this.setServerNode(new Node(null, port));
  }

  public int getReadBufferSize() {
    return readBufferSize;
  }

  public UdpHandler getUdpHandler() {
    return udpHandler;
  }

  public void setReadBufferSize(int readBufferSize) {
    this.readBufferSize = readBufferSize;
  }

  public void setUdpHandler(UdpHandler udpHandler) {
    this.udpHandler = udpHandler;
  }

  public ExecutorService getHandlerExecutorService() {
    return handlerExecutorService;
  }

  public void setHandlerExecutorService(ExecutorService handlerExecutorService) {
    this.handlerExecutorService = handlerExecutorService;
  }

}
