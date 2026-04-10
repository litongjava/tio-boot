package com.nexus.tio.utils.telegram;

import org.junit.Test;

import com.nexus.model.http.response.ResponseVo;
import com.nexus.tio.utils.json.JsonUtils;

public class TelegramBotTest {

  @Test
  public void test() {
    String token = "7847170133:AAGfkBGGTScN_xc_Sj57WhlVUA5U6BhSb-0";
    String webHook = "https://gpt-translator-backend-api.fly.dev/telegram/webhook";
    TelegramBot telegramBot = new TelegramBot("main", token);
    ResponseVo setWebhook = telegramBot.setWebhook(webHook);
    System.out.println(JsonUtils.toJson(setWebhook));
    ResponseVo webhookInfo = telegramBot.getWebhookInfo();
    System.out.println(JsonUtils.toJson(webhookInfo));
    //ResponseVo deleteWebhook = telegramBot.deleteWebhook();
    //System.out.println(JsonUtils.toJson(deleteWebhook));
  }

}
