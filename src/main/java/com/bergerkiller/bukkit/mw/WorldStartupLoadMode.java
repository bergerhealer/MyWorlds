package com.bergerkiller.bukkit.mw;

import com.bergerkiller.bukkit.common.config.ConfigurationNode;

/**
 * The way a world is configured to be loaded. This information is written to configuration
 * and restored after a restart, and then used to figure out what to do.
 */
public enum WorldStartupLoadMode {
    /** The world should not be loaded by MyWorlds, even if it was loaded at shutdown */
    IGNORE,
    /** MyWorlds should load this world at startup. It was loaded at previous shutdown. */
    LOADED,
    /** MyWorlds should not try to load this world at startup. It was not loaded at previous shutdown. */
    NOT_LOADED;

    /**
     * Adjusts this startup load based on whether the world is currently loaded or not.
     * Mode LOADED/NOT_LOADED automatically update when the load is loaded or not.
     *
     * @param isCurrentlyLoaded True if the World is currently loaded
     * @return Adjusted WorldStartupLoadMode
     */
    public WorldStartupLoadMode afterWorldLoadedChanged(boolean isCurrentlyLoaded) {
        switch (this) {
            case LOADED:
            case NOT_LOADED:
                return isCurrentlyLoaded ? LOADED : NOT_LOADED;
            default:
                return this;
        }
    }

    public void writeToConfig(ConfigurationNode config) {
        switch (this) {
            case IGNORE:
                config.set("loaded", "ignore");
                break;
            case LOADED:
                config.set("loaded", true);
                break;
            case NOT_LOADED:
                config.set("loaded", false);
                break;
        }
    }

    public static WorldStartupLoadMode readFromConfig(ConfigurationNode config) {
        String modeStr = config.getOrDefault("loaded", String.class, "");
        if ("ignore".equalsIgnoreCase(modeStr)) {
            return IGNORE;
        } else {
            return config.getOrDefault("loaded", false) ? LOADED : NOT_LOADED;
        }
    }
}
