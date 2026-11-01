package nexus.io.tio.boot.env;

import java.util.ServiceLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs the {@link EnvLoadedListener}s found on the classpath.
 */
public class EnvLoadedListeners {
  private static final Logger log = LoggerFactory.getLogger(EnvLoadedListeners.class);

  public static void fire(String[] args) {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    if (loader == null) {
      loader = EnvLoadedListeners.class.getClassLoader();
    }
    for (EnvLoadedListener listener : ServiceLoader.load(EnvLoadedListener.class, loader)) {
      log.info("environment listener {}", listener.getClass().getName());
      listener.onEnvLoaded(args);
    }
  }
}
