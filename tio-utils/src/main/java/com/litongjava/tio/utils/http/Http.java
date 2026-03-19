package com.litongjava.tio.utils.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

import com.litongjava.model.http.response.ResponseVo;
import com.litongjava.tio.utils.json.JsonUtils;

public class Http {

  /**
   * 发送带有 JSON 负载的 POST 请求
   * 
   * @param url    请求的 URL
   * @param params 请求参数
   * @return ResponseVo 响应对象
   */
  public static ResponseVo postJson(String url, Map<String, String> params) {
    String payload = JsonUtils.toJson(params);
    return postJson(url, payload, null);
  }

  /**
   * 发送带有 JSON 负载的 POST 请求
   * 
   * @param serverUrl 请求的 URL
   * @param payload   JSON 字符串负载
   * @return ResponseVo 响应对象
   */
  public static ResponseVo postJson(String serverUrl, String payload) {
    return postJson(serverUrl, payload, null);
  }

  /**
   * 发送带有 JSON 负载和自定义头部的 POST 请求
   * 
   * @param serverUrl 请求的 URL
   * @param payload   JSON 字符串负载
   * @param headers   自定义头部
   * @return ResponseVo 响应对象
   */
  public static ResponseVo postJson(String serverUrl, String payload, Map<String, String> headers) {
    URL url = null;
    try {
      url = new URL(serverUrl);
    } catch (MalformedURLException e) {
      throw new RuntimeException("Invalid URL: " + serverUrl, e);
    }

    HttpURLConnection conn = null;
    try {
      conn = (HttpURLConnection) url.openConnection();
      conn.setDoOutput(true);
      conn.setRequestMethod("POST");
      conn.setRequestProperty("Content-Type", "application/json");

      // 设置自定义头部
      if (headers != null) {
        for (Map.Entry<String, String> entry : headers.entrySet()) {
          conn.setRequestProperty(entry.getKey(), entry.getValue());
        }
      }

      // 写入请求负载
      try (OutputStream os = conn.getOutputStream()) {
        byte[] input = payload.getBytes(StandardCharsets.UTF_8);
        os.write(input, 0, input.length);
      }

      // 处理响应
      return handleResponse(conn);

    } catch (IOException e) {
      throw new RuntimeException("Failed to send POST JSON request to " + serverUrl, e);
    } finally {
      if (conn != null) {
        conn.disconnect();
      }
    }
  }

  /**
   * 发送带有表单参数的 POST 请求
   * 
   * @param serverUrl 请求的 URL
   * @param params    表单参数
   * @return ResponseVo 响应对象
   */
  public static ResponseVo post(String serverUrl, Map<String, String> params) {
    return post(serverUrl, params, null);
  }

  /**
   * 发送带有表单参数和自定义头部的 POST 请求
   * 
   * @param serverUrl 请求的 URL
   * @param params    表单参数
   * @param headers   自定义头部
   * @return ResponseVo 响应对象
   */
  public static ResponseVo post(String serverUrl, Map<String, String> params, Map<String, String> headers) {
    URL url;
    try {
      url = new URL(serverUrl);
    } catch (MalformedURLException e) {
      throw new RuntimeException("Invalid URL: " + serverUrl, e);
    }

    HttpURLConnection conn = null;
    try {
      conn = (HttpURLConnection) url.openConnection();
      conn.setDoOutput(true);
      conn.setRequestMethod("POST");
      conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

      // 设置自定义头部
      if (headers != null) {
        for (Map.Entry<String, String> entry : headers.entrySet()) {
          conn.setRequestProperty(entry.getKey(), entry.getValue());
        }
      }

      // 构建表单参数字符串
      String formParams = params.entrySet().stream().map(entry -> encodeURIComponent(entry.getKey()) + "=" + encodeURIComponent(entry.getValue())).collect(Collectors.joining("&"));

      // 写入表单参数
      try (OutputStream os = conn.getOutputStream()) {
        byte[] input = formParams.getBytes(StandardCharsets.UTF_8);
        os.write(input, 0, input.length);
      }

      // 处理响应
      return handleResponse(conn);

    } catch (IOException e) {
      throw new RuntimeException("Failed to send POST request to " + serverUrl, e);
    } finally {
      if (conn != null) {
        conn.disconnect();
      }
    }
  }

  /**
   * 发送 GET 请求
   * 
   * @param serverUrl 请求的 URL
   * @return ResponseVo 响应对象
   */
  public static ResponseVo get(String serverUrl) {
    return get(serverUrl, null);
  }

  /**
   * 发送带有自定义头部的 GET 请求
   * 
   * @param serverUrl 请求的 URL
   * @param headers   自定义头部
   * @return ResponseVo 响应对象
   */
  public static ResponseVo get(String serverUrl, Map<String, String> headers) {
    URL url;
    try {
      url = new URL(serverUrl);
    } catch (MalformedURLException e) {
      throw new RuntimeException("Invalid URL: " + serverUrl, e);
    }

    HttpURLConnection conn = null;
    try {
      conn = (HttpURLConnection) url.openConnection();
      conn.setRequestMethod("GET");

      // 设置自定义头部
      if (headers != null) {
        for (Map.Entry<String, String> entry : headers.entrySet()) {
          conn.setRequestProperty(entry.getKey(), entry.getValue());
        }
      }

      // 处理响应
      return handleResponse(conn);

    } catch (IOException e) {
      throw new RuntimeException("Failed to send GET request to " + serverUrl, e);
    } finally {
      if (conn != null) {
        conn.disconnect();
      }
    }
  }

  /**
   * 统一处理 HTTP 响应
   * 
   * @param conn HTTP 连接对象
   * @return ResponseVo 响应对象
   * @throws IOException 如果读取响应失败
   */
  private static ResponseVo handleResponse(HttpURLConnection conn) throws IOException {
    int responseCode = conn.getResponseCode();
    InputStream inputStream = null;

    if (responseCode >= HttpURLConnection.HTTP_OK && responseCode < HttpURLConnection.HTTP_MULT_CHOICE) {
      inputStream = conn.getInputStream();
    } else {
      inputStream = conn.getErrorStream();
      if (inputStream == null) { // 有时候 errorStream 可能为空
        return ResponseVo.fail(responseCode, "No error message available");
      }
    }

    String responseBody = new String(readInputStream(inputStream), StandardCharsets.UTF_8);

    return responseCode == HttpURLConnection.HTTP_OK ? ResponseVo.ok(responseCode, responseBody) : ResponseVo.fail(responseCode, responseBody);
  }

  /**
   * 读取输入流的所有字节
   * 
   * @param inputStream 输入流
   * @return 字节数组
   * @throws IOException 如果读取失败
   */
  private static byte[] readInputStream(InputStream inputStream) throws IOException {
    try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
      int nRead;
      byte[] data = new byte[1024];
      while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
        buffer.write(data, 0, nRead);
      }
      return buffer.toByteArray();
    }
  }

  /**
   * 对 URL 参数进行编码，避免参数中包含特殊字符导致的问题
   * 
   * @param s 要编码的字符串
   * @return 编码后的字符串
   */
  private static String encodeURIComponent(String s) {
    try {
      return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8.toString());
    } catch (Exception e) {
      // 理论上不会发生，因为 UTF-8 是有效的编码
      throw new RuntimeException("Failed to encode parameter: " + s, e);
    }
  }
}