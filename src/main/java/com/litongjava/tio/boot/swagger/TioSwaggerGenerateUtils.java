package com.litongjava.tio.boot.swagger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.litongjava.constants.ServerConfigKeys;
import com.litongjava.tio.boot.http.handler.controller.TioBootHttpControllerRouter;
import com.litongjava.tio.utils.environment.EnvUtils;
import com.litongjava.tio.utils.hutool.StrUtil;
import com.litongjava.tio.utils.json.JsonUtils;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import springfox.documentation.service.ApiInfo;

public class TioSwaggerGenerateUtils {
  private static final Logger log = LoggerFactory.getLogger(TioSwaggerGenerateUtils.class);
  
  // 静态集合，用于存储需要生成定义的类
  private static Set<Class<?>> definitionsToGenerate = new HashSet<>();

  /**
   * 生成 Swagger v2 JSON
   *
   * @param router
   * @param apiInfo
   * @return
   */
  public static String generateSwaggerJson(TioBootHttpControllerRouter router, ApiInfo apiInfo) {
    Map<String, Object> swagger = new LinkedHashMap<>();
    swagger.put("swagger", "2.0");

    // 基本信息
    swagger.put("info", apiInfo);

    // 主机信息，可以根据需要动态获取
    int port = EnvUtils.getInt(ServerConfigKeys.SERVER_PORT, 80);
    String contextPath = EnvUtils.get(ServerConfigKeys.SERVER_CONTEXT_PATH);
    if (contextPath == null) {
      contextPath = "/";
    }
    swagger.put("host", "127.0.0.1:" + port);
    swagger.put("basePath", contextPath);
    swagger.put("schemes", Arrays.asList("http"));

    // Paths
    Map<String, Object> paths = new LinkedHashMap<>();
    Set<Class<?>> responseClasses = new HashSet<>(); // 用于收集所有响应类

    for (Map.Entry<String, Method> entry : router.PATH_METHOD_MAP.entrySet()) {
      String key = entry.getKey(); // e.g., "POST /app/domain/selectLrbAppDomainById"

      String httpMethod;
      String path;
      if (key.contains(" ")) {
        String[] parts = key.split(" ", 2);
        httpMethod = parts[0].toLowerCase();
        path = parts[1];
      } else {
        httpMethod = "post";
        path = key;
      }

      // 转换路径变量为 Swagger 格式，如 /app/domain/{AppDomainId}
      String swaggerPath = path.replaceAll("\\{", "{").replaceAll("}", "}");

      // 获取或创建 pathItem
      @SuppressWarnings("unchecked")
      Map<String, Object> pathItem = (Map<String, Object>) paths.get(swaggerPath);
      if (pathItem == null) {
        pathItem = new LinkedHashMap<>();
        paths.put(swaggerPath, pathItem);
      }

      // 构建 operation
      Map<String, Object> operation = new LinkedHashMap<>();

      // 获取 Controller 类上的 @Api 注解，如果存在则取 tags，否则使用类名
      Method method = entry.getValue();
      Class<?> controllerClass = method.getDeclaringClass();
      Api apiAnnotation = controllerClass.getAnnotation(Api.class);
      if (apiAnnotation != null && apiAnnotation.tags().length > 0) {
        operation.put("tags", Arrays.asList(apiAnnotation.tags()));
      } else {
        operation.put("tags", Collections.singletonList(controllerClass.getSimpleName()));
      }

      // 方法级别的 @ApiOperation 注解
      ApiOperation apiOperation = method.getAnnotation(ApiOperation.class);
      if (apiOperation != null) {
        operation.put("summary", apiOperation.value());
        operation.put("description", apiOperation.notes());
      } else {
        // 默认值
        operation.put("summary", path);
        operation.put("description", "Request method " + httpMethod.toUpperCase() + ", request type application/x-www-form-urlencoded or application/json");
      }
      operation.put("operationId", method.getName() + "Using" + httpMethod.toUpperCase());

      // 参数处理
      ApiImplicitParams apiImplicitParams = method.getAnnotation(ApiImplicitParams.class);
      List<Map<String, Object>> parameters = new ArrayList<>();
      boolean hasNonHeaderParam = false;

      if (apiImplicitParams != null) {
        for (ApiImplicitParam param : apiImplicitParams.value()) {
          if (!"header".equalsIgnoreCase(param.paramType())) {
            hasNonHeaderParam = true;
          }
          Map<String, Object> paramMap = new LinkedHashMap<>();
          paramMap.put("name", param.name());
          paramMap.put("in", param.paramType());
          paramMap.put("description", param.value());
          paramMap.put("required", param.required());
          Map<String, Object> schema = new LinkedHashMap<>();
          if (StrUtil.isNotBlank(param.dataType())) {
            schema.put("type", mapSwaggerType(param.dataType()));
          } else {
            schema.put("type", "string"); // 默认类型
          }
          paramMap.put("schema", schema);
          parameters.add(paramMap);
        }
      }

      // 获取参数名称和类型
      String[] paramNames = router.METHOD_PARAM_NAME_MAP.get(method);
      Class<?>[] paramTypes = router.METHOD_PARAM_TYPE_MAP.get(method);

      @SuppressWarnings("unused")
      boolean hasBodyParam = false;

      if (paramNames != null && paramTypes != null) {
        for (int i = 0; i < paramNames.length; i++) {
          Class<?> paramType = paramTypes[i];
          String paramName = paramNames[i]; // 获取参数的实际名称

          if (isPrimitiveOrWrapper(paramType) || paramType == String.class) {
            if (!hasNonHeaderParam) { // 如果没有非header的@ApiImplicitParam
              Map<String, Object> paramMap = new LinkedHashMap<>();
              paramMap.put("name", paramName);
              paramMap.put("in", "query");
              paramMap.put("description", "request parameter");
              paramMap.put("required", true);
              Map<String, Object> schema = new LinkedHashMap<>();
              schema.put("type", mapSwaggerType(paramType.getSimpleName()));
              paramMap.put("schema", schema);
              parameters.add(paramMap);
            }
            operation.put("consumes", Collections.singletonList("*/*"));
          } else {
            // 如果参数是复杂对象，添加为body参数
            Map<String, Object> bodyParam = new LinkedHashMap<>();
            bodyParam.put("name", paramName);
            bodyParam.put("in", "body");
            bodyParam.put("description", "request body");
            bodyParam.put("required", true);
            Map<String, Object> schema = new LinkedHashMap<>();
            schema.put("$ref", "#/definitions/" + paramType.getSimpleName());
            bodyParam.put("schema", schema);
            parameters.add(bodyParam);

            // 设置 consumes 为 application/json
            operation.put("consumes", Collections.singletonList("application/json"));
            hasBodyParam = true;

            // 将复杂对象添加到 definitions 以生成其结构
            definitionsToGenerate.add(paramType);
          }
        }
      } else {
        // 没有请求参数时，默认的 consumes
        operation.put("consumes", Collections.singletonList("*/*"));
      }

      if (!parameters.isEmpty()) {
        operation.put("parameters", parameters);
      }

      // 处理响应
      ApiResponses apiResponses = method.getAnnotation(ApiResponses.class);

      if (apiResponses != null) {
        // 使用自定义的 @ApiResponses 注解定义
        operation.put("produces", Collections.singletonList("application/json"));
        operation.put("responses", createApiResponses(apiResponses, method));
      } else {
        // 现有的响应处理逻辑
        Class<?> returnType = method.getReturnType();
        if (returnType.getName().equals("com.litongjava.model.body.RespBodyVo")) {
          operation.put("produces", Collections.singletonList("application/json"));
          operation.put("responses", createCustomResponseForRespBodyVo());
          responseClasses.add(returnType); // 添加到 responseClasses 以生成定义
        } else if (returnType.getName().equals("com.litongjava.tio.http.common.HttpResponse")) {
          operation.put("produces", Collections.singletonList("*/*"));
          operation.put("responses", createStandardHttpResponse());
        } else {
          operation.put("produces", Collections.singletonList("application/json"));
          operation.put("responses", createStandardHttpResponse());
        }
      }

      // 添加到 paths
      pathItem.put(httpMethod, operation);
    }

    swagger.put("paths", paths);

    // 动态获取定义部分
    Map<String, Object> definitions = new LinkedHashMap<>();

    // 创建一个新的集合来存储需要处理的类，以避免在遍历时修改原始集合
    Set<Class<?>> classesToProcess = new HashSet<>(definitionsToGenerate);

    // 清除 definitionsToGenerate 以避免重复处理
    definitionsToGenerate.clear();

    // 使用一个循环来确保所有新增的类也被处理
    while (!classesToProcess.isEmpty()) {
      Iterator<Class<?>> iterator = classesToProcess.iterator();
      Class<?> responseClass = iterator.next();
      iterator.remove(); // 移除已处理的类

      // 跳过通用集合类型，如 List、Map 等
      if (isGenericCollection(responseClass)) {
        log.info("Skipping generic collection class: {}", responseClass.getSimpleName());
        continue;
      }

      try {
        definitions.put(responseClass.getSimpleName(), generateDefinition(responseClass));
      } catch (ClassNotFoundException e) {
        log.error("Failed to generate definition for class: {}", responseClass, e);
      }

      // 如果在 generateDefinition 方法中有新增类被添加到 definitionsToGenerate，
      // 将这些新增类添加到 classesToProcess 中以进行处理
      for (Class<?> newClass : definitionsToGenerate) {
        if (!definitions.containsKey(newClass.getSimpleName()) && !classesToProcess.contains(newClass) && !isGenericCollection(newClass)) {
          classesToProcess.add(newClass);
        }
      }

      // 清除 definitionsToGenerate 以避免重复添加
      definitionsToGenerate.clear();
    }

    // 处理 responseClasses 中的类
    for (Class<?> responseClass : responseClasses) {
      // 跳过通用集合类型，如 List、Map 等
      if (isGenericCollection(responseClass)) {
        log.info("Skipping generic collection class: {}", responseClass.getSimpleName());
        continue;
      }

      try {
        definitions.put(responseClass.getSimpleName(), generateDefinition(responseClass));
      } catch (ClassNotFoundException e) {
        log.error("Failed to generate definition for class: {}", responseClass, e);
      }
    }

    swagger.put("definitions", definitions);
    String json = JsonUtils.toJson(swagger);
    return json;
  }

