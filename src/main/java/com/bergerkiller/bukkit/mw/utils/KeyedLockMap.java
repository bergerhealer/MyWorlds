package com.bergerkiller.bukkit.mw.utils;

import com.bergerkiller.bukkit.common.Task;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores synchronization objects mapped to keys. Automatically cleans up entries
 * that haven't been used in a while.
 *
 * @param <K> - Key type
 */
public class KeyedLockMap<K> {
    private Task cleanupTask = null;
    private final ConcurrentHashMap<K, Object> lockMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<K, Object> unused = new ConcurrentHashMap<>();

    /**
     * Starts cleaning up keyed locks that haven't been used in a while.
     * This starts a repeating bukkit background task.
     *
     * @param plugin Plugin
     */
    public void startCleanup(JavaPlugin plugin) {
        stopCleanup();

        // This is the amount of time we maximally expect a lock to stay locked
        // It's unlikely for this to be over 10 seconds, so that is what we've chosen
        final long interval = 20*10;
        this.cleanupTask = new Task(plugin) {
            @Override
            public void run() {
                // Clear all current entries in the unused hashmap
                unused.clear();

                // Add all current entries to the unused hashmap
                unused.putAll(lockMap);

                // As a safety thing, now delete all keys from the lock map that exist in the unused one
                // It's possible (asynchronous!) during this time additional entries were added to lockMap
                // It won't happen that entries are removed during this, after all, all entries in the
                // unused map must also exist in the lock map, computeIfAbsent won't fail.
                if (!unused.isEmpty()) {
                    for (K key : new ArrayList<>(unused.keySet())) {
                        lockMap.remove(key);
                    }
                }
            }
        }.start(interval, interval);
    }

    /**
     * Stops doing background cleanup operations
     */
    public void stopCleanup() {
        Task.stop(cleanupTask);
        cleanupTask = null;
    }

    /**
     * Gets or creates a lock object for the key specified.
     * Synchronize on the returned object.
     *
     * @param key Key
     * @return Lock object
     */
    public Object getLock(K key) {
        return lockMap.computeIfAbsent(key, k -> {
            Object recentlyUsed = unused.remove(k);
            return (recentlyUsed != null) ? recentlyUsed : new Object();
        });
    }
}
