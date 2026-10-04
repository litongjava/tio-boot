package nexus.io.tio.utils.commandline;

import static org.junit.Assert.*;
import java.io.File;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.testng.annotations.Test;

public class ProcessLifecycleRegressionTest {
  private List<String> child(String... args) throws Exception {
    String executable = new File(System.getProperty("java.home"), "bin/java").getPath();
    String classes = new File(Child.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();
    List<String> command = new ArrayList<>(Arrays.asList(executable, "-cp", classes, Child.class.getName()));
    command.addAll(Arrays.asList(args));
    return command;
  }

  @Test public void quotesWhitespaceAndLiteralArgumentsArePreserved() throws Exception {
    assertEquals(Arrays.asList("C:\\Program Files\\tool.exe", "a b.txt", "", "c d", "|"),
        ProcessUtils.parseCommand("  \"C:\\Program Files\\tool.exe\"\t\"a b.txt\" '' 'c d' |  "));
    assertEquals(Arrays.asList("tool", "a\"b"), ProcessUtils.parseCommand("tool \"a\\\"b\""));
    for (String invalid : new String[] {null, " ", "tool 'unterminated"}) {
      try { ProcessUtils.parseCommand(invalid); fail("Accepted malformed command"); }
      catch (IllegalArgumentException expected) { }
    }
    Path dir = Files.createTempDirectory(Paths.get("target"), "process-args-");
    ProcessResult result = ProcessUtils.execute(dir.toFile(), child("echo", "a b.txt", "", "x|y"));
    assertEquals(0, result.getExitCode());
    assertTrue(result.getStdOut(), result.getStdOut().contains("[a b.txt, , x|y]"));
  }

  @Test public void interruptionTerminatesChildBeforeReturning() throws Exception {
    Path dir = Files.createTempDirectory(Paths.get("target").toAbsolutePath(), "process-interrupt-");
    Path ready = dir.resolve("ready");
    ProcessBuilder builder = new ProcessBuilder(child("wait", ready.toString()));
    AtomicReference<Throwable> failure = new AtomicReference<>();
    Thread worker = new Thread(() -> {
      try { ProcessUtils.execute(dir.toFile(), builder); failure.set(new AssertionError("No interruption")); }
      catch (InterruptedException expected) {
        if (!Thread.currentThread().isInterrupted()) failure.set(new AssertionError("Interrupt status lost"));
      } catch (Throwable e) { failure.set(e); }
    });
    worker.start();
    try {
      long deadline = System.nanoTime() + 5_000_000_000L;
      while (!Files.exists(ready) && worker.isAlive() && System.nanoTime() < deadline) Thread.sleep(20);
      assertTrue("Child did not start: " + failure.get(), Files.exists(ready));
      int port = Integer.parseInt(new String(Files.readAllBytes(ready), StandardCharsets.UTF_8));
      worker.interrupt();
      worker.join(7000);
      assertFalse("Worker did not finish cleanup", worker.isAlive());
      if (failure.get() != null) throw new AssertionError(failure.get());
      // Windows may briefly complete a TCP handshake while tearing down a dead
      // process's listening socket. Bound the probe well below the child's sleep.
      boolean closed = false;
      long closeDeadline = System.nanoTime() + 2_000_000_000L;
      while (!closed && System.nanoTime() < closeDeadline) {
        try (Socket socket = new Socket()) {
          socket.connect(new InetSocketAddress("127.0.0.1", port), 200);
        } catch (java.io.IOException expected) { closed = true; }
        if (!closed) Thread.sleep(20);
      }
      assertTrue("Child still accepts connections after cleanup", closed);
    } finally { worker.interrupt(); worker.join(7000); }
  }

  @Test public void timeoutReturnsMinusOne() throws Exception {
    Path dir = Files.createTempDirectory(Paths.get("target"), "process-timeout-");
    ProcessResult result = ProcessUtils.execute(dir.toFile(), "timeout", new ProcessBuilder(child("sleep")), 1);
    assertEquals(-1, result.getExitCode());
  }

  public static class Child {
    public static void main(String[] args) throws Exception {
      if (args[0].equals("echo")) System.out.println(Arrays.toString(Arrays.copyOfRange(args, 1, args.length)));
      else if (args[0].equals("wait")) {
        try (ServerSocket server = new ServerSocket(0, 1, java.net.InetAddress.getByName("127.0.0.1"))) {
          Path ready = Paths.get(args[1]);
          Path temp = ready.resolveSibling("ready.tmp");
          Files.write(temp, Integer.toString(server.getLocalPort()).getBytes(StandardCharsets.UTF_8));
          Files.move(temp, ready);
          Thread.sleep(15000);
        }
      } else Thread.sleep(15000);
    }
  }
}
