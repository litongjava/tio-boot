package nexus.io.tio.utils.lock;

/**
 * @author tanyaowu
 */
public interface ReadWriteLockHandler {
  /**
   * 
   * @return
   */
  public void write() throws Exception;
}
