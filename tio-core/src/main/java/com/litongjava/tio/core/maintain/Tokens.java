package com.litongjava.tio.core.maintain;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.litongjava.tio.core.ChannelContext;
import com.litongjava.tio.core.TioConfig;
import com.litongjava.tio.utils.hutool.StrUtil;
import com.litongjava.tio.utils.lock.LockUtils;
import com.litongjava.tio.utils.lock.MapWithLock;
import com.litongjava.tio.utils.lock.SetWithLock;

/**
 *一对多  (token <--> ChannelContext)<br>
 * @author tanyaowu 
 * 2017年10月19日 上午9:40:40
 */
public class Tokens {
  private static Logger log = LoggerFactory.getLogger(Tokens.class);

  /**
   * key: token
   * value: SetWithLock<ChannelContext>
   */
  private MapWithLock<String, SetWithLock<ChannelContext>> mapWithLock = new MapWithLock<>(
      new HashMap<String, SetWithLock<ChannelContext>>());

  private final Object synLockObj1 = new Object();

  /**
   * 绑定token.
   *
   * @param token the token
   * @param channelContext the channel context
   * @author tanyaowu
   */
  public void bind(String token, ChannelContext channelContext) {
    if (channelContext.tioConfig.isShortConnection) {
      return;
    }

    if (StrUtil.isBlank(token)) {
      return;
    }

    try {
      SetWithLock<ChannelContext> setWithLock = mapWithLock.get(token);
      if (setWithLock == null) {
        LockUtils.runWriteOrWaitRead("_tio_tokens_bind__" + token, synLockObj1, () -> {
//					@Override
//					public void read() {
//					}

//					@Override
//					public void write() {
//						SetWithLock<ChannelContext> setWithLock  = mapWithLock.get(token);
          if (mapWithLock.get(token) == null) {
//							setWithLock = new SetWithLock<>(new HashSet<>());
            mapWithLock.put(token, new SetWithLock<>(new HashSet<>()));
          }
//					}
        });
        setWithLock = mapWithLock.get(token);
      }
      setWithLock.add(channelContext);

      channelContext.setToken(token);
    } catch (Throwable e) {
      log.error("", e);
    } finally {
      // lock.unlock();
    }

  }

  /**
   * Find.
   *
   * @param token the token
   * @return the channel context
   */
  public SetWithLock<ChannelContext> find(TioConfig tioConfig, String token) {
    if (tioConfig.isShortConnection) {
      return null;
    }

    if (StrUtil.isBlank(token)) {
      return null;
    }
    String key = token;
    Lock lock = mapWithLock.readLock();
    lock.lock();
    try {
      Map<String, SetWithLock<ChannelContext>> m = mapWithLock.getObj();
      return m.get(key);
    } catch (Throwable e) {
      throw e;
    } finally {
      lock.unlock();
    }
  }

  /**
   * @return the mapWithLock
   */
  public MapWithLock<String, SetWithLock<ChannelContext>> getMap() {
    return mapWithLock;
  }

  /**
   * 解除channelContext绑定的token
   *
   * @param channelContext the channel context
   */
  public void unbind(ChannelContext channelContext) {
    if (channelContext.tioConfig.isShortConnection) {
      return;
    }
    try {
      String token = channelContext.getToken();
      if (StrUtil.isBlank(token)) {
        log.debug("{}, {}, unbind token", channelContext.tioConfig.getName(), channelContext.toString());
        return;
      }

      try {
        SetWithLock<ChannelContext> setWithLock = mapWithLock.get(token);
        if (setWithLock == null) {
          log.warn("{}, {}, token:{}, can't find SetWithLock", channelContext.tioConfig.getName(),
              channelContext.toString(), token);
          return;
        }
        channelContext.setToken(null);
        setWithLock.remove(channelContext);

        if (setWithLock.size() == 0) {
          mapWithLock.remove(token);
        }
      } catch (Throwable e) {
        throw e;
      }
    } catch (Throwable e) {
      log.error(e.toString(), e);
    }
  }

  /**
   * 解除tioConfig范围内所有ChannelContext的 token绑定
   *
   * @param token the token
   * @author tanyaowu
   */
  public void unbind(TioConfig tioConfig, String token) {
    if (tioConfig.isShortConnection) {
      return;
    }
    if (StrUtil.isBlank(token)) {
      return;
    }

    try {
      SetWithLock<ChannelContext> setWithLock = mapWithLock.get(token);
      if (setWithLock == null) {
        return;
      }

      WriteLock writeLock = setWithLock.writeLock();
      writeLock.lock();
      try {
        Set<ChannelContext> set = setWithLock.getObj();
        if (set.size() > 0) {
          for (ChannelContext channelContext : set) {
            channelContext.setToken(null);
          }
          set.clear();
        }

        mapWithLock.remove(token);
      } catch (Throwable e) {
        log.error(e.getMessage(), e);
      } finally {
        writeLock.unlock();
      }
    } catch (Throwable e) {
      throw e;
    }
  }
}
