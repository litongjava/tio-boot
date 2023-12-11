package com.litongjava.tio.boot.context;

import java.util.HashMap;
import java.util.Map;

import com.litongjava.tio.utils.jfinal.P;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class Enviorment {
  private String[] args;
  private Map<String, String> cmdArgsMap = new HashMap<>();

  public Enviorment(String[] args) {
    this.args = args;
    buildCmdArgsMap(args);

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
    cmdArgsMap = result;
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
      if (P.isLoad()) {
        value = P.get(key);
      }

    }

    return value;
  }

  public String get(String key) {
    return getStr(key);
  }

  public Integer getInt(String key) {
    String value = getStr(key);
    if (value != null) {
      return Integer.valueOf(value);
    } else {
      return null;
    }

  }

  public int getInt(String key, int defaultValue) {
    String value = get(key);
    if (value != null) {
      return Integer.parseInt(value);
    } else {
      return defaultValue;
    }
  }

  public String get(String key, String defaultValue) {
    String value = get(key);
    if (value != null) {
      return value;
    } else {
      return defaultValue;
    }
  }

  public boolean getBoolean(String key) {
    return Boolean.parseBoolean(get(key));
  }

  public boolean getBoolean(String key, boolean defaultValue) {
    String value = get(key);
    if (value != null) {
      return Boolean.parseBoolean(value);
    } else {
      return defaultValue;
    }
  }
}
