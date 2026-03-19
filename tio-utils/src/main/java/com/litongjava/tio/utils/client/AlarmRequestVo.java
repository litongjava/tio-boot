package com.litongjava.tio.utils.client;

public class AlarmRequestVo {
  private String fromUser;
  private String toUser;
  private String mailBox;
  private String subject;
  private String body;

  public AlarmRequestVo() {
    super();
  }

  public AlarmRequestVo(String fromUser, String toUser, String mailBox, String subject, String body) {
    super();
    this.fromUser = fromUser;
    this.toUser = toUser;
    this.mailBox = mailBox;
    this.subject = subject;
    this.body = body;
  }

  public String getFromUser() {
    return fromUser;
  }

  public void setFromUser(String fromUser) {
    this.fromUser = fromUser;
  }

  public String getToUser() {
    return toUser;
  }

  public void setToUser(String toUser) {
    this.toUser = toUser;
  }

  public String getMailBox() {
    return mailBox;
  }

  public void setMailBox(String mailBox) {
    this.mailBox = mailBox;
  }

  public String getSubject() {
    return subject;
  }

  public void setSubject(String subject) {
    this.subject = subject;
  }

  public String getBody() {
    return body;
  }

  public void setBody(String body) {
    this.body = body;
  }
}
