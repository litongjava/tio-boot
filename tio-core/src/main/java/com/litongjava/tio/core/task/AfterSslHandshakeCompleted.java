package com.litongjava.tio.core.task;

import java.util.concurrent.ConcurrentLinkedQueue;

import com.litongjava.aio.Packet;

public class AfterSslHandshakeCompleted {
  
  private ConcurrentLinkedQueue<Packet> forSendAfterSslHandshakeCompleted = null;
  public ConcurrentLinkedQueue<Packet> getForSendAfterSslHandshakeCompleted(boolean forceCreate) {
    if (forSendAfterSslHandshakeCompleted == null && forceCreate) {
      synchronized (this) {
        if (forSendAfterSslHandshakeCompleted == null) {
          forSendAfterSslHandshakeCompleted = new ConcurrentLinkedQueue<>();
        }
      }
    }

    return forSendAfterSslHandshakeCompleted;
  }

}
