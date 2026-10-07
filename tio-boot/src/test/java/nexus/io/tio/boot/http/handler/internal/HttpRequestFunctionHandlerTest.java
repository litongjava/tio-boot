package nexus.io.tio.boot.http.handler.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.Test;

import nexus.io.annotation.EnableCORS;
import nexus.io.model.body.RespBodyVo;
import nexus.io.model.exception.BusinessException;
import nexus.io.model.type.TioTypeReference;
import nexus.io.tio.boot.http.TioRequestContext;
import nexus.io.tio.http.common.HeaderName;
import nexus.io.tio.http.common.HttpRequest;
import nexus.io.tio.http.common.RequestLine;
import nexus.io.tio.http.common.HttpConst;
import nexus.io.tio.http.common.HttpResponse;
import nexus.io.tio.http.server.handler.IHttpRequestFunction;
import nexus.io.tio.http.server.handler.RouteEntry;
import nexus.io.tio.http.server.router.DefaultHttpRequestFunctionRouter;
import nexus.io.tio.http.server.router.HttpRequestFunctionRouter;
import nexus.io.model.exception.ParameterValidationException;

public class HttpRequestFunctionHandlerTest {
  public static class Input {
    public String name;
  }

  public static class GreetingService {
    public RespBodyVo greet(Input input) {
      return RespBodyVo.ok(input.name);
    }
  }

  @EnableCORS(allowOrigin = "https://class.example")
  public static class BaseFunction implements IHttpRequestFunction<RespBodyVo, Input> {
    @Override
    public RespBodyVo handle(Input input) {
      return RespBodyVo.ok(input.name);
    }
  }

  public static class InheritedFunction extends BaseFunction {
  }

  public static class AnnotatedMethodFunction extends BaseFunction {
    @Override
    @EnableCORS(allowOrigin = "https://method.example")
    public RespBodyVo handle(Input input) {
      return RespBodyVo.ok(input.name);
    }
  }

  public static class InheritedMethodFunction extends AnnotatedMethodFunction {
  }

  @After
  public void releaseContext() {
    TioRequestContext.release();
  }

  private <R, T> HttpResponse execute(String body, IHttpRequestFunction<R, T> function, TioTypeReference<T> type) {
    return execute(body, new RouteEntry<R, T>(function, type));
  }

  private HttpResponse execute(String body, RouteEntry<?, ?> entry) {
    HttpRequest request = new HttpRequest();
    request.requestLine = new RequestLine();
    request.requestLine.setVersion(HttpConst.HttpVersion.V1_1);
    request.setCharset("UTF-8");
    request.setBody(body == null ? null : body.getBytes(StandardCharsets.UTF_8));
    request.setBodyString(body);
    TioRequestContext.hold(request, new HttpResponse(request));
    return new HttpRequestFunctionHandler().handleFunction(request, null, false, entry, "/greeting");
  }

  @Test
  public void registersServiceMethodReferenceWithoutAHandler() {
    GreetingService service = new GreetingService();
    HttpRequestFunctionRouter router = new DefaultHttpRequestFunctionRouter();
    router.add("/greeting", service::greet, new TioTypeReference<Input>() { });
    HttpResponse response = execute("{\"name\":\"Alice\"}", router.find("/greeting"));
    String body = new String(response.getBody(), StandardCharsets.UTF_8);
    assertTrue(body.contains("Alice"));
    assertNotNull(response.getHeader(HeaderName.Content_Type));
  }

  @Test
  public void bindsGenericListToEntities() {
    AtomicReference<List<Input>> captured = new AtomicReference<>();
    execute("[{\"name\":\"Alice\"}]", input -> {
      captured.set(input);
      return RespBodyVo.ok();
    }, new TioTypeReference<List<Input>>() { });
    assertEquals("Alice", captured.get().get(0).name);
  }

  @Test
  public void preservesBusinessAndValidationExceptionIdentity() {
    BusinessException business = new BusinessException(409, "CONFLICT", "Conflict");
    assertSame(business, assertThrows(BusinessException.class, () -> execute("text", input -> {
      throw business;
    }, new TioTypeReference<String>() { })));
    ParameterValidationException invalid = new ParameterValidationException("Invalid name");
    assertSame(invalid, assertThrows(ParameterValidationException.class, () -> execute("{}", input -> {
      throw invalid;
    }, new TioTypeReference<Input>() { })));
  }

  @Test
  public void doesNotMisclassifyServiceClassCastException() {
    ClassCastException failure = new ClassCastException("Service failure");
    assertSame(failure, assertThrows(ClassCastException.class, () -> execute("{}", input -> {
      throw failure;
    }, new TioTypeReference<Input>() { })));
  }

  @Test
  public void preservesCheckedExceptionCause() {
    IOException failure = new IOException("Unavailable");
    RuntimeException actual = assertThrows(RuntimeException.class, () -> execute("text", input -> {
      throw failure;
    }, new TioTypeReference<String>() { }));
    assertSame(failure, actual.getCause());
  }

  @Test
  public void rejectsInvalidJsonBeforeCallingService() {
    AtomicInteger calls = new AtomicInteger();
    assertThrows(ParameterValidationException.class, () -> execute("{bad", input -> {
      calls.incrementAndGet();
      return RespBodyVo.ok();
    }, new TioTypeReference<Input>() { }));
    assertEquals(0, calls.get());
  }

