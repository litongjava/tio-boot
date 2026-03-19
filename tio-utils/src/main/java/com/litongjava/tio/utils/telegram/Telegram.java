package com.litongjava.tio.utils.telegram;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class Telegram {

  private static final ConcurrentHashMap<String, TelegramBot> botMap = new ConcurrentHashMap<>();
  private static TelegramBot mainBot = null;

  // Private constructor to prevent instantiation
  private Telegram() {
  }

  public static void clearBot() {
    botMap.clear();
  }

  public static void addBot(TelegramBot bot) {
    if (bot == null) {
      throw new IllegalArgumentException("Bot can not be null");
    }

    if (botMap.containsKey(bot.getName())) {
      throw new IllegalArgumentException("The bot name already exists");
    }

    botMap.put(bot.getName(), bot);
    if (mainBot == null)
      mainBot = bot;
  }

  public static TelegramBot use() {
    return mainBot;
  }

  public static TelegramBot use(String botName) {
    return botMap.get(botName);
  }

  public static void setMainBot(String botName) {
    if (!botMap.containsKey(botName))
      throw new IllegalArgumentException("The bot does not exist: " + botName);

    mainBot = botMap.get(botName);
  }

  public static void config(Consumer<TelegramBot> botConfig) {
    if (mainBot == null)
      throw new IllegalStateException("Main bot is not set");
    botConfig.accept(mainBot);
  }

  public static void config(String botName, Consumer<TelegramBot> botConfig) {
    TelegramBot bot = botMap.get(botName);
    if (bot == null)
      throw new IllegalArgumentException("The bot does not exist: " + botName);
    botConfig.accept(bot);
  }

}
