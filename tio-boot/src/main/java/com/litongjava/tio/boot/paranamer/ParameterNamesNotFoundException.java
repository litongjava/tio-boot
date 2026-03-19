package com.litongjava.tio.boot.paranamer;

/**
 * Exception thrown when no parameter names are found
 * 
 * @author Paul Hammant
 * @author Mauro Talevi
 */
@SuppressWarnings("serial")
public class ParameterNamesNotFoundException extends RuntimeException {

  public static final String __PARANAMER_DATA = "v1.0 \n" + "<init> java.lang.String message \n";
  private Exception cause;

  public ParameterNamesNotFoundException(String message, Exception cause) {
    super(message);
    this.cause = cause;
  }

  public ParameterNamesNotFoundException(String message) {
    super(message);
  }

  public Throwable getCause() {
    return cause;
  }
}