  private <T> void assertBodyRejected(String body, TioTypeReference<T> type) {
    AtomicInteger calls = new AtomicInteger();
    assertThrows(ParameterValidationException.class, () -> execute(body, input -> {
      calls.incrementAndGet();
      return RespBodyVo.ok();
    }, type));
    assertEquals(0, calls.get());
  }

  @Test
  public void rejectsMissingAndEmptyBodiesBeforeCallingService() {
    for (String body : new String[] { null, "" }) {
      assertBodyRejected(body, new TioTypeReference<String>() { });
      assertBodyRejected(body, new TioTypeReference<byte[]>() { });
      assertBodyRejected(body, new TioTypeReference<Integer>() { });
      assertBodyRejected(body, new TioTypeReference<Input>() { });
      assertBodyRejected(body, new TioTypeReference<List<Input>>() { });
    }
  }

  @Test
  public void rejectsBlankTextAndJsonBodiesBeforeCallingService() {
    for (String body : new String[] { " ", "\r\n\t" }) {
      assertBodyRejected(body, new TioTypeReference<String>() { });
      assertBodyRejected(body, new TioTypeReference<Input>() { });
      assertBodyRejected(body, new TioTypeReference<List<Input>>() { });
    }
  }

  @Test
  public void rejectsJsonNullBeforeCallingService() {
    for (String body : new String[] { "null", " \r\nnull\t" }) {
      assertBodyRejected(body, new TioTypeReference<Input>() { });
      assertBodyRejected(body, new TioTypeReference<List<Input>>() { });
    }
  }

  @Test
  public void preservesNonemptyRawInputsWithoutTrimmingOrJsonParsing() {
    AtomicReference<String> text = new AtomicReference<>();
    execute("  null  ", input -> {
      text.set(input);
      return RespBodyVo.ok();
    }, new TioTypeReference<String>() { });
    assertEquals("  null  ", text.get());
    AtomicReference<byte[]> bytes = new AtomicReference<>();
    execute(" \t", input -> {
      bytes.set(input);
      return RespBodyVo.ok();
    }, new TioTypeReference<byte[]>() { });
    assertEquals(" \t", new String(bytes.get(), StandardCharsets.UTF_8));
  }

  @Test
  public void leavesObjectFieldsAndCollectionSizeToApplicationValidation() {
    AtomicInteger calls = new AtomicInteger();
    execute("{}", input -> {
      assertNotNull(input);
      calls.incrementAndGet();
      return RespBodyVo.ok();
    }, new TioTypeReference<Input>() { });
    execute("[]", input -> {
      assertTrue(input.isEmpty());
      calls.incrementAndGet();
      return RespBodyVo.ok();
    }, new TioTypeReference<List<Input>>() { });
    assertEquals(2, calls.get());
  }

  @Test
  public void validatesBooleanAndCharacterBodies() {
    for (String invalid : new String[] { null, "", "yes", "0" }) {
      assertThrows(ParameterValidationException.class,
          () -> execute(invalid, value -> RespBodyVo.ok(value), new TioTypeReference<Boolean>() { }));
    }
    for (String invalid : new String[] { null, "", "ab" }) {
      assertThrows(ParameterValidationException.class,
          () -> execute(invalid, value -> RespBodyVo.ok(value), new TioTypeReference<Character>() { }));
    }
    AtomicReference<Boolean> booleanValue = new AtomicReference<>();
    execute("TRUE", value -> {
      booleanValue.set(value);
      return RespBodyVo.ok();
    }, new TioTypeReference<Boolean>() { });
    assertEquals(Boolean.TRUE, booleanValue.get());
    AtomicReference<Character> character = new AtomicReference<>();
    execute("a", value -> {
      character.set(value);
      return RespBodyVo.ok();
    }, new TioTypeReference<Character>() { });
    assertEquals(Character.valueOf('a'), character.get());
  }

  @Test
  public void rejectsOverflowAndNonFiniteNumbers() {
    assertThrows(ParameterValidationException.class,
        () -> execute("2147483648", value -> RespBodyVo.ok(value), new TioTypeReference<Integer>() { }));
    assertThrows(ParameterValidationException.class,
        () -> execute("NaN", value -> RespBodyVo.ok(value), new TioTypeReference<Double>() { }));
    assertThrows(ParameterValidationException.class,
        () -> execute("1e100", value -> RespBodyVo.ok(value), new TioTypeReference<Float>() { }));
  }

  @Test
  public void acceptsInheritedFunctionAndClassCors() {
    HttpResponse response = execute("{\"name\":\"Alice\"}", new InheritedFunction(), new TioTypeReference<Input>() { });
    assertEquals("https://class.example", response.getHeader(HeaderName.Access_Control_Allow_Origin).toString());
  }

  @Test
  public void inheritedMethodCorsOverridesClassCors() {
    HttpResponse response = execute("{\"name\":\"Alice\"}", new InheritedMethodFunction(), new TioTypeReference<Input>() { });
    assertEquals("https://method.example", response.getHeader(HeaderName.Access_Control_Allow_Origin).toString());
  }

  @Test
  public void acceptsRawBytesAndAnExistingResponse() {
    AtomicReference<byte[]> captured = new AtomicReference<>();
    HttpResponse response = execute("raw bytes", value -> {
      captured.set(value);
      return TioRequestContext.getResponse().setStatus(202);
    }, new TioTypeReference<byte[]>() { });
    assertEquals("raw bytes", new String(captured.get(), StandardCharsets.UTF_8));
    assertEquals(202, response.getStatus().status);
  }
}
