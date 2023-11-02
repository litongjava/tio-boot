package com.litongjava.tio.boot.context;

import java.util.HashMap;
import java.util.Map;

import org.tio.utils.jfinal.P;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class Enviorment {
  private String[] args;
  private Map<String, String> cmdArgsMap;

  public Enviorment(String[] args) {
    this.args = args;
    this.cmdArgsMap = buildCmdArgsMap(args);

  }

  public Map<String, String> buildCmdArgsMap(String[] args) {
    Map<String, String> result = new HashMap<>();
    for (String arg : args) {
      if (arg.startsWith("--")) {
        String[] parts = arg.substring(2).split("=", 2);
        if (parts.length == 2) {
          result.put(parts[0], parts[1]);
        }
      }
    }
    return result;
  }

  public String getStr(String key) {
    // comamdn line
    String value = cmdArgsMap.get(key);
    // enviorment
    if (value == null) {
      value = System.getProperty(key);
    }

    // config file
    if (value == null) {
      value = P.get(key);
    }

    return value;
  }

  public String get(String key) {
    return getStr(key);
  }

  public Integer getInt(String key) {
    return Integer.valueOf(getStr(key));
  }
}
