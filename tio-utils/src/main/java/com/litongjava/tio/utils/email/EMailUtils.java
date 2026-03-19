package com.litongjava.tio.utils.email;

public class EMailUtils {
  private static IEMailFactory defaultJsonFactory = new Wangyi126MailFactory();

  public static void setEMailFactory(IEMailFactory factory) {
    defaultJsonFactory = factory;
  }
  
  public static IEMailFactory getEMailFactory() {
    return defaultJsonFactory;
  }

  /**
   * send mail
   * @param to
   * @param subject
   * @param content
   * @param isDebug
   */
  public static void send(String to, String subject, String content, boolean isDebug) {
    defaultJsonFactory.getMail().send(to, subject, content, isDebug);
  }

  /**
   * send mail
   * @param to
   * @param subject
   * @param content
   */
  public static void send(String to, String subject, String content) {
    defaultJsonFactory.getMail().send(to, subject, content, false);

  }

}
