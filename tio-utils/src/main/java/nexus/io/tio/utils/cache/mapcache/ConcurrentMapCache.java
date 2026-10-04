package nexus.io.tio.utils.cache.mapcache;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

import nexus.io.tio.utils.cache.AbsCache;
import nexus.io.tio.utils.cache.CacheRemovalListener;
import nexus.io.tio.utils.cache.RemovalCause;

public class ConcurrentMapCache extends AbsCache {
  private static final ScheduledThreadPoolExecutor SCHEDULER = createScheduler();
  private final CacheRemovalListener<String, Serializable> removalListener;
  private final ConcurrentHashMap<String, Serializable> map = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, Expiration> expirations = new ConcurrentHashMap<>();
  private final LongSupplier clock;

  private static ScheduledThreadPoolExecutor createScheduler() {
    ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1, task -> {
      Thread thread = new Thread(task, "tio-map-cache-expiration");
      thread.setDaemon(true);
      return thread;
    });
    executor.setRemoveOnCancelPolicy(true);
    return executor;
  }

  private static class Expiration {
    private final long ttlDeadline;
    private long idleDeadline;
    private ScheduledFuture<?> task;

    private Expiration(long ttlDeadline, long idleDeadline) {
      this.ttlDeadline = ttlDeadline;
      this.idleDeadline = idleDeadline;
    }

    private long deadline() {
      return Math.min(ttlDeadline, idleDeadline);
    }

    private void cancel() {
      if (task != null) {
        task.cancel(false);
      }
    }
  }

  public ConcurrentMapCache(String cacheName, Long timeToLiveSeconds, Long timeToIdleSeconds,
      CacheRemovalListener<String, Serializable> removalListener) {
    this(cacheName, timeToLiveSeconds, timeToIdleSeconds, removalListener, System::currentTimeMillis);
  }

  ConcurrentMapCache(String cacheName, Long timeToLiveSeconds, Long timeToIdleSeconds,
      CacheRemovalListener<String, Serializable> removalListener, LongSupplier clock) {
    super(cacheName, timeToLiveSeconds, timeToIdleSeconds);
    this.removalListener = removalListener;
    this.clock = clock;
  }

  @Override
  public synchronized void clear() {
    for (Expiration expiration : expirations.values()) {
      expiration.cancel();
    }
    expirations.clear();
    map.clear();
  }

  @Override
  public Serializable _get(String key) {
    return read(key, true);
  }

  private Serializable read(String key, boolean touch) {
    Serializable expired;
    synchronized (this) {
      Serializable value = map.get(key);
      Expiration expiration = expirations.get(key);
      if (expiration == null) {
        return value;
      }
      long now = clock.getAsLong();
      if (now >= expiration.deadline()) {
        expired = map.remove(key);
        expirations.remove(key);
        expiration.cancel();
      } else {
        if (value != null && touch && getTimeToIdleSeconds() != null) {
          expiration.idleDeadline = deadline(now, getTimeToIdleSeconds());
        }
        return value;
      }
    }
    notifyRemoval(key, expired, RemovalCause.EXPIRED);
    return null;
  }

  private void purgeExpired() {
    for (String key : expirations.keySet()) {
      read(key, false);
    }
  }

  @Override
  public Iterable<String> keys() {
    return keysCollection();
  }

  @Override
  public Collection<String> keysCollection() {
    purgeExpired();
    return map.keySet();
  }

  @Override
  public void put(String key, Serializable value) {
    put(key, value, getTimeToLiveSeconds());
  }

  private synchronized void put(String key, Serializable value, Long ttl) {
    map.put(key, value);
    long now = clock.getAsLong();
    Expiration expiration = new Expiration(deadline(now, ttl), deadline(now, getTimeToIdleSeconds()));
    Expiration old = expirations.put(key, expiration);
    if (old != null) {
      old.cancel();
    }
    schedule(key, expiration, now);
  }

  private static long deadline(long now, Long seconds) {
    if (seconds == null) {
      return Long.MAX_VALUE;
    }
    long duration = TimeUnit.SECONDS.toMillis(seconds);
    if (duration <= 0) {
      return now;
    }
    return duration >= Long.MAX_VALUE - now ? Long.MAX_VALUE : now + duration;
  }

  @Override
  public void remove(String key) {
    Serializable value;
    synchronized (this) {
      value = map.remove(key);
      Expiration expiration = expirations.remove(key);
      if (expiration != null) {
        expiration.cancel();
      }
    }
    notifyRemoval(key, value, RemovalCause.EXPLICIT);
  }

  @Override
  public void putTemporary(String key, Serializable value) {
    put(key, value, (long) MAX_EXPIRE_IN_LOCAL);
  }

  @Override
  public long ttl(String key) {
    read(key, false);
    synchronized (this) {
      Expiration expiration = expirations.get(key);
      if (expiration == null || expiration.deadline() == Long.MAX_VALUE) {
        return -1;
      }
      return Math.max(0, expiration.deadline() - clock.getAsLong());
    }
  }

  // Accesses move only the idle deadline, not the TTL. Called under the cache lock.
  private void schedule(String key, Expiration expiration, long now) {
    if (expiration.deadline() != Long.MAX_VALUE) {
      expiration.task = SCHEDULER.schedule(() -> expire(key, expiration),
          Math.max(0, expiration.deadline() - now), TimeUnit.MILLISECONDS);
    }
  }

  private void expire(String key, Expiration expiration) {
    Serializable value;
    synchronized (this) {
      if (expirations.get(key) != expiration) {
        return;
      }
      long now = clock.getAsLong();
      if (now < expiration.deadline()) {
        schedule(key, expiration, now);
        return;
      }
      value = map.remove(key);
      expirations.remove(key);
    }
    notifyRemoval(key, value, RemovalCause.EXPIRED);
  }

  private void notifyRemoval(String key, Serializable value, RemovalCause cause) {
    if (removalListener != null && value != null) {
      removalListener.onCacheRemoval(key, value, cause);
    }
  }

  @Override
  public Map<String, Serializable> asMap() {
    purgeExpired();
    return map;
  }

  @Override
  public long size() {
    purgeExpired();
    return map.size();
  }
}
