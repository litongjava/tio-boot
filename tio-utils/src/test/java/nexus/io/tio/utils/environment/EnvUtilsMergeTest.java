package nexus.io.tio.utils.environment;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.*;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import static org.junit.Assert.*;

public class EnvUtilsMergeTest {
  private Path root;
  private Path home;
  private Path work;
  private String previousEnv;

  @BeforeMethod public void prepare() throws Exception {
    PropUtils.clear();
    resetLoader();
    previousEnv = System.getProperty("app.env");
    System.clearProperty("app.env");
    root = Files.createTempDirectory("env-loading-test");
    home = Files.createDirectory(root.resolve("home"));
    work = Files.createDirectory(root.resolve("work"));
  }

  private void resetLoader() throws Exception {
    Field loaded = EnvUtils.class.getDeclaredField("loaded");
    loaded.setAccessible(true);
    loaded.setBoolean(null, false);
    Field map = EnvUtils.class.getDeclaredField("appMap");
    map.setAccessible(true);
    ((Map<?, ?>) map.get(null)).clear();
    EnvUtils.buildCmdArgsMap(new String[0]);
  }

  @AfterMethod public void cleanup() throws Exception {
    PropUtils.clear();
    resetLoader();
    if (previousEnv == null) { System.clearProperty("app.env"); }
    else { System.setProperty("app.env", previousEnv); }
    try (java.util.stream.Stream<Path> paths = Files.walk(root)) {
      for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toArray(Path[]::new)) {
        Files.delete(path);
      }
    }
  }

  private void write(Path directory, String name, String text) throws Exception {
    Files.write(directory.resolve(name), text.getBytes(StandardCharsets.UTF_8));
  }

  private void load() { EnvUtils.load(home.toFile(), work.toFile(), "config-order.properties"); }

  @Test public void aggregateExistsAndDoesNotAliasSingleFiles() throws Exception {
    Prop aggregate = PropUtils.getProp();
    assertFalse(PropUtils.isLoad());
    write(home, "secrets.txt", "secret=home\n");
    PropUtils.append(home.resolve("secrets.txt").toFile());
    Prop single = PropUtils.use("env-loading.properties");
    assertSame(aggregate, PropUtils.getProp());
    assertEquals("application", EnvUtils.get("merge.application"));
    assertEquals("home", EnvUtils.get("secret"));
    assertNull(single.get("secret"));
    assertNull(single.get("added"));
    Prop extra = new Prop(); extra.getProperties().setProperty("added", "value");
    PropUtils.append(extra);
    assertNull(single.get("added"));
    PropUtils.clear();
    assertSame(aggregate, PropUtils.getProp());
    assertFalse(PropUtils.isLoad());
    assertEquals("application", single.get("merge.application"));
  }

  @Test public void propUtilsDoesNotSelectEnvironment() {
    Prop single = PropUtils.use("config-order.properties");
    assertEquals("base", single.get("winner"));
    assertEquals("base", PropUtils.get("winner"));
  }

  @Test public void optionalFilesSkipAbsenceButReportInvalidContents() throws Exception {
    String missing = work.resolve("missing.properties").toString();
    assertNull(PropUtils.getProp(missing));
    assertNull(PropUtils.useless(missing));
    PropUtils.appendIfExists(missing);
    assertFalse(PropUtils.isLoad());
    write(work, "invalid.properties", "broken=\\uZZZZ\n");
    try {
      PropUtils.appendIfExists(work.resolve("invalid.properties").toString());
      fail("Malformed existing properties must fail");
    } catch (IllegalArgumentException expected) { }
    assertFalse(PropUtils.isLoad());
  }

  @Test public void defaultProfileAndHomeSecretsMerge() throws Exception {
    write(home, "secrets.txt", "secret=home\n");
    load();
    assertEquals("dev-profile", EnvUtils.get("winner"));
    assertEquals("home", EnvUtils.get("secret"));
    assertEquals("dev", EnvUtils.get("app.env"));
  }

  @Test public void commandLineOverridesSystemAndFileEnvironment() throws Exception {
    System.setProperty("app.env", "dev");
    write(work, "secrets.txt", "app.env=dev\n");
    EnvUtils.buildCmdArgsMap(new String[]{"--app.env=test"});
    load();
    assertEquals("test-profile", EnvUtils.get("winner"));
    assertEquals("test", EnvUtils.get("app.env"));
  }

  @Test public void programmaticEnvironmentHasHighestPriority() {
    EnvUtils.buildCmdArgsMap(new String[]{"--app.env=dev"});
    EnvUtils.useTest();
    load();
    assertEquals("test-profile", EnvUtils.get("winner"));
  }

  @Test public void workingFilesSelectEnvironmentAndOverrideProfiles() throws Exception {
    write(work, "secrets.txt", "app.env=test\nsecret=local\n");
    write(work, "config-order-test.properties", "winner=work-profile\nprofileOnly=yes\napp.env=other\n");
    write(work, "my.txt", "winner=last\n");
    load();
    assertEquals("last", EnvUtils.get("winner"));
    assertEquals("yes", EnvUtils.get("profileOnly"));
    assertEquals("test", EnvUtils.get("app.env"));
  }

  @Test public void sameBasenameFilesDoNotShareCache() throws Exception {
    write(home, "same.properties", "value=home\n");
    write(work, "same.properties", "value=work\n");
    Prop first = PropUtils.use(home.resolve("same.properties").toFile());
    Prop second = PropUtils.use(work.resolve("same.properties").toFile());
    assertNotSame(first, second);
    assertEquals("home", first.get("value"));
    assertEquals("work", PropUtils.get("value"));
    assertSame(first, PropUtils.getProp(home.resolve("same.properties").toString()));
  }

  @Test public void failedLoadPublishesNothingAndCanRetry() throws Exception {
    Prop prior = new Prop(); prior.getProperties().setProperty("existing", "kept"); PropUtils.append(prior);
    write(home, "secrets.txt", "unpublished=yes\n");
    write(work, "config-order.properties", "broken=\\uZZZZ\n");
    try { load(); fail("Malformed properties must fail"); }
    catch (IllegalArgumentException expected) { }
    assertNull(EnvUtils.get("unpublished"));
    assertEquals("kept", EnvUtils.get("existing"));
    write(work, "config-order.properties", "winner=repaired\n");
    load();
    assertEquals("yes", EnvUtils.get("unpublished"));
    assertEquals("dev-profile", EnvUtils.get("winner"));
  }

  @Test public void concurrentAndRepeatedLoadPublishesOnce() throws Exception {
    ExecutorService pool = Executors.newFixedThreadPool(4);
    try {
      java.util.List<Future<?>> futures = new java.util.ArrayList<>();
      for (int i = 0; i < 8; i++) { futures.add(pool.submit(() -> load())); }
      for (Future<?> future : futures) { future.get(10, TimeUnit.SECONDS); }
      write(work, "my.txt", "winner=late\n");
      load();
      assertEquals("dev-profile", EnvUtils.get("winner"));
    } finally { pool.shutdownNow(); }
  }

  @Test public void namedLoaderAndLegacyOverloadUseEnvUtilsProfilePolicy() {
    EnvUtils.load("test", "config-order.properties");
    assertEquals("test-profile", EnvUtils.get("winner"));
    PropUtils.clear();
    Prop values = PropUtils.use("config-order.properties", "test");
    assertEquals("test-profile", values.get("winner"));
    assertEquals("test", EnvUtils.get("app.env"));
  }

  @Test public void profileCanLoadWithoutBaseFile() throws Exception {
    write(work, "secrets.txt", "app.env=test\n");
    write(work, "only-test.properties", "only=yes\n");
    EnvUtils.load(home.toFile(), work.toFile(), "only.properties");
    assertEquals("yes", EnvUtils.get("only"));
  }

  @Test public void missingProfileIsOptionalAndInvalidEnvironmentFails() throws Exception {
    EnvUtils.use("missing"); load(); assertEquals("base", EnvUtils.get("winner"));
    resetLoader(); EnvUtils.use("../outside");
    try { load(); fail("Invalid environment must fail"); }
    catch (IllegalArgumentException expected) { }
  }
}
