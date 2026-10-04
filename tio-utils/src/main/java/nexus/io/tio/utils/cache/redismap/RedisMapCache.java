package nexus.io.tio.utils.cache.redismap;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import nexus.io.tio.utils.cache.AbsCache;
import nexus.io.tio.utils.cache.CacheRemovalListener;
import nexus.io.tio.utils.cache.RemovalCause;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

/**
 * Redis 3.2+ cache with atomic fixed and idle expiration.
 * Uses versioned {@code tio_cache_v2:} keys and hash values. Legacy
 * {@code tio_cache:} string entries are neither read nor deleted; deployments
 * must repopulate this cache and manage any non-expiring legacy keys separately.
 */
public class RedisMapCache extends AbsCache {
  private CacheRemovalListener<String, Serializable> removalListener;
  private final String namespace;
  private static final String KEYSPACE_EXPIRED_CHANNEL = "__keyevent@0__:expired"; // Adjust the DB index if needed
  private static final byte[] VALUE_FIELD = bytes("value");
  // A single hash holds the payload and fixed deadline. Redis time and atomic scripts
  // keep multiple application hosts and concurrent replacements consistent.
  private static final byte[] PUT_SCRIPT = bytes(
      "redis.replicate_commands(); "
      + "local t=redis.call('TIME'); local now=t[1]*1000+math.floor(t[2]/1000); "
      + "local ttl=tonumber(ARGV[2]); local idle=tonumber(ARGV[3]); local deadline=0; "
      + "if ttl>0 then deadline=now+ttl end; "
      + "redis.call('HMSET',KEYS[1],'value',ARGV[1],'deadline',string.format('%.0f',deadline)); "
      + "local expiry=ttl; if idle>0 and (expiry==0 or idle<expiry) then expiry=idle end; "
      + "if expiry>0 then redis.call('PEXPIRE',KEYS[1],expiry) else redis.call('PERSIST',KEYS[1]) end; return 1");
  private static final byte[] GET_SCRIPT = bytes(
      "redis.replicate_commands(); "
      + "local value=redis.call('HGET',KEYS[1],'value'); if not value then return false end; "
      + "local t=redis.call('TIME'); local now=t[1]*1000+math.floor(t[2]/1000); "
      + "local deadline=tonumber(redis.call('HGET',KEYS[1],'deadline')) or 0; "
      + "if deadline>0 and deadline<=now then redis.call('DEL',KEYS[1]); return false end; "
      + "local idle=tonumber(ARGV[1]); if idle>0 then "
      + "if deadline>0 then idle=math.min(idle,deadline-now) end; "
      + "redis.call('PEXPIRE',KEYS[1],idle) end; return value");
  private static final byte[] REMOVE_SCRIPT = bytes(
      "local value=redis.call('HGET',KEYS[1],'value'); redis.call('DEL',KEYS[1]); return value");

  private static byte[] bytes(String value) { return value.getBytes(StandardCharsets.UTF_8); }

  private static long durationMillis(Long seconds) {
    if (seconds == null) return 0;
    // Keep milliseconds exact in Redis Lua's double precision arithmetic.
    if (seconds <= 0 || seconds > 1_000_000_000_000L) {
      throw new IllegalArgumentException("Cache duration must be positive and at most 1000000000000 seconds");
    }
    return seconds * 1000;
  }

