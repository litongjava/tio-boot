package com.litongjava.tio.utils.email;

import javax.mail.MessagingException;
import javax.mail.Transport;

public class MailIOUtils {
  public static void closeQuietly(Transport closeable) {
    if (closeable != null) {
      try {
        closeable.close();
      } catch (MessagingException e) {
        e.printStackTrace();
      }
    }
  }
}
