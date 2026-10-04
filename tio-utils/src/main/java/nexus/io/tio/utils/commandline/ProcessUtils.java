package nexus.io.tio.utils.commandline;

import java.io.File;
import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.List;
import java.io.Closeable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nexus.io.tio.utils.hutool.FileUtil;
import nexus.io.tio.utils.snowflake.SnowflakeIdUtils;

public class ProcessUtils {

  private static final Logger log = LoggerFactory.getLogger(ProcessUtils.class);

  /** 默认读取日志的最大字节数（每个文件），防止 OOM：2MB */
  private static final int DEFAULT_MAX_READ_BYTES = 2 * 1024 * 1024;
  /** destroy() 后的宽限期（秒） */
  private static final int GRACE_SECONDS = 5;
  /** destroyForcibly() 后再等（秒） */
  private static final int FORCE_SECONDS = 5;

  /**
   * Executes a command line with single/double quoted arguments. Despite the
   * historical name, no shell is invoked: operators and variables are literal.
   * Prefer {@link #execute(File, List)} for arbitrary paths and arguments.
   */
  public static ProcessResult executeShell(String command) throws IOException, InterruptedException {
    ProcessBuilder pb = new ProcessBuilder(parseCommand(command));
    long id = SnowflakeIdUtils.id();
    String folder = "shell-tasks" + File.separator + id;
    File outDir = new File(folder);
    if (!outDir.exists()) {
      outDir.mkdirs();
    }
    File runSh = new File(folder, id + "_run.sh");
    FileUtil.writeString(command, runSh);
    return execute(outDir, pb);
  }

  /** Executes literal arguments, without shell expansion, pipes or redirection. */
  public static ProcessResult execute(File outDir, List<String> arguments) throws IOException, InterruptedException {
    return execute(outDir, new ProcessBuilder(new ArrayList<>(arguments)));
  }

  /** Tokenizes single/double quoted arguments. Shell operators remain literal arguments. */
  static List<String> parseCommand(String command) {
    if (command == null) throw new IllegalArgumentException("Command must not be null");
    List<String> arguments = new ArrayList<>();
    StringBuilder argument = new StringBuilder();
    char quote = 0;
    boolean started = false;
    for (int i = 0; i < command.length(); i++) {
      char c = command.charAt(i);
      if (c == '\\' && quote != '\'' && i + 1 < command.length()
          && (command.charAt(i + 1) == '"' || (quote == 0 && command.charAt(i + 1) == '\''))) {
        argument.append(command.charAt(++i));
        started = true;
      } else if (quote != 0) {
        if (c == quote) quote = 0;
        else argument.append(c);
        started = true;
      } else if (c == '\'' || c == '"') {
        quote = c;
        started = true;
      } else if (Character.isWhitespace(c)) {
        if (started) {
          arguments.add(argument.toString());
          argument.setLength(0);
          started = false;
        }
      } else {
        argument.append(c);
        started = true;
      }
    }
    if (quote != 0) throw new IllegalArgumentException("Unclosed command quote");
    if (started) arguments.add(argument.toString());
    if (arguments.isEmpty() || arguments.get(0).isEmpty()) throw new IllegalArgumentException("Command must not be empty");
    return arguments;
  }

  public static ProcessResult execute(File outDir, ProcessBuilder pb) throws IOException, InterruptedException {
    // 沿用你原本的 10*60 秒
    return run(outDir, null, pb, 10 * 60, DEFAULT_MAX_READ_BYTES);
  }

  /**
   * @param outDir  日志目录
   * @param id      日志文件前缀 id
   * @param pb      ProcessBuilder
   * @param timeout 超时时间（秒）
   */
  public static ProcessResult execute(File outDir, String taskName, ProcessBuilder pb, int timeout)
      throws IOException, InterruptedException {
    return run(outDir, taskName, pb, timeout, DEFAULT_MAX_READ_BYTES);
  }

  // ======================
  // 私有实现
  // ======================