  public RedisMapCache(String cacheName, Long timeToLiveSeconds, Long timeToIdleSeconds,
      //
      CacheRemovalListener<String, Serializable> removalListener) {
    super(cacheName, timeToLiveSeconds, timeToIdleSeconds);
    this.removalListener = removalListener;
    durationMillis(timeToLiveSeconds);
    durationMillis(timeToIdleSeconds);
    // Versioned format: old ambiguous string keys are deliberately not read or deleted.
    this.namespace = "tio_cache_v2:" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes(cacheName)) + ":";
    if (removalListener != null) {
      // Start a listener for key expirations
      new Thread(new ExpiredKeyListener()).start();
    }
  }

  private String getRedisKey(String key) {
    return namespace + key;
  }

  @Override
  public void clear() {
    try (Jedis jedis = JedisPoolCan.jedisPool.getResource()) {
      Set<String> keys = jedis.keys(namespace + "*");
      if (!keys.isEmpty()) {
        jedis.del(keys.toArray(new String[0]));
      }
    }
  }

  @Override
  public Serializable _get(String key) {
    try (Jedis jedis = JedisPoolCan.jedisPool.getResource()) {
      String redisKey = getRedisKey(key);
      byte[] data = (byte[]) jedis.eval(GET_SCRIPT, Collections.singletonList(bytes(redisKey)),
          Collections.singletonList(bytes(Long.toString(durationMillis(getTimeToIdleSeconds())))));
      if (data != null) {
        return deserialize(data);
      }
      return null;
    }
  }

  @Override
  public Iterable<String> keys() {
    try (Jedis jedis = JedisPoolCan.jedisPool.getResource()) {
      Set<String> keys = jedis.keys(namespace + "*");
      // Remove namespace prefix
      return keys.stream().map(k -> k.substring(namespace.length())).collect(Collectors.toList());
    }
  }

  @Override
  public Collection<String> keysCollection() {
    try (Jedis jedis = JedisPoolCan.jedisPool.getResource()) {
      Set<String> keys = jedis.keys(namespace + "*");
      // Remove namespace prefix
      return keys.stream().map(k -> k.substring(namespace.length())).collect(Collectors.toList());
    }
  }

  @Override
  public void put(String key, Serializable value) {
    put(key, value, getTimeToLiveSeconds());
  }

  public void put(String key, Serializable value, int ttlSeconds) {
    put(key, value, Long.valueOf(ttlSeconds));
  }

  public void put(String key, Serializable value, Long ttlSeconds) {
    long ttlMillis = durationMillis(ttlSeconds);
    long idleMillis = durationMillis(getTimeToIdleSeconds());
    try (Jedis jedis = JedisPoolCan.jedisPool.getResource()) {
      String redisKey = getRedisKey(key);
      byte[] serializedValue = serialize(value);
      jedis.eval(PUT_SCRIPT, Collections.singletonList(bytes(redisKey)),
          Arrays.asList(serializedValue, bytes(Long.toString(ttlMillis)), bytes(Long.toString(idleMillis))));
    }
  }

  @Override
  public void remove(String key) {
    try (Jedis jedis = JedisPoolCan.jedisPool.getResource()) {
      String redisKey = getRedisKey(key);
      byte[] data = (byte[]) jedis.eval(REMOVE_SCRIPT, Collections.singletonList(bytes(redisKey)), Collections.<byte[]>emptyList());
      if (data != null) {
        Serializable value = deserialize(data);
        if (removalListener != null) {
          removalListener.onCacheRemoval(key, value, RemovalCause.EXPLICIT);
        }
      }
    }
  }

  @Override
  public void putTemporary(String key, Serializable value) {
    // Assuming MAX_EXPIRE_IN_LOCAL is defined in AbsCache as a constant for maximum expiration
    put(key, value, MAX_EXPIRE_IN_LOCAL);
  }

  @Override
  public long ttl(String key) {
    try (Jedis jedis = JedisPoolCan.jedisPool.getResource()) {
      String redisKey = getRedisKey(key);
      Long ttl = jedis.ttl(redisKey);
      return ttl != null && ttl > 0 ? ttl : -1;
    }
  }

  @Override
  public Map<String, Serializable> asMap() {
    // Not efficient for large datasets. Use with caution.
    try (Jedis jedis = JedisPoolCan.jedisPool.getResource()) {
      Set<String> keys = jedis.keys(namespace + "*");
      Map<String, Serializable> map = new HashMap<>();
      for (String redisKey : keys) {
        byte[] data = jedis.hget(bytes(redisKey), VALUE_FIELD);
        if (data != null) {
          Serializable value = deserialize(data);
          String key = redisKey.substring(namespace.length());
          map.put(key, value);
        }
      }
      return map;
    }
  }

  @Override
  public long size() {
    try (Jedis jedis = JedisPoolCan.jedisPool.getResource()) {
      Set<String> keys = jedis.keys(namespace + "*");
      return keys.size();
    }
  }

  // Serialization utility methods
  private byte[] serialize(Serializable obj) {
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(baos);) {
      oos.writeObject(obj);
      return baos.toByteArray();
    } catch (IOException e) {
      throw new RuntimeException("Serialization error", e);
    }
  }

  private Serializable deserialize(byte[] data) {
    try (ByteArrayInputStream bais = new ByteArrayInputStream(data); ObjectInputStream ois = new ObjectInputStream(bais);) {
      return (Serializable) ois.readObject();
    } catch (IOException | ClassNotFoundException e) {
      throw new RuntimeException("Deserialization error", e);
    }
  }

  // Inner class to listen for expired keys
  private class ExpiredKeyListener implements Runnable {
    @Override
    public void run() {
      try (Jedis jedis = JedisPoolCan.jedisPool.getResource()) {
        jedis.psubscribe(new JedisPubSub() {
          @Override
          public void onPMessage(String pattern, String channel, String message) {
            if (message.startsWith(namespace)) {
              String key = message.substring(namespace.length());
              if (removalListener != null) {
                // Since the key has expired, we don't have the value anymore.
                // To get the value before expiration, additional mechanisms are required.
                // Here, we notify with a null value.
                removalListener.onCacheRemoval(key, null, RemovalCause.EXPIRED);
              }
            }
          }
        }, KEYSPACE_EXPIRED_CHANNEL);
      }
    }
  }
}
