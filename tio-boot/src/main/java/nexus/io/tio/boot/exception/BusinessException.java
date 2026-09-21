package nexus.io.tio.boot.exception;

/** An expected business failure with an HTTP error status and a client-safe message. */
public class BusinessException extends RuntimeException {
  private static final long serialVersionUID = 1L;
  private final int status;

  public BusinessException(int status, String message) {
    this(status, message, null);
  }

  public BusinessException(int status, String message, Throwable cause) {
    super(message, cause);
    if (status < 400 || status > 599) {
      throw new IllegalArgumentException("Business error status must be between 400 and 599");
    }
    this.status = status;
  }

  public int getStatus() {
    return status;
  }

  public static void require(boolean condition, String message) {
    require(condition, 400, message);
  }

  public static void require(boolean condition, int status, String message) {
    if (!condition) {
      throw new BusinessException(status, message);
    }
  }
}
