package com.litongjava.tio.utils.encoder;

public class ImageVo {
  private String mimeType;
  private byte[] data;

  public String getExtension() {
    return mimeType.split("/")[1];
  }

  public ImageVo() {
    super();
  }

  public ImageVo(String mimeType, byte[] data) {
    super();
    this.mimeType = mimeType;
    this.data = data;
  }

  public String getMimeType() {
    return mimeType;
  }

  public void setMimeType(String mimeType) {
    this.mimeType = mimeType;
  }

  public byte[] getData() {
    return data;
  }

  public void setData(byte[] data) {
    this.data = data;
  }
}
