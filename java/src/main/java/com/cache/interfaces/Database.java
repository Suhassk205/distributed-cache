package com.cache.interfaces;

import java.util.Optional;

/**
 * Database abstraction — caller provides the concrete implementation.
 * Represents an external data store consulted on cache misses.
 *
 * @param <K> key type
 * @param <V> value type
 */
public interface Database<K, V> {

    /**
     * Fetch a value by key. Returns {@link Optional#empty()} when the key does
     * not exist in the backing store.
     */
    Optional<V> get(K key);

    /**
     * Persist a key-value pair to the backing store.
     */
    void put(K key, V value);
}
