package com.cache.distribution;

/**
 * Simple djb2-style hash function for arbitrary keys.
 * Works with any object by delegating to {@link Object#toString()}.
 * Returns a non-negative integer safe for modulo distribution.
 */
public final class HashUtil {

    private HashUtil() {}

    /**
     * Compute a stable, non-negative hash for {@code key}.
     * Uses the same shift-and-add algorithm as the original implementation
     * so node assignments remain consistent across languages when key types
     * are equivalent strings.
     *
     * @param key the key to hash; {@code toString()} is applied if not a String
     * @return non-negative hash value
     */
    public static int simpleHash(Object key) {
        String str = (key instanceof String s) ? s : key.toString();

        int hash = 0;
        for (int i = 0; i < str.length(); i++) {
            int c = str.charAt(i);
            // Mirroring: hash = (hash << 5) - hash + char  (same as TypeScript version)
            hash = (hash << 5) - hash + c;
            // Keep it 32-bit
            hash = hash & hash;
        }

        return Math.abs(hash);
    }
}
