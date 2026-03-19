package com.litongjava.tio.utils.jwt;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.litongjava.model.token.AuthToken;

public class JwtUtils {

  public static final String dot_delimiter = "\\.";
  public static final String colon_delimiter = ":";

  public static AuthToken createToken(String key, AuthToken authToken) {
    Map<String, Object> payloadMap = new HashMap<>();
    payloadMap.put("userId", authToken.getUserId());
    payloadMap.put("exp", authToken.getExpirationTime());
    String createToken = createToken(key, payloadMap);
    authToken.setToken(createToken);
    return authToken;
  }

  public static String createTokenByUserId(String key, Object userId) {
    // 1小时过期时间
    long tokenTimeout = (System.currentTimeMillis() + 3600000) / 1000;
    Map<String, Object> payloadMap = new HashMap<>();
    payloadMap.put("userId", userId);
    payloadMap.put("exp", tokenTimeout);
    return createToken(key, payloadMap);
  }

  public static String createTokenByUserId(String key, Object userId, long tokenTimeout) {
    Map<String, Object> payloadMap = new HashMap<>();
    payloadMap.put("userId", userId);
    payloadMap.put("exp", tokenTimeout); // 1小时过期时间
    return createToken(key, payloadMap);
  }

  /**
   * 创建JWT Token
   * 
   * @param key        密钥
   * @param payloadMap 载荷
   * @return 生成的JWT Token
   */
  public static String createToken(String key, Map<String, Object> payloadMap) {
    // 1. 创建header
    String header = Base64.getUrlEncoder().encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));

    String payload = Base64.getUrlEncoder().encodeToString(toJson(payloadMap).getBytes(StandardCharsets.UTF_8));

    // 3. 创建signature
    String signature = hmacSha256(header + "." + payload, key);

    // 4. 组合token
    return header + "." + payload + "." + signature;
  }

  private static String toJson(Map<String, Object> payloadMap) {
    StringBuilder json = new StringBuilder();
    json.append("{");
    int size = payloadMap.size();
    int i = 0;

    for (Map.Entry<String, Object> entry : payloadMap.entrySet()) {
      json.append("\"").append(entry.getKey()).append("\":");

      Object value = entry.getValue();
      if (value instanceof String) {
        json.append("\"").append(value).append("\"");
      } else {
        json.append(value);
      }

      if (i < size - 1) {
        json.append(",");
      }
      i++;
    }

    json.append("}");
    return json.toString();
  }

  public static boolean verify(String key, String token, String delimiter) {
    String[] parts = token.split(delimiter);
    if (parts.length != 3) {
      return false;
    }

    String header = parts[0];
    String payload = parts[1];
    String signature = parts[2];

    String calculatedSignature = null;
    // 重新计算签名并与传入的签名进行比较
    if (colon_delimiter.equals(delimiter)) {
      calculatedSignature = hmacSha256(header + ":" + payload, key);
    } else {
      calculatedSignature = hmacSha256(header + "." + payload, key);
    }

    // 先解码payload
    String decodedPayload = new String(Base64.getUrlDecoder().decode(payload), StandardCharsets.UTF_8);

    return signature.equals(calculatedSignature) && !isTokenExpired(decodedPayload);
  }

  /**
   * 验证JWT Token
   * 
   * @param key   密钥
   * @param token JWT Token
   * @return 如果Token有效则返回true，否则返回false
   */
  public static boolean verify(String key, String token) {
    return verify(key, token, dot_delimiter);
  }

  public static Map<String, Object> getPayload(String token, String delimiter) {
    String[] parts = token.split(delimiter);
    if (parts.length != 3) {
      throw new IllegalArgumentException("Invalid JWT token");
    }

    String payload = parts[1];
    String decodedPayload = new String(Base64.getUrlDecoder().decode(payload), StandardCharsets.UTF_8);

    // 解析为Map
    return parsePayload(decodedPayload);
  }

  /**
   * 获取JWT的payload中的数据
   * 
   * @param token JWT Token
   * @return payload中的数据
   */
  public static Map<String, Object> getPayload(String token) {
    return getPayload(token, dot_delimiter);
  }

  /**
   * 使用HMAC SHA256生成签名
   * 
   * @param data   要签名的数据
   * @param secret 密钥
   * @return 生成的签名
   */
  private static String hmacSha256(String data, String secret) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
      mac.init(secretKeySpec);
      byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
      return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    } catch (Exception e) {
      throw new RuntimeException("Failed to calculate HMAC SHA256", e);
    }
  }

  /**
   * 检查JWT的payload部分是否过期
   * 
   * @param payload JWT的payload部分
   * @return 如果Token已过期返回true，否则返回false
   */
  private static boolean isTokenExpired(String payload) {
    Map<String, Object> payloadMap = parsePayload(payload);
    long exp = (long) payloadMap.get("exp");
    if (exp == -1) {
      return false;
    }
    // 检查是否过期
    return exp < (System.currentTimeMillis() / 1000);
  }

  /**
   * 将payload字符串解析为Map
   * 
   * @param payload 载荷字符串
   * @return 解析后的Map
   */
  public static Map<String, Object> parsePayload(String payload) {
    Map<String, Object> payloadMap = new HashMap<>();

    payload = payload.trim();
    if (payload.startsWith("{") && payload.endsWith("}")) {
      payload = payload.substring(1, payload.length() - 1);
    }

    List<String> pairs = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    boolean inQuotes = false;
    int bracketLevel = 0;

    for (char c : payload.toCharArray()) {
      if (c == '"')
        inQuotes = !inQuotes;
      else if (!inQuotes) {
        if (c == '[')
          bracketLevel++;
        else if (c == ']')
          bracketLevel--;
        else if (c == ',' && bracketLevel == 0) {
          pairs.add(current.toString());
          current.setLength(0);
          continue;
        }
      }
      current.append(c);
    }
    if (current.length() > 0)
      pairs.add(current.toString());

    for (String pair : pairs) {
      String[] keyValue = pair.split(":", 2);
      if (keyValue.length < 2)
        continue;

      String key = keyValue[0].trim().replaceAll("\"", "");
      String rawValue = keyValue[1].trim();

      Object value;
      if (rawValue.startsWith("[") && rawValue.endsWith("]")) {
        // 解析数组
        rawValue = rawValue.substring(1, rawValue.length() - 1);
        String[] parts = rawValue.split(",");
        List<Object> list = new ArrayList<>();
        for (String p : parts) {
          String v = p.trim().replaceAll("\"", "");
          try {
            list.add(Long.parseLong(v));
          } catch (NumberFormatException e) {
            list.add(v);
          }
        }
        value = list;
      } else {
        try {
          value = Long.parseLong(rawValue);
        } catch (NumberFormatException e) {
          value = rawValue.replaceAll("\"", "");
        }
      }

      payloadMap.put(key, value);
    }

    return payloadMap;
  }

  public static AuthToken getAuthToken(String token) {
    String[] parts = token.split("\\.");
    if (parts.length != 3) {
      throw new IllegalArgumentException("Invalid JWT token");
    }

    String payload = parts[1];
    String decodedPayload = new String(Base64.getUrlDecoder().decode(payload), StandardCharsets.UTF_8);

    // 解析为authToken
    AuthToken authToken = parseToAuthToken(decodedPayload);
    authToken.setToken(token);
    return authToken;
  }

  private static AuthToken parseToAuthToken(String payload) {
    AuthToken authToken = new AuthToken();

    // 移除大括号
    payload = payload.substring(1, payload.length() - 1);

    // 解析值
    String[] pairs = payload.split(",");
    for (String pair : pairs) {
      String[] keyValue = pair.split(":");
      String key = keyValue[0].trim().replaceAll("\"", "");
      Object value = keyValue[1].trim();
      if ("userId".equals(key)) {
        authToken.setUserId(value);
      } else if ("exp".equals(key)) {
        Long exp = Long.parseLong((String) value);
        authToken.setExpirationTime(exp);
      }
    }
    return authToken;
  }

  public static Long parseUserIdLong(String token) {
    Map<String, Object> payload = JwtUtils.getPayload(token);
    return (Long) payload.get("userId");
  }

  public static String parseUserIdString(String token) {
    Map<String, Object> payload = JwtUtils.getPayload(token);
    return (String) payload.get("userId");
  }

  public static Integer parseUserIdInt(String token) {
    Map<String, Object> payload = JwtUtils.getPayload(token);
    return (Integer) payload.get("userId");
  }

  public static Object parseUserId(String token) {
    Map<String, Object> payload = JwtUtils.getPayload(token);
    return payload.get("userId");
  }
}
