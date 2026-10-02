package nexus.io.tio.utils.telegram;

import org.junit.Test;

import nexus.io.model.http.response.ResponseVo;
import nexus.io.tio.utils.json.JsonUtils;

public class TelegramBotTest {

  @Test
  public void test() {
    String token = "xxx";
    String webHook = "https://example.com/telegram/webhook";
    TelegramBot telegramBot = new TelegramBot("main", token);
    ResponseVo setWebhook = telegramBot.setWebhook(webHook);
    System.out.println(JsonUtils.toJson(setWebhook));
    ResponseVo webhookInfo = telegramBot.getWebhookInfo();
    System.out.println(JsonUtils.toJson(webhookInfo));
    // ResponseVo deleteWebhook = telegramBot.deleteWebhook();
    // System.out.println(JsonUtils.toJson(deleteWebhook));
  }

}
