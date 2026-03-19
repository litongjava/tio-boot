package com.litongjava.tio.boot.i18n;

import java.text.MessageFormat;
import java.util.ResourceBundle;
import com.jfinal.kit.StrKit;

/**
 * Res is used to get the message value from the ResourceBundle of the related Locale.
 */
public class Res {

  private final ResourceBundle resourceBundle;

  public Res(String baseName, String locale) {
    if (StrKit.isBlank(baseName)) {
      throw new IllegalArgumentException("baseName can not be blank");
    }
    if (StrKit.isBlank(locale)) {
      throw new IllegalArgumentException("locale can not be blank, the format like this: zh_CN or en_US");
    }

    this.resourceBundle = ResourceBundle.getBundle(baseName, I18n.toLocale(locale));
  }

  /**
   * Get the message value from ResourceBundle of the related Locale.
   * @param key message key
   * @return message value
   */
  public String get(String key) {
    return resourceBundle.getString(key);
  }

  /**
   * Get the message value from ResourceBundle by the key then format with the arguments.
   * Example:<br>
   * In resource file : msg=Hello {0}, today is{1}.<br>
   * In java code : res.format("msg", "james", new Date()); <br>
   * In freemarker template : ${_res.format("msg", "james", new Date())}<br>
   * The result is : Hello james, today is 2015-04-14.
   */
  public String format(String key, Object... arguments) {
    return MessageFormat.format(resourceBundle.getString(key), arguments);
  }

  public ResourceBundle getResourceBundle() {
    return resourceBundle;
  }
}
