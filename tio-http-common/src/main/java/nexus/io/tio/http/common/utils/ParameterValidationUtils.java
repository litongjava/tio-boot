package nexus.io.tio.http.common.utils;

import nexus.io.model.exception.ParameterValidationException;
import nexus.io.tio.http.common.HttpRequest;
import nexus.io.tio.utils.validator.ParameterValidator;

/**
 * Binds JSON request bodies to request models using the configured JSON
 * provider.
 */
public final class ParameterValidationUtils {

  private ParameterValidationUtils() {
  }

  /**
   * Binds a required JSON object body. Field constraints remain the
   * responsibility of the caller.
   */
  public static <T> T body(HttpRequest request, Class<T> type) {
    ParameterValidator.require(request != null, "Request is required");
    ParameterValidator.require(type != null, "Request model type is required");
    try {
      return request.getBodyObject(type);
    } catch (ParameterValidationException error) {
      throw error;
    } catch (RuntimeException error) {
      throw new ParameterValidationException("Invalid request parameters");
    }
  }
}
