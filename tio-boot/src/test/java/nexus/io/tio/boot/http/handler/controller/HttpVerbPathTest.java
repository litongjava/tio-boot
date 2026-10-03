package nexus.io.tio.boot.http.handler.controller;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;
import nexus.io.annotation.*;

public class HttpVerbPathTest {
  public static class Controller {
    @Get("") public String list() { return "list"; }
    @Post("/create") public String create() { return "create"; }
    @Put("/{id}") public String update(Long id) { return "update"; }
    @Delete("/{id}") public String remove(Long id) { return "remove"; }
    @RequestPath("/legacy") public String legacy() { return "legacy"; }
  }
  @Test
  public void pathsUseHttpAnnotationsAndKeepRequestPath() throws Exception {
    TioBootHttpControllerRouter router = new TioBootHttpControllerRouter();
    Method scan = TioBootHttpControllerRouter.class.getDeclaredMethod("processClazzMethods", Object.class, String.class, Method[].class);
    scan.setAccessible(true);
    scan.invoke(router, new Controller(), "/test", Controller.class.getDeclaredMethods());
    assertEquals("list", router.PATH_METHOD_MAP.get("GET /test").getName());
    assertEquals("create", router.PATH_METHOD_MAP.get("POST /test/create").getName());
    assertEquals("legacy", router.PATH_METHOD_MAP.get("/test/legacy").getName());
    Field field = TioBootHttpControllerRouter.class.getDeclaredField("PATH_VARIABLE_METHOD_MAP");
    field.setAccessible(true);
    Map<?, ?> variables = (Map<?, ?>) field.get(router);
    assertTrue(variables.containsKey("PUT /test/{id}"));
    assertTrue(variables.containsKey("DELETE /test/{id}"));
    assertFalse(router.PATH_METHOD_MAP.containsKey("GET /test/list"));
  }
}
