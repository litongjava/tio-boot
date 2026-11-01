package nexus.io.tio.boot.server;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nexus.io.tio.core.ChannelContext;

/**
 * Per-connection close callbacks for server channels. A callback added here
 * runs once when the connection closes for any reason: a close frame, a
 * network error or a heartbeat timeout. Proxies use it to release the upstream
 * connection that belongs to a client connection.
 */
public class ChannelCloseHooks {
  private static final Logger log = LoggerFactory.getLogger(ChannelCloseHooks.class);

  private static final String ATTRIBUTE = "__tio_boot_close_hooks";

  /**
   * Adds a callback to run when the connection closes.
   *
   * @param channelContext server connection
   * @param hook           callback
   */
  @SuppressWarnings("unchecked")
  public static void add(ChannelContext channelContext, Runnable hook) {
    synchronized (channelContext) {
      List<Runnable> hooks = (List<Runnable>) channelContext.getAttribute(ATTRIBUTE);
      if (hooks == null) {
        hooks = new CopyOnWriteArrayList<>();
        channelContext.setAttribute(ATTRIBUTE, hooks);
      }
      hooks.add(hook);
    }
  }

  /**
   * Runs and clears the callbacks of the connection.
   *
   * @param channelContext server connection
   */
  @SuppressWarnings("unchecked")
  public static void run(ChannelContext channelContext) {
    List<Runnable> hooks;
    synchronized (channelContext) {
      hooks = (List<Runnable>) channelContext.getAttribute(ATTRIBUTE);
      if (hooks == null) {
        return;
      }
      channelContext.setAttribute(ATTRIBUTE, null);
    }
    for (Runnable hook : hooks) {
      try {
        hook.run();
      } catch (Exception e) {
        log.error("close hook failed", e);
      }
    }
  }
}
