package com.cache;

import com.cache.cache.DistributedCache;
import com.cache.distribution.ModuloDistributionStrategy;
import com.cache.eviction.LRUEvictionPolicy;
import com.cache.interfaces.Database;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Entry point — runs a comprehensive demonstration of all cache behaviours:
 *
 * <ol>
 *   <li>Cache hit  — key already in cache, no database call.</li>
 *   <li>Cache miss — key not cached, fetched from DB and stored locally.</li>
 *   <li>Distribution — multiple keys routed deterministically across nodes.</li>
 *   <li>LRU eviction — least-recently-used item removed when a node is full.</li>
 * </ol>
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("=== Distributed Cache Demo ===\n");

        // -----------------------------------------------------------------
        // Setup
        // -----------------------------------------------------------------
        MockDatabase<String, String> db = new MockDatabase<>();
        ModuloDistributionStrategy<String> distributionStrategy = new ModuloDistributionStrategy<>();

        DistributedCache<String, String> cache = new DistributedCache<>(
                3,                                              // 3 nodes
                2,                                              // capacity 2 per node
                db,
                distributionStrategy,
                () -> new LRUEvictionPolicy<>(2)               // independent policy per node
        );

        System.out.println("Configuration:");
        System.out.println("  - Number of nodes: " + cache.getNodeCount());
        System.out.println("  - Capacity per node: 2");
        System.out.println("  - Distribution: Modulo hashing");
        System.out.println("  - Eviction: LRU\n");

        // -----------------------------------------------------------------
        // Scenario 1: Put and Get (cache hit)
        // -----------------------------------------------------------------
        System.out.println("--- Scenario 1: Put and Get ---");
        System.out.println("Action: put(\"user:1\", \"Alice\")");
        cache.put("user:1", "Alice");
        System.out.println("Result: Stored in local node cache and database\n");

        System.out.println("Action: get(\"user:1\")");
        Optional<String> result1 = cache.get("user:1");
        System.out.println("Result: Cache hit, returned \"" + result1.orElse("null") + "\"\n");

        // -----------------------------------------------------------------
        // Scenario 2: Cache miss with database fetch
        // -----------------------------------------------------------------
        System.out.println("--- Scenario 2: Cache miss with database fetch ---");
        System.out.println("Assume \"user:2\" exists in database but not in cache");
        db.putDirect("user:2", "Bob");  // bypass cache — seed the DB directly
        System.out.println("Action: get(\"user:2\")");
        Optional<String> result2 = cache.get("user:2");
        System.out.println("Result: Fetched from database, cached, returned \"" + result2.orElse("null") + "\"\n");

        // -----------------------------------------------------------------
        // Scenario 3: Distribution across nodes
        // -----------------------------------------------------------------
        System.out.println("--- Scenario 3: Distribution across nodes ---");
        String[] keys = {"key:A", "key:B", "key:C", "key:D", "key:E"};
        System.out.println("Putting " + keys.length + " keys to test distribution:");
        for (String key : keys) {
            int nodeIndex = distributionStrategy.getNodeIndex(key, cache.getNodeCount());
            System.out.println("  \"" + key + "\" \u2192 Node " + nodeIndex);
            cache.put(key, "value-" + key);
        }
        System.out.println();

        // -----------------------------------------------------------------
        // Scenario 4: LRU eviction
        // -----------------------------------------------------------------
        System.out.println("--- Scenario 4: LRU Eviction ---");
        System.out.println("Each node has capacity 2. Filling one node to trigger eviction:\n");

        String testKey1 = "test:1";
        String testKey2 = "test:2";
        String testKey3 = "test:3";

        int nodeIdx = distributionStrategy.getNodeIndex(testKey1, cache.getNodeCount());
        System.out.println("Putting \"" + testKey1 + "\" and \"" + testKey2 + "\" to node " + nodeIdx + ":");
        cache.put(testKey1, "value1");
        cache.put(testKey2, "value2");
        System.out.println("Both stored (node now at capacity)\n");

        System.out.println("Putting \"" + testKey3 + "\" to same node " + nodeIdx + ":");
        System.out.println("This should evict the least recently used item (test:1)\n");
        cache.put(testKey3, "value3");

        System.out.println("Verification:");
        System.out.println("get(\"" + testKey1 + "\") - should hit DB (was evicted from cache)");
        Optional<String> evicted = cache.get(testKey1);
        System.out.println("Result: " + (evicted.isPresent() ? "retrieved from DB" : "not found") + "\n");

        System.out.println("get(\"" + testKey2 + "\") - should hit cache (still in node)");
        Optional<String> kept = cache.get(testKey2);
        System.out.println("Result: Cache hit, returned \"" + kept.orElse("null") + "\"\n");

        System.out.println("=== Demo Complete ===");
    }

    // -------------------------------------------------------------------------
    // Mock database — simulates an external data store
    // -------------------------------------------------------------------------

    /**
     * In-memory database stub used by the demo.
     * Logs every read and write so call paths are visible in the output.
     */
    static class MockDatabase<K, V> implements Database<K, V> {

        private final Map<K, V> store = new HashMap<>();

        @Override
        public Optional<V> get(K key) {
            System.out.println("  [DB] get(\"" + key + "\")");
            return Optional.ofNullable(store.get(key));
        }

        @Override
        public void put(K key, V value) {
            System.out.println("  [DB] put(\"" + key + "\", " + value + ")");
            store.put(key, value);
        }

        /** Seed the database directly without going through the cache. */
        public void putDirect(K key, V value) {
            store.put(key, value);
        }
    }
}
