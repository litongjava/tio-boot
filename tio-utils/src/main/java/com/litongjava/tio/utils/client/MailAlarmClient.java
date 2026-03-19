package com.litongjava.tio.utils.client;

import com.litongjava.model.http.response.ResponseVo;
import com.litongjava.tio.utils.http.HttpUtils;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;

public class MailAlarmClient {
  /**
   * send alarm to mail server
   * @param url
   * @param apiKey
   * @param requestVo
   * @return
   */
  public static ResponseVo send(String url, String apiKey, AlarmRequestVo requestVo) {
    String fromUser = requestVo.getFromUser();
    String toUser = requestVo.getToUser();
    String mailBox = requestVo.getMailBox();
    String subject = requestVo.getSubject();
    String bodyString = requestVo.getBody();
    MediaType mediaType = MediaType.parse("text/plain");
    RequestBody requestBody = RequestBody.create(bodyString, mediaType);

    Request request = new Request.Builder().url(url).method("POST", requestBody)
        //
        .addHeader("Authorization", "Bearer " + apiKey)
        //
        .addHeader("mail-from-user", fromUser).addHeader("mail-to-user", toUser)
        //
        .addHeader("mail-to-mailbox", mailBox).addHeader("mail-subject", subject)
        //
        .build();
    return HttpUtils.call(request);
  }
}
