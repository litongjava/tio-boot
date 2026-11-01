package nexus.io.tio.boot.env;

/**
 * Called once the environment (command-line arguments, app.properties and its
 * profile files, environment variables) is loaded and before tio-boot reads
 * any server setting such as server.port.
 * <p>
 * Use it to add configuration from another source, for example a
 * configuration center, so that it applies to the whole startup. Listeners are
 * discovered with {@link java.util.ServiceLoader}: list the implementation class
 * in {@code META-INF/services/nexus.io.tio.boot.env.EnvLoadedListener}. A
 * library on the classpath is then picked up without any application code.
 */
public interface EnvLoadedListener {

  /**
   * @param args command-line arguments of the application
   */
  void onEnvLoaded(String[] args);
}
