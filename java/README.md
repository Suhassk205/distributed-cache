# Distributed Cache — Java

A production-grade distributed cache system implemented in Java with pluggable
distribution strategies and eviction policies.

## Overview

This module is a Java port of the original TypeScript implementation. It exposes
the same behaviour, the same four demo scenarios and the same design patterns
while taking full advantage of idiomatic Java (generics, `Optional`, records,
`Supplier`, Java 21 features).

### Key Features

| Feature                   | Implementation                                                |
| ------------------------- | ------------------------------------------------------------- |
| **Distribution Strategy** | Pluggable (modulo hashing provided; consistent hashing ready) |
| **Eviction Policy**       | Pluggable (LRU provided; MRU, LFU ready)                      |
| **Cache Hit Path**        | Local node lookup, no DB call                                 |
| **Cache Miss Path**       | DB fetch → local cache → return                               |
| **Per-Node Eviction**     | Independent LRU tracking per node                             |
| **Type Safety**           | 100 % generic — no raw types, no unsafe casts                 |
| **Dependencies**          | Zero external packages                                        |

---

## Architecture

### Design Patterns Applied

- **Strategy Pattern** — `DistributionStrategy` and `EvictionPolicy` are pluggable
- **Facade Pattern** — `DistributedCache` hides node management complexity
- **Dependency Inversion** — core logic depends on abstractions, not implementations

### Component Overview

```
DistributedCache (Facade)
├── Routes via DistributionStrategy
├── Manages multiple CacheNodes
└── Each CacheNode
    ├── Local storage (HashMap)
    ├── EvictionPolicy tracker
    └── Database interface
```

### Directory Structure

```
java/
├── pom.xml
└── src/main/java/com/cache/
    ├── Main.java                               # Demo + entry point
    ├── interfaces/
    │   ├── Database.java                       # External DB abstraction
    │   ├── EvictionPolicy.java                 # Eviction algorithm contract
    │   └── DistributionStrategy.java           # Routing algorithm contract
    ├── distribution/
    │   ├── HashUtil.java                       # Hash function utility
    │   └── ModuloDistributionStrategy.java     # hash(key) % nodeCount
    ├── eviction/
    │   └── LRUEvictionPolicy.java              # Least-Recently-Used eviction
    └── cache/
        ├── CacheNode.java                      # Single node logic
        └── DistributedCache.java               # Main facade
```

---

## Requirements

- Java 21+
- Maven 3.8+

---

## Build & Run

### Compile

```bash
cd java
mvn compile
```

### Run the Demo

```bash
mvn exec:java -Dexec.mainClass=com.cache.Main
```

Or build a runnable jar first:

```bash
mvn package
java -jar target/distributed-cache-1.0.0.jar
```

### Verify Compilation (zero warnings/errors)

```bash
mvn compile -q
```

---

## Demo Scenarios

The `Main` class exercises the same four scenarios as the TypeScript version:

1. **Cache Hit** — `put` then `get`; the second call never touches the DB
2. **Cache Miss** — `get` a key seeded directly in the DB; the node fetches,
   caches and returns it
3. **Distribution** — five keys routed across three nodes via modulo hashing
4. **LRU Eviction** — fill a node to capacity, insert one more; the LRU item
   is dropped and can only be recovered from the DB

---

## Extension Guide

### Custom Distribution Strategy

```java
public class ConsistentHashStrategy<K> implements DistributionStrategy<K> {
    @Override
    public int getNodeIndex(K key, int nodeCount) {
        // consistent hashing implementation
    }
}

// Plug it in — nothing else changes
DistributedCache<String, String> cache = new DistributedCache<>(
    3, 100, db,
    new ConsistentHashStrategy<>(),
    () -> new LRUEvictionPolicy<>(100)
);
```

### Custom Eviction Policy

```java
public class LFUEvictionPolicy<K, V> implements EvictionPolicy<K, V> {
    // implement recordSet, recordAccess, evict, has, remove
}

// Plug it in — nothing else changes
DistributedCache<String, String> cache = new DistributedCache<>(
    3, 100, db,
    new ModuloDistributionStrategy<>(),
    () -> new LFUEvictionPolicy<>(100)   // ← swap here
);
```

---

## Design Principles

| Principle                       | Application                                                                          |
| ------------------------------- | ------------------------------------------------------------------------------------ |
| **S** — Single Responsibility   | Each class owns one concern: distribution, eviction, routing, or node logic          |
| **O** — Open/Closed             | New strategies extend without modifying existing classes                             |
| **L** — Liskov Substitution     | All strategies are interchangeable via their interfaces                              |
| **I** — Interface Segregation   | Interfaces are minimal and focused                                                   |
| **D** — Dependency Inversion    | Cache depends on abstractions, not concrete classes                                  |
| **DRY**                         | No duplicated distribution or eviction logic                                         |
| **KISS**                        | Modulo hashing, per-node LRU, no unnecessary complexity                              |
| **YAGNI**                       | Only what the spec requires — no speculative generality                              |

---

## Performance Characteristics

| Operation         | Time Complexity                    | Space Complexity     |
| ----------------- | ---------------------------------- | -------------------- |
| `get(key)`        | O(1) lookup + O(n) LRU update      | O(nodeCount) nodes   |
| `put(key, value)` | O(1) storage + O(n) LRU update     | O(capacity) per node |
| Distribution      | O(k) hash (k = key length)         | O(1)                 |
| Eviction          | O(n) LRU scan (n = capacity)       | O(capacity)          |

*A production-grade LRU would use a `LinkedHashMap` or doubly-linked list +
HashMap to achieve O(1) eviction; the current design matches the TypeScript
original for clarity.*

---

## Future Enhancements

- 🔄 **Consistent Hashing** — replace `ModuloDistributionStrategy`
- 🔄 **LFU / MRU Eviction** — implement `EvictionPolicy`, plug in directly
- 🔄 **TTL Support** — add timestamp tracking to `CacheNode`
- 🔄 **Metrics / Observability** — wrap nodes with an observer
- 🔄 **Network Replication** — async replication between remote nodes
