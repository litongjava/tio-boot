package com.litongjava.tio.utils.email;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class EMailSendUtils {

  public static void send(String to, String subject, String content, boolean isDebug, String mailHost, Integer smtpPort,
      String user, String password, String from, Properties prop) {
    // 获取session
    Session session = Session.getInstance(prop);
    // 开启debug模式时
    session.setDebug(isDebug);
    // 获取 tranport
    Transport ts = null;
    MimeMessage message = new MimeMessage(session);
    try {
      ts = session.getTransport();
      ts.connect(mailHost, smtpPort, user, password);

      // 设置消息
      message.setFrom(new InternetAddress(from));
      message.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
      message.setSubject(subject);
      message.setText(content);

      // 发送消息
      ts.sendMessage(message, message.getAllRecipients());
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      MailIOUtils.closeQuietly(ts);
    }
  }

}
