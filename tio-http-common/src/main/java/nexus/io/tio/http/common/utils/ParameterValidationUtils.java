package nexus.io.tio.http.common.utils;

import nexus.io.tio.http.common.HttpRequest;
import nexus.io.tio.utils.json.JsonUtils;
import nexus.io.model.exception.ParameterValidationException;
import nexus.io.tio.utils.validator.ParameterValidator;

/** Binds decoded HTTP parameters to request models using the configured JSON provider. */
public final class ParameterValidationUtils {

  private ParameterValidationUtils() {
  }

  /**
   * Binds query, form and JSON object parameters with the precedence defined by HttpRequest.
   * Field constraints remain the responsibility of the caller.
   */
  public static <T> T body(HttpRequest request, Class<T> type) {
    ParameterValidator.require(request != null, "Request is required");
    ParameterValidator.require(type != null, "Request model type is required");
    String body = request.getBodyString();
    ParameterValidator.require((body != null && !body.trim().isEmpty()) || !request.getParam().isEmpty(),
        "Request parameters are required");
    String json = JsonUtils.toJson(request.getRequestMap());
    try {
      T input = JsonUtils.parse(json, type);
      ParameterValidator.require(input != null, "Request parameters are required");
      return input;
    } catch (ParameterValidationException error) {
      throw error;
    } catch (RuntimeException error) {
      throw new ParameterValidationException("Invalid request parameters");
    }
  }
}
