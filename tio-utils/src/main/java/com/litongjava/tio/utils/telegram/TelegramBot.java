package com.litongjava.tio.utils.telegram;

import java.util.HashMap;
import java.util.Map;

import com.litongjava.model.http.response.ResponseVo;
import com.litongjava.tio.utils.http.Http;
import com.litongjava.tio.utils.json.JsonUtils;

public class TelegramBot {

  public static final String SERVER_URL = "https://api.telegram.org";

  private String botToken;
  private String name;

  public TelegramBot(String botToken) {
    this.name = "main";
    this.botToken = botToken;
  }

  public TelegramBot(String name, String botToken) {
    this.name = name;
    this.botToken = botToken;
  }

  public String getName() {
    return name;
  }

  public TelegramBot withToken(String botToken) {
    this.botToken = botToken;
    return this;
  }

  public ResponseVo sendMessage(String chatId, String message) {
    String urlString = SERVER_URL + "/bot" + botToken + "/sendMessage";
    Map<String, String> map = new HashMap<>();
    map.put("chat_id", chatId);
    map.put("text", message);
    String payload = JsonUtils.toJson(map);
    return Http.postJson(urlString, payload);
  }

  public ResponseVo setWebhook(String url) {
    String urlString = SERVER_URL + "/bot" + botToken + "/setWebhook" + "?url=" + url;
    return Http.get(urlString);
  }

  public ResponseVo getWebhookInfo() {
    String urlString = SERVER_URL + "/bot" + botToken + "/getWebhookInfo";
    return Http.get(urlString);
  }

  public ResponseVo deleteWebhook() {
    String urlString = SERVER_URL + "/bot" + botToken + "/deleteWebhook";
    return Http.get(urlString);
  }
  
  public ResponseVo getUpdates() {
    String urlString = SERVER_URL + "/bot" + botToken + "/getUpdates";
    return Http.get(urlString);
  }
}