package nexus.io.tio.http.common.utils;

import java.util.Locale;
import java.util.Map;

import nexus.io.tio.http.common.HttpRequest;
import nexus.io.tio.utils.json.Json;
import nexus.io.tio.utils.json.JsonUtils;
import nexus.io.model.exception.ParameterValidationException;
import nexus.io.tio.utils.validator.ParameterValidator;

/** Binds decoded HTTP parameters to request models using the configured JSON provider. */
public final class ParameterValidationUtils {

  private ParameterValidationUtils() {
  }

  /**
   * Binds JSON object bodies directly, or decoded form/query parameters.
   * JSON bodies do not include query parameters.
   * Field constraints remain the responsibility of the caller.
   */
  public static <T> T body(HttpRequest request, Class<T> type) {
    ParameterValidator.require(request != null, "Request is required");
    ParameterValidator.require(type != null, "Request model type is required");
    String body = request.getBodyString();
    ParameterValidator.require((body != null && !body.trim().isEmpty()) || !request.getParam().isEmpty(),
        "Request parameters are required");
    try {
      String contentType = request.getContentType();
      String mediaType = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
      boolean form = mediaType.startsWith("application/x-www-form-urlencoded")
          || mediaType.startsWith("multipart/form-data");
      T input;
      if (!form && body != null && !body.trim().isEmpty()) {
        ParameterValidator.require(Json.getJson().parse(body) instanceof Map, "JSON object required");
        input = request.getBodyObject(type);
      } else {
        input = JsonUtils.parse(JsonUtils.toJson(request.getRequestMap()), type);
      }
      ParameterValidator.require(input != null, "Request parameters are required");
      return input;
    } catch (ParameterValidationException error) {
      throw error;
    } catch (RuntimeException error) {
      throw new ParameterValidationException("Invalid request parameters");
    }
  }
}
