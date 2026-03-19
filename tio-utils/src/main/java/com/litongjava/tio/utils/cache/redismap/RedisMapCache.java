package com.litongjava.tio.utils.cache.redismap;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.litongjava.tio.utils.cache.AbsCache;
import com.litongjava.tio.utils.cache.CacheRemovalListener;
import com.litongjava.tio.utils.cache.RemovalCause;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.params.SetParams;

public class RedisMapCache extends AbsCache {
  private CacheRemovalListener<String, Serializable> removalListener;
  private final String namespace;
  private static final String KEYSPACE_EXPIRED_CHANNEL = "__keyevent@0__:expired"; // Adjust the DB index if needed

  public RedisMapCache(String cacheName, Long timeToLiveSeconds, Long timeToIdleSeconds,
      //
      CacheRemovalListener<String, Serializable> removalListener) {
    super(cacheName, timeToLiveSeconds, timeToIdleSeconds);
    this.removalListener = removalListener;
    this.namespace = "tio_cache:" + cacheName + ":";
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
      byte[] data = jedis.get(redisKey.getBytes());
      if (data != null) {
        Serializable value = deserialize(data);
        if (getTimeToIdleSeconds() != null) {
          // Reset TTL for TTI
          jedis.expire(redisKey, getTimeToIdleSeconds().intValue());
        }
        return value;
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
    try (Jedis jedis = JedisPoolCan.jedisPool.getResource()) {
      String redisKey = getRedisKey(key);
      byte[] serializedValue = serialize(value);
      SetParams params = new SetParams();
      if (ttlSeconds != null) {
        params.ex(ttlSeconds);
      }
      if (getTimeToIdleSeconds() != null) {
        // For TTI, we'll handle it by resetting TTL on access
        // So set the expire to the minimum of TTL and TTI
        if (ttlSeconds != null) {
          params.ex((int) Math.min(ttlSeconds, getTimeToIdleSeconds()));
        } else {
          params.ex(getTimeToIdleSeconds());
        }
      }
      jedis.set(redisKey.getBytes(), serializedValue, params);
    }
  }

  @Override
  public void remove(String key) {
    try (Jedis jedis = JedisPoolCan.jedisPool.getResource()) {
      String redisKey = getRedisKey(key);
      byte[] data = jedis.get(redisKey.getBytes());
      if (data != null) {
        Serializable value = deserialize(data);
        jedis.del(redisKey);
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
        byte[] data = jedis.get(redisKey.getBytes());
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