  private static ProcessResult run(File outDir, String taskName, ProcessBuilder pb, int timeoutSeconds,
      int maxReadBytes) throws IOException, InterruptedException {

    if (outDir != null && !outDir.exists()) {
      // 并发安全：mkdirs 本身幂等；失败时抛异常
      if (!outDir.mkdirs() && !outDir.isDirectory()) {
        throw new IOException("Failed to create log directory: " + outDir.getAbsolutePath());
      }
    }

    final String prefix = (taskName == null) ? "" : (String.valueOf(taskName) + "_");
    final File stdoutFile = new File(outDir, prefix + "stdout.log");
    final File stderrFile = new File(outDir, prefix + "stderr.log");

    // 将输出和错误流重定向到文件
    pb.redirectOutput(stdoutFile);
    pb.redirectError(stderrFile);

    long start = System.currentTimeMillis();
    int exitCode;
    boolean timedOut = false;

    Process process = null;
    try {
      process = pb.start();
      // No input is supplied by this API; allow programs reading stdin to observe EOF.
      process.getOutputStream().close();
      boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);

      if (!finished) {
        timedOut = true;
        log.error("{} process did not finish within {} seconds. Attempting graceful shutdown...",
            outDir != null ? outDir.getAbsolutePath() : "<no-outDir>", timeoutSeconds);

        // 先尝试温和终止
        process.destroy();
        boolean ended = process.waitFor(GRACE_SECONDS, TimeUnit.SECONDS);

        if (!ended) {
          log.warn("Process did not terminate after destroy(); escalating to destroyForcibly()...");
          process.destroyForcibly();
          // 再给一点缓冲时间
          if (!process.waitFor(FORCE_SECONDS, TimeUnit.SECONDS)) {
            throw new IOException("Process did not terminate after forced shutdown");
          }
        }
      }

      // 走到这里，不管是否超时都可以取 exitValue
      exitCode = process.exitValue();
      if (timedOut) {
        // 用 -1 标示你的“超时”特殊码；保持与原有逻辑兼容
        exitCode = -1;
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw e;
    } finally {
      cleanup(process);
    }

    long end = System.currentTimeMillis();

    // 读取日志（尾部最多 2MB，防止 OOM）
    String stdoutContent = safeReadTail(stdoutFile.toPath(), maxReadBytes);
    String stderrContent = safeReadTail(stderrFile.toPath(), maxReadBytes);

    // 如果是超时，在 stderr 末尾追加说明，便于你上层定位
    if (timedOut) {
      stderrContent = appendLine(stderrContent, "[ProcessUtils] Process timed out after " + timeoutSeconds
          + " seconds. " + "Sent destroy(), then destroyForcibly() if needed.");
    }

    ProcessResult result = new ProcessResult();
    result.setExitCode(exitCode);
    result.setStdOut(stdoutContent);
    result.setStdErr(stderrContent);
    result.setElapsed(end - start);
    return result;
  }

  private static void cleanup(Process process) {
    if (process == null) return;
    boolean interrupted = Thread.interrupted();
    try {
      if (process.isAlive()) {
        process.destroyForcibly();
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(FORCE_SECONDS);
        while (process.isAlive()) {
          long remaining = deadline - System.nanoTime();
          if (remaining <= 0) {
            log.warn("Process is still alive after forced cleanup");
            break;
          }
          try { process.waitFor(remaining, TimeUnit.NANOSECONDS); }
          catch (InterruptedException e) { interrupted = true; }
        }
      }
    } finally {
      closeQuietly(process.getOutputStream());
      closeQuietly(process.getInputStream());
      closeQuietly(process.getErrorStream());
      if (interrupted) Thread.currentThread().interrupt();
    }
  }

  private static void closeQuietly(Closeable stream) {
    try { stream.close(); } catch (IOException e) { log.debug("Could not close process stream", e); }
  }

  // ======================
  // 工具方法
  // ======================

  /**
   * 安全读取文件“尾部”，最多读取 maxBytes 字节；若文件更大，会在末尾追加一行说明被截断。
   */
  private static String safeReadTail(Path path, int maxBytes) throws IOException {
    if (path == null)
      return "";
    if (!Files.exists(path))
      return "";
    long size = Files.size(path);
    if (size <= 0)
      return "";

    int toRead;
    long offset;
    if (size <= maxBytes) {
      toRead = (int) size;
      offset = 0;
    } else {
      toRead = maxBytes;
      offset = size - maxBytes;
    }

    byte[] buf = new byte[toRead];

    try (SeekableByteChannel channel = Files.newByteChannel(path);) {
      channel.position(offset);
      int n = channel.read(java.nio.ByteBuffer.wrap(buf));
      if (n < toRead && n > 0) {
        byte[] shrink = new byte[n];
        System.arraycopy(buf, 0, shrink, 0, n);
        buf = shrink;
      }
    }

    String content = new String(buf, StandardCharsets.UTF_8);
    if (size > maxBytes) {
      content = appendLine(content,
          "[ProcessUtils] Log truncated to last " + maxBytes + " bytes (file size: " + size + ").");
    }
    return content;
  }

  private static String appendLine(String s, String line) {
    if (s == null || s.isEmpty())
      return line + System.lineSeparator();
    String sep = s.endsWith("\n") ? "" : System.lineSeparator();
    return s + sep + line + System.lineSeparator();
  }
}