  /**
   * 创建自定义的 RespBodyVo 响应描述
   */
  private static Map<String, Object> createCustomResponseForRespBodyVo() {
    Map<String, Object> responses = new LinkedHashMap<>();
    Map<String, Object> response200 = new LinkedHashMap<>();
    response200.put("description", "Success");
    Map<String, Object> schema = new LinkedHashMap<>();
    schema.put("type", "object");

    Map<String, Object> properties = new LinkedHashMap<>();
    properties.put("code", Collections.singletonMap("type", "integer"));
    properties.put("data", Collections.singletonMap("type", "object"));
    properties.put("msg", Collections.singletonMap("type", "string"));
    properties.put("ok", Collections.singletonMap("type", "boolean"));

    schema.put("properties", properties);
    response200.put("schema", schema);

    responses.put("200", response200);
    responses.put("400", createDefaultResponse("Bad Request"));
    responses.put("401", createDefaultResponse("Unauthorized"));
    responses.put("403", createDefaultResponse("Forbidden"));
    responses.put("404", createDefaultResponse("Not Found"));
    responses.put("500", createDefaultResponse("Internal Server Error"));

    return responses;
  }

  /**
   * 创建标准 HttpResponse 响应描述
   */
  private static Map<String, Object> createStandardHttpResponse() {
    Map<String, Object> responses = new LinkedHashMap<>();
    responses.put("200", createDefaultResponse("Standard HTTP Response"));
    responses.put("400", createDefaultResponse("Bad Request"));
    responses.put("401", createDefaultResponse("Unauthorized"));
    responses.put("403", createDefaultResponse("Forbidden"));
    responses.put("404", createDefaultResponse("Not Found"));
    responses.put("500", createDefaultResponse("Internal Server Error"));
    return responses;
  }

