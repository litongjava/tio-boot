package com.litongjava.tio.http.multipart;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TioMultipartParts {
  private final List<TioMultipartPart> parts = new ArrayList<>();

  public List<TioMultipartPart> getParts() {
    return Collections.unmodifiableList(parts);
  }

  public TioMultipartParts addText(String name, String value) {
    return addText(name, value, "text/plain; charset=utf-8");
  }

  public TioMultipartParts addText(String name, String value, String contentType) {
    if (name == null || name.isEmpty()) {
      return this;
    }
    if (value == null) {
      return this;
    }
    parts.add(TioMultipartPart.text(name, value, contentType));
    return this;
  }

  public TioMultipartParts addObject(String name, Object value) {
    return addObject(name, value, "text/plain; charset=utf-8");
  }

  public TioMultipartParts addJson(String name, String value) {
    return addText(name, value, "application/json; charset=utf-8");
  }

  public TioMultipartParts addObject(String name, Object value, String contentType) {
    if (value == null) {
      return this;
    }
    return addText(name, String.valueOf(value), contentType);
  }

  public boolean isEmpty() {
    return parts.isEmpty();
  }

  public static TioMultipartParts create() {
    return new TioMultipartParts();
  }
}
