package com.cache.interfaces;

/**
 * Distribution strategy abstraction (Strategy pattern).
 * Pluggable algorithm that decides which cache node is responsible for a key.
 *
 * @param <K> key type
 */
public interface DistributionStrategy<K> {

    /**
     * Determine which node index should store or serve this key.
     *
     * @param key       the key to route
     * @param nodeCount total number of cache nodes
     * @return node index in range [0, nodeCount)
     */
    int getNodeIndex(K key, int nodeCount);
}
