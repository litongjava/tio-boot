package com.litongjava.tio.utils.qrcode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;

public class QrCodeUtils {

  /**
   * 生成二维码图片的字节数组
   *
   * @param content 二维码内容
   * @param width   二维码图片宽度
   * @param height  二维码图片高度
   * @return 字节数组，图片格式为 PNG
   * @throws WriterException 如果编码错误
   * @throws IOException     如果写入流错误
   */
  public static void generateQRCode(String content, int width, int height, ByteArrayOutputStream outputStream) {
    // 配置编码参数
    Map<EncodeHintType, Object> hints = new HashMap<>();
    hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

    // 生成二维码矩阵
    BitMatrix bitMatrix = null;
    try {
      bitMatrix = new MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, width, height, hints);
    } catch (WriterException e) {
      throw new RuntimeException(e);
    }
    // 将矩阵写入输出流
    try {
      MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
