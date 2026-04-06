package com.cache.cache;

import com.cache.interfaces.Database;
import com.cache.interfaces.EvictionPolicy;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * CacheNode — a single node in the distributed cache cluster.
 *
 * Each node owns its own in-memory storage and runs an independent eviction
 * policy. It consults the backing database only on a cache miss.
 *
 * <p>Responsibility: manage local storage and eviction for exactly one node.
 * <p>Dependency Inversion: depends on the {@link Database} and
 * {@link EvictionPolicy} abstractions, never on concrete implementations.
 *
 * @param <K> key type
 * @param <V> value type
 */
public class CacheNode<K, V> {

    private final Map<K, V> storage = new HashMap<>();
    private final Database<K, V> database;
    private final EvictionPolicy<K, V> evictionPolicy;

    public CacheNode(Database<K, V> database, EvictionPolicy<K, V> evictionPolicy) {
        this.database = database;
        this.evictionPolicy = evictionPolicy;
    }

    /**
     * Retrieve a value by key.
     *
     * <ul>
     *   <li><b>Cache hit</b> — returns from local storage and updates the
     *       eviction tracker.</li>
     *   <li><b>Cache miss</b> — fetches from the database, stores locally,
     *       then returns the value.</li>
     * </ul>
     */
    public Optional<V> get(K key) {
        if (storage.containsKey(key)) {
            V value = storage.get(key);
            evictionPolicy.recordAccess(key);
            return Optional.ofNullable(value);
        }

        // Cache miss — go to backing store
        Optional<V> dbValue = database.get(key);
        dbValue.ifPresent(v -> putLocal(key, v));
        return dbValue;
    }

    /**
     * Store a value in this node's cache and persist it to the database.
     */
    public void put(K key, V value) {
        putLocal(key, value);
        database.put(key, value);
    }

    /**
     * Return {@code true} when the key is present in local storage
     * (no database fallback).
     */
    public boolean has(K key) {
        return storage.containsKey(key);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void putLocal(K key, V value) {
        storage.put(key, value);
        evictionPolicy.recordSet(key, value);
    }
}
