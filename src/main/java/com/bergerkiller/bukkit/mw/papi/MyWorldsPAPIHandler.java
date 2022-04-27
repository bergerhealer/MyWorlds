package com.bergerkiller.bukkit.mw.papi;

import java.util.List;

import org.bukkit.OfflinePlayer;

/**
 * Handles placeholder replacement requests
 */
public interface MyWorldsPAPIHandler {

    List<String> getPlaceholderNames();

    String getPlaceholder(OfflinePlayer player, String identifier);
}
