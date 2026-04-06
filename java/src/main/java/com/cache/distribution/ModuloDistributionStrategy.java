package com.cache.distribution;

import com.cache.interfaces.DistributionStrategy;

/**
 * Modulo-based distribution strategy.
 *
 * Routes a key to a node using {@code hash(key) % nodeCount}.
 * Simple and deterministic — suitable as a default; swap in consistent hashing
 * later without touching any other class.
 *
 * <p>Pattern: Strategy
 * <p>Why: Pluggable distribution keeps the routing algorithm isolated and
 * independently replaceable.
 *
 * @param <K> key type
 */
public class ModuloDistributionStrategy<K> implements DistributionStrategy<K> {

    @Override
    public int getNodeIndex(K key, int nodeCount) {
        if (nodeCount <= 0) {
            throw new IllegalArgumentException("nodeCount must be > 0");
        }
        return HashUtil.simpleHash(key) % nodeCount;
    }
}
