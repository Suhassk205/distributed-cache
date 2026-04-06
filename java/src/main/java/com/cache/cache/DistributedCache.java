package com.cache.cache;

import com.cache.interfaces.Database;
import com.cache.interfaces.DistributionStrategy;
import com.cache.interfaces.EvictionPolicy;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * DistributedCache — the main facade for the distributed cache system.
 *
 * Manages a fixed set of {@link CacheNode} instances and routes every
 * {@code get}/{@code put} request to the correct node using a pluggable
 * {@link DistributionStrategy}.
 *
 * <p>Pattern: Facade — hides node management, eviction policy creation and
 * request routing behind a single, clean API.
 *
 * <p>Responsibility: coordinate cache nodes and route requests.
 * <p>Dependency Inversion: depends on the {@link Database},
 * {@link DistributionStrategy} and {@link EvictionPolicy} abstractions.
 *
 * @param <K> key type
 * @param <V> value type
 */
public class DistributedCache<K, V> {

    private final List<CacheNode<K, V>> nodes;
    private final DistributionStrategy<K> distributionStrategy;
    private final int nodeCount;

    /**
     * @param numberOfNodes         how many cache nodes to create
     * @param nodeCapacity          maximum entries per node before eviction kicks in
     * @param database              backing store for cache misses / persistence
     * @param distributionStrategy  routing algorithm (e.g. modulo hashing)
     * @param evictionPolicyFactory factory called once per node to produce an
     *                              independent eviction policy instance
     */
    public DistributedCache(
            int numberOfNodes,
            int nodeCapacity,
            Database<K, V> database,
            DistributionStrategy<K> distributionStrategy,
            Supplier<EvictionPolicy<K, V>> evictionPolicyFactory
    ) {
        if (numberOfNodes <= 0) {
            throw new IllegalArgumentException("numberOfNodes must be > 0");
        }
        if (nodeCapacity <= 0) {
            throw new IllegalArgumentException("nodeCapacity must be > 0");
        }

        this.nodeCount = numberOfNodes;
        this.distributionStrategy = distributionStrategy;
        this.nodes = new ArrayList<>(numberOfNodes);

        for (int i = 0; i < numberOfNodes; i++) {
            EvictionPolicy<K, V> policy = evictionPolicyFactory.get();
            nodes.add(new CacheNode<>(database, policy));
        }
    }

    /**
     * Retrieve a value from the cache.
     * Routes the request to the appropriate node via the distribution strategy.
     */
    public Optional<V> get(K key) {
        return nodeFor(key).get(key);
    }

    /**
     * Store a value in the cache.
     * Routes the request to the appropriate node via the distribution strategy.
     */
    public void put(K key, V value) {
        nodeFor(key).put(key, value);
    }

    /**
     * Total number of nodes (useful for debugging and tests).
     */
    public int getNodeCount() {
        return nodeCount;
    }

    /**
     * Direct access to a specific node (useful for debugging and tests).
     *
     * @param index node index in [0, nodeCount)
     * @return the node at that index
     * @throws IndexOutOfBoundsException if index is out of range
     */
    public CacheNode<K, V> getNode(int index) {
        if (index < 0 || index >= nodeCount) {
            throw new IndexOutOfBoundsException("Invalid node index: " + index);
        }
        return nodes.get(index);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private CacheNode<K, V> nodeFor(K key) {
        int index = distributionStrategy.getNodeIndex(key, nodeCount);
        if (index < 0 || index >= nodeCount) {
            throw new IllegalStateException("Distribution strategy returned invalid index: " + index);
        }
        return nodes.get(index);
    }
}
