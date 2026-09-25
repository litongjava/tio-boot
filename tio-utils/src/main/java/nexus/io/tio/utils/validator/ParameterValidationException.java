package nexus.io.tio.utils.validator;

/** Invalid external parameter; HTTP applications normally translate this to 400. */
public class ParameterValidationException extends IllegalArgumentException {
  private static final long serialVersionUID = 1L;
  public ParameterValidationException(String message) { super(message); }
}
