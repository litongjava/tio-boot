package com.litongjava.tio.boot.tcp;

import java.nio.ByteBuffer;

import com.litongjava.tio.core.intf.Packet;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
public class ByteBufferPacket extends Packet {
  private static final long serialVersionUID = 1L;
  private ByteBuffer byteBuffer;

  public void setByteBuffer(ByteBuffer byteBuffer) {
    this.byteBuffer = byteBuffer;
  }

  public ByteBuffer getByteBuffer() {
    return byteBuffer;
  }
}
