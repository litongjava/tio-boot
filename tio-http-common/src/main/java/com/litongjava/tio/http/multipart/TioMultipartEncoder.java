package com.litongjava.tio.http.multipart;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class TioMultipartEncoder {
  private static final byte[] CRLF = "\r\n".getBytes(StandardCharsets.US_ASCII);
  private static final byte[] DASHDASH = "--".getBytes(StandardCharsets.US_ASCII);

  public TioMultipartBody encode(TioMultipartParts parts) throws IOException {
    String boundary = "----tio-" + UUID.randomUUID().toString().replace("-", "");
    return encode(boundary, parts);
  }

  public TioMultipartBody encode(String boundary, TioMultipartParts parts) throws IOException {
    byte[] b = boundary.getBytes(StandardCharsets.US_ASCII);
    ByteArrayOutputStream out = new ByteArrayOutputStream(4096);

    for (TioMultipartPart part : parts.getParts()) {
      writePart(out, b, part);
    }

    // end boundary
    out.write(DASHDASH);
    out.write(b);
    out.write(DASHDASH);
    out.write(CRLF);

    return new TioMultipartBody(boundary, out.toByteArray());
  }

  private void writePart(ByteArrayOutputStream out, byte[] b, TioMultipartPart part) throws IOException {
    out.write(DASHDASH);
    out.write(b);
    out.write(CRLF);

    out.write(("Content-Type: " + part.getContentType()).getBytes(StandardCharsets.US_ASCII));
    out.write(CRLF);

    out.write(("Content-Disposition: form-data; name=\"" + part.getName() + "\"").getBytes(StandardCharsets.US_ASCII));
    out.write(CRLF);

    out.write(CRLF);
    out.write(part.getPayload());
    out.write(CRLF);
  }
}
