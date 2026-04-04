package nexus.io.tio.boot.context;

import java.io.IOException;

public interface DynamicReloadConfig {
  public void config() throws IOException;

}
