package com.litongjava.tio.utils.thymeleaf;

import java.util.concurrent.ConcurrentHashMap;

import org.thymeleaf.context.Context;

public class Thymyleaf {

  private static final ConcurrentHashMap<String, ThymyleafEngine> cacheMap = new ConcurrentHashMap<>();
  private static ThymyleafEngine main = null;

  // Private constructor to prevent instantiation
  private Thymyleaf() {
  }

  public static void clear() {
    cacheMap.clear();
  }

  public static void add(ThymyleafEngine single) {
    if (single == null) {
      throw new IllegalArgumentException("Bot can not be null");
    }

    if (cacheMap.containsKey(single.getName())) {
      throw new IllegalArgumentException("The bot name already exists");
    }

    cacheMap.put(single.getName(), single);
    if (main == null) {
      main = single;
    }
  }

  public static ThymyleafEngine use() {
    return main;
  }

  public static ThymyleafEngine use(String botName) {
    return cacheMap.get(botName);
  }

  public static String process(String template, Context context) {
    return main.process(template, context);
  }
}
