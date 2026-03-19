package com.litongjava.tio.utils.email;

import java.util.Properties;

import com.litongjava.tio.utils.environment.EnvUtils;

public class GMail implements EMail {

  /**
   * 发送邮件
   * @param to 收件人
   * @param subject 主题
   * @param content 内容
   * @paaram isDebug 是否开启debug模式
   */
  public void send(String to, String subject, String content, boolean isDebug) {
    String smptHost = EnvUtils.get("mail.host");
    String mailTransportProtocol = EnvUtils.get("mail.protocol");

    Integer smtpPort = EnvUtils.getInt("mail.smpt.port");
    String user = EnvUtils.get("mail.user");
    String password = EnvUtils.get("mail.password");
    String from = EnvUtils.get("mail.from");

    Properties prop = new Properties();
    // 邮件服务器
    prop.setProperty("mail.host", smptHost);
    // 传输协议
    prop.setProperty("mail.transport.protocol", mailTransportProtocol);
    // 开启验证
    prop.setProperty("mail.smtp.auth", "true");
    // 设置端口
    prop.setProperty("mail.smtp.port", smtpPort + "");
    prop.setProperty("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
    prop.setProperty("mail.smtp.socketFactory.fallback", "false");
    prop.setProperty("mail.smtp.socketFactory.port", smtpPort + "");

    EMailSendUtils.send(to, subject, content, isDebug, smptHost, smtpPort, user, password, from, prop);
  }

}
