package com.litongjava.tio.core.vo;

import java.nio.ByteBuffer;

public class WriteCompletionVo {
  private ByteBuffer byteBuffer;
  private Object obj;
  private int totalWritten;

  public WriteCompletionVo(ByteBuffer byteBuffer, Object obj) {
    this.byteBuffer = byteBuffer;
    this.obj = obj;
  }

  public ByteBuffer getByteBuffer() {
    return byteBuffer;
  }

  public void setByteBuffer(ByteBuffer byteBuffer) {
    this.byteBuffer = byteBuffer;
  }

  public Object getObj() {
    return obj;
  }

  public void setObj(Object obj) {
    this.obj = obj;
  }

  public int getTotalWritten() {
    return totalWritten;
  }

  public void setTotalWritten(int totalWritten) {
    this.totalWritten = totalWritten;
  }

}
