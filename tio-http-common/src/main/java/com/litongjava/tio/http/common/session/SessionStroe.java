package com.litongjava.tio.http.common.session;

import java.util.concurrent.ConcurrentHashMap;

public class SessionStroe {
  // 简单内存会话表
  public static final ConcurrentHashMap<String, String> sessionStrMap = new ConcurrentHashMap<>();
  public static final ConcurrentHashMap<String, Object> sessionObjMap = new ConcurrentHashMap<>();

  public static String putStr(String sessionId, String u) {
    return sessionStrMap.put(sessionId, u);
  }

  public static String getString(String id) {
    return sessionStrMap.get(id);
  }

  public static String putString(String id, String v) {
    return sessionStrMap.put(id, v);
  }

  public static Object getObject(String sessionId) {
    return sessionObjMap.get(sessionId);
  }

  public static Object putObject(String sessionId, Object o) {
    return sessionObjMap.put(sessionId, o);
  }

  public static boolean containsKey(String sessionId) {
    return sessionStrMap.containsKey(sessionId) || sessionObjMap.containsKey(sessionId);
  }
}