  /**
   * 创建响应集合，解析 @ApiResponses 注解
   */
  private static Map<String, Object> createApiResponses(ApiResponses apiResponses, Method method) {
    Map<String, Object> responses = new LinkedHashMap<>();
    for (ApiResponse apiResponse : apiResponses.value()) {
      Map<String, Object> response = new LinkedHashMap<>();
      response.put("description", apiResponse.message());

      // 检查是否有 response 属性
      Class<?> responseClass = apiResponse.response();
      if (responseClass != null && responseClass != Void.class) {
        response.put("schema", Collections.singletonMap("$ref", "#/definitions/" + responseClass.getSimpleName()));
        // 将响应类添加到 definitions
        definitionsToGenerate.add(responseClass);
      }

      responses.put(String.valueOf(apiResponse.code()), response);
    }
    return responses;
  }

  /**
   * 使用反射生成类定义，增加对 @ApiModelProperty 注解的处理
   *
   * @param clazz 类对象
   * @return 类的 Swagger 定义
   * @throws ClassNotFoundException 如果类找不到
   */
  private static Map<String, Object> generateDefinition(Class<?> clazz) throws ClassNotFoundException {
    Map<String, Object> definition = new LinkedHashMap<>();
    definition.put("type", "object");
    definition.put("title", clazz.getSimpleName());

    // 处理类上的 @ApiModel 注解
    ApiModel apiModel = clazz.getAnnotation(ApiModel.class);
    if (apiModel != null) {
      if (StrUtil.isNotBlank(apiModel.value())) {
        definition.put("title", apiModel.value());
      }
      if (StrUtil.isNotBlank(apiModel.description())) {
        definition.put("description", apiModel.description());
      }
    }

    Map<String, Object> properties = new LinkedHashMap<>();
    List<String> requiredFields = new ArrayList<>();

    // 特殊处理 com.litongjava.model.body.RespBodyVo
    if (clazz.getName().equals("com.litongjava.model.body.RespBodyVo")) {
      properties.put("code", Collections.singletonMap("type", "integer"));
      properties.put("data", Collections.singletonMap("type", "object"));
      properties.put("msg", Collections.singletonMap("type", "string"));
      properties.put("ok", Collections.singletonMap("type", "boolean"));
    } else if (clazz.getName().equals("com.litongjava.tio.http.common.HttpRequest")) {

    } else {
      Field[] declaredFields = null;
      try {
        declaredFields = clazz.getDeclaredFields();
      } catch (Exception e) {
        log.error("Failed to build class: {}, error: {}", clazz, e);
      }

      if (declaredFields != null) {
        for (Field field : declaredFields) {
          // 跳过 static 和 transient 字段
          if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) || java.lang.reflect.Modifier.isTransient(field.getModifiers())) {
            continue;
          }

          // 跳过导致自引用的字段
          if (field.getType().equals(clazz)) {
            log.warn("Skipping self-referencing field: {} in class: {}", field.getName(), clazz.getSimpleName());
            continue;
          }

          Map<String, Object> fieldInfo = new LinkedHashMap<>();
          Class<?> fieldClass = field.getType();
          String fieldType = fieldClass.getSimpleName();

          // 处理 @ApiModelProperty 注解
          ApiModelProperty apiModelProperty = field.getAnnotation(ApiModelProperty.class);
          if (apiModelProperty != null) {
            // 添加描述
            if (StrUtil.isNotBlank(apiModelProperty.value())) {
              fieldInfo.put("description", apiModelProperty.value());
            }
            // 处理 required 属性
            if (apiModelProperty.required()) {
              requiredFields.add(field.getName());
            }
            // 处理 dataType
            if (StrUtil.isNotBlank(apiModelProperty.dataType())) {
              fieldType = apiModelProperty.dataType();
            }
            // 处理 allowableValues
            if (StrUtil.isNotBlank(apiModelProperty.allowableValues())) {
              String allowableValues = apiModelProperty.allowableValues();
              String[] values = allowableValues.split(",");
              fieldInfo.put("enum", Arrays.asList(values));
            }
            // 处理 example
            if (StrUtil.isNotBlank(apiModelProperty.example())) {
              fieldInfo.put("example", apiModelProperty.example());
            }
          }

          if (fieldClass.isEnum()) {
            fieldInfo.put("type", "string");
            Object[] enumConstants = fieldClass.getEnumConstants();
            List<String> enumValues = new ArrayList<>();
            for (Object enumConstant : enumConstants) {
              enumValues.add(enumConstant.toString());
            }
            fieldInfo.put("enum", enumValues);
          } else if (Collection.class.isAssignableFrom(fieldClass)) {
            fieldInfo.put("type", "array");
            Type genericType = field.getGenericType();
            if (genericType instanceof ParameterizedType) {
              Type[] actualTypeArguments = ((ParameterizedType) genericType).getActualTypeArguments();
              if (actualTypeArguments.length > 0) {
                String itemTypeName = actualTypeArguments[0].getTypeName();
                try {
                  Class<?> itemClass = Class.forName(itemTypeName);
                  if (isPrimitiveOrWrapper(itemClass) || itemClass == String.class) {
                    Map<String, Object> items = new LinkedHashMap<>();
                    items.put("type", mapSwaggerType(itemClass.getSimpleName()));
                    fieldInfo.put("items", items);
                  } else {
                    fieldInfo.put("items", Collections.singletonMap("$ref", "#/definitions/" + itemClass.getSimpleName()));
                    // 避免添加通用集合
                    if (!isGenericCollection(itemClass)) {
                      definitionsToGenerate.add(itemClass);
                    }
                  }
                } catch (ClassNotFoundException e) {
                  fieldInfo.put("items", Collections.singletonMap("type", "object"));
                }
              } else {
                fieldInfo.put("items", Collections.singletonMap("type", "object"));
              }
            } else {
              fieldInfo.put("items", Collections.singletonMap("type", "object"));
            }
          } else if (Map.class.isAssignableFrom(fieldClass)) {
            fieldInfo.put("type", "object");
            // 处理 Map 类型，设置 additionalProperties
            fieldInfo.put("additionalProperties", true);
          } else if (!isPrimitiveOrWrapper(fieldClass) && !fieldType.equalsIgnoreCase("String") && !fieldClass.isEnum()) {
            fieldInfo.put("$ref", "#/definitions/" + fieldClass.getSimpleName());
            if (!isGenericCollection(fieldClass)) {
              definitionsToGenerate.add(fieldClass);
            }
          } else {
            fieldInfo.put("type", mapSwaggerType(fieldType));
          }

          properties.put(field.getName(), fieldInfo);
        }
      }
    }

    definition.put("properties", properties);
    if (!requiredFields.isEmpty()) {
      definition.put("required", requiredFields);
    }
    return definition;
  }

  /**
   * 检查类型是否是基本类型或包装类型
   *
   * @param clazz 类对象
   * @return 是否是基本类型或包装类型
   */
  private static boolean isPrimitiveOrWrapper(Class<?> clazz) {
    return clazz.isPrimitive() || clazz == Integer.class || clazz == Long.class || clazz == Boolean.class || clazz == Double.class || clazz == Float.class || clazz == Character.class
        || clazz == Byte.class || clazz == Short.class || clazz == Void.class;
  }

  /**
   * 检查类是否为通用集合类型（如 List、Map）
   *
   * @param clazz 类对象
   * @return 是否为通用集合类型
   */
  private static boolean isGenericCollection(Class<?> clazz) {
    return Collection.class.isAssignableFrom(clazz) || Map.class.isAssignableFrom(clazz);
  }

  /**
   * 映射自定义数据类型到 Swagger 数据类型
   *
   * @param dataType 数据类型名称
   * @return Swagger 数据类型
   */
  private static String mapSwaggerType(String dataType) {
    if (StrUtil.isBlank(dataType)) {
      return "string";
    }
    switch (dataType.toLowerCase()) {
    case "int":
    case "integer":
      return "integer";
    case "long":
      return "long";
    case "boolean":
      return "boolean";
    case "float":
      return "float";
    case "double":
      return "double";
    case "string":
      return "string";
    case "list":
      return "array";
    case "map":
      return "object";
    default:
      return "object"; // 默认类型
    }
  }

  /**
   * 创建默认响应对象
   *
   * @param description 响应描述
   * @return 响应对象
   */
  private static Map<String, Object> createDefaultResponse(String description) {
    Map<String, Object> response = new LinkedHashMap<>();
    response.put("description", description);
    return response;
  }
}