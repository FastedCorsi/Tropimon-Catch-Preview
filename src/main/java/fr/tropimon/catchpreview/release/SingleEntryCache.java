package fr.tropimon.catchpreview.release;

import java.util.Objects;

/** Single-entry cache, including negative results. Client-thread confined. */
final class SingleEntryCache<K, V> {
    private boolean valid;
    private K key;
    private V value;
    boolean contains(K candidate) { return valid && Objects.equals(key, candidate); }
    V value() { return value; }
    void put(K key, V value) { this.key = key; this.value = value; valid = true; }
    void invalidate() { valid = false; key = null; value = null; }
}
