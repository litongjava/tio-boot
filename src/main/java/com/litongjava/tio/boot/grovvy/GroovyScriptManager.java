package com.litongjava.tio.boot.grovvy;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import com.litongjava.jfinal.aop.Aop;
import com.litongjava.tio.http.common.HttpResponse;
import com.litongjava.tio.http.server.handler.IHttpRequestHandler;
import com.litongjava.tio.utils.hutool.ResourceUtil;

import groovy.lang.GroovyShell;

public class GroovyScriptManager {

  @SuppressWarnings("unchecked")
  public static <T> T executeScript(String script) {
    GroovyShell shell = Aop.get(GroovyShell.class);
    return (T) shell.evaluate(script);
  }

  /**
   * Execute a script located in the classpath.
   */
  public static <T> T executeClasspathScript(String filename) {
    try (InputStream inputStream = ResourceUtil.getResourceAsStream(filename)) {
      if (inputStream != null) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
          String script = reader.lines().collect(Collectors.joining("\n"));
          return executeScript(script);
        }
      } else {
        throw new IllegalArgumentException("Script file not found: " + filename);
      }
    } catch (Exception e) {
      throw new RuntimeException("Error executing Groovy script", e);
    }
  }
  
  public static IHttpRequestHandler getHttpRequestHandler(String scriptValue) {
    return (request) -> {
      return (HttpResponse) GroovyScriptManager.executeScript(scriptValue);
    };
  }
}