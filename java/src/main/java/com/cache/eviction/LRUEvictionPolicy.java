package com.cache.eviction;

import com.cache.interfaces.EvictionPolicy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * LRU (Least Recently Used) eviction policy.
 *
 * Tracks access order and evicts the entry that was accessed least recently
 * when the node exceeds capacity.
 *
 * <p>Pattern: Strategy
 * <p>Why: Pluggable eviction lets callers swap in LFU or MRU without touching
 * the node or cache facade.
 *
 * <p>Implementation note:
 * <ul>
 *   <li>A {@link HashMap} gives O(1) key lookup and value storage.</li>
 *   <li>An {@link ArrayList} tracks access order; index 0 is the oldest entry.</li>
 *   <li>Eviction removes the element at index 0 — O(n) in the worst case but
 *       acceptable for typical interview-grade cache sizes.</li>
 * </ul>
 *
 * @param <K> key type
 * @param <V> value type
 */
public class LRUEvictionPolicy<K, V> implements EvictionPolicy<K, V> {

    private final int maxSize;
    private final Map<K, V> storage;
    private final List<K> accessOrder; // index 0 = least recently used

    public LRUEvictionPolicy(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize must be > 0");
        }
        this.maxSize = maxSize;
        this.storage = new HashMap<>();
        this.accessOrder = new ArrayList<>();
    }

    @Override
    public void recordSet(K key, V value) {
        if (storage.containsKey(key)) {
            storage.put(key, value);
            removeFromAccessOrder(key);
        } else {
            storage.put(key, value);
        }

        // Mark as most recently used
        accessOrder.add(key);

        // Evict until within capacity
        while (storage.size() > maxSize) {
            evict();
        }
    }

    @Override
    public void recordAccess(K key) {
        if (!storage.containsKey(key)) {
            return;
        }
        // Promote to most-recently-used position
        removeFromAccessOrder(key);
        accessOrder.add(key);
    }

    @Override
    public Optional<KeyValuePair<K, V>> evict() {
        if (accessOrder.isEmpty()) {
            return Optional.empty();
        }

        K lruKey = accessOrder.remove(0);
        V value = storage.remove(lruKey);

        if (value == null) {
            return Optional.empty();
        }

        return Optional.of(new KeyValuePair<>(lruKey, value));
    }

    @Override
    public boolean has(K key) {
        return storage.containsKey(key);
    }

    @Override
    public void remove(K key) {
        storage.remove(key);
        removeFromAccessOrder(key);
    }

    private void removeFromAccessOrder(K key) {
        accessOrder.remove(key);
    }
}
