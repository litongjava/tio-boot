package com.litongjava.tio.utils.telegram;

import org.junit.Test;

import com.litongjava.model.http.response.ResponseVo;
import com.litongjava.tio.utils.json.JsonUtils;

public class TelegramTest {

  String BOT_TOKEN = "xxx";
  String CHAT_ID = "xxx";

  @Test
  public void testSend() {
    // Create a Telegram bot instance
    TelegramBot bot = new TelegramBot("mainBot", BOT_TOKEN);
    // Add the bot to the Telegram management class
    Telegram.addBot(bot);

    // Send a message using the main bot
    ResponseVo responseVo = Telegram.use().sendMessage(CHAT_ID, "Hello, Telegram Group!");
    System.out.println(JsonUtils.toJson(responseVo));
  }

  @Test
  public void testFull() {

    // Create a Telegram bot instance
    TelegramBot bot = new TelegramBot("mainBot", BOT_TOKEN);
    // Add the bot to the Telegram management class
    Telegram.addBot(bot);

    // Set it as the main bot (optional if only one bot is used)
    Telegram.setMainBot("mainBot");

    // Send a message using the main bot
    Telegram.use().sendMessage(CHAT_ID, "Hello, Telegram Group!");

    // Alternatively, you can configure and send messages like this:
    Telegram.config(botConfig -> botConfig.withToken("BOT_TOKEN"));

    Telegram.use().sendMessage(CHAT_ID, "Hello from another bot");

  }

}
