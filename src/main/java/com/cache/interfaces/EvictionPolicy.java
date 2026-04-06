package com.cache.interfaces;

import java.util.Optional;

/**
 * Eviction policy abstraction (Strategy pattern).
 * Pluggable algorithm that decides what to remove when a node exceeds capacity.
 *
 * @param <K> key type
 * @param <V> value type
 */
public interface EvictionPolicy<K, V> {

    /**
     * Record that a key was freshly set or updated.
     * If the policy tracks insertion order this is where the entry is registered.
     */
    void recordSet(K key, V value);

    /**
     * Record that an existing key was accessed (read).
     * For LRU this moves the key to the most-recently-used position.
     */
    void recordAccess(K key);

    /**
     * Evict one entry according to the policy's ordering rules.
     *
     * @return the evicted key-value pair, or {@link Optional#empty()} if the
     *         tracker is already empty
     */
    Optional<KeyValuePair<K, V>> evict();

    /**
     * Return {@code true} when the key is tracked by this policy.
     */
    boolean has(K key);

    /**
     * Remove a key from the tracker without triggering a normal eviction
     * (e.g. when a caller explicitly deletes an entry).
     */
    void remove(K key);

    /**
     * Simple holder for a key-value pair returned by {@link #evict()}.
     */
    record KeyValuePair<K, V>(K key, V value) {}
}
