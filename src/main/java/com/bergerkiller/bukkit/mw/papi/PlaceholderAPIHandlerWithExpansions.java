package com.bergerkiller.bukkit.mw.papi;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.OfflinePlayer;

import com.bergerkiller.bukkit.common.component.LibraryComponent;
import com.bergerkiller.bukkit.mw.MyWorlds;

import me.clip.placeholderapi.PlaceholderAPIPlugin;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.clip.placeholderapi.expansion.manager.LocalExpansionManager;

/**
 * Uses the PlaceHolderAPI LocalExpansionManager to track what expansions are registered
 */
public class PlaceholderAPIHandlerWithExpansions implements LibraryComponent {
    private final MyWorlds plugin;
    private final MyWorldsPAPIHandlerImpl handler;
    private final List<Hook> hooks;

    public PlaceholderAPIHandlerWithExpansions(MyWorlds plugin) {
        this.plugin = plugin;
        this.handler = new MyWorldsPAPIHandlerImpl();
        this.hooks = new ArrayList<Hook>();
    }

    @Override
    public void enable() {
        // Hook as myworlds, only hook as 'mw' if not already an expansion
        LocalExpansionManager manager = PlaceholderAPIPlugin.getInstance().getLocalExpansionManager();

        Hook mainHook = new Hook(this.plugin, this.handler, "myworlds");
        if (manager.register(mainHook)) {
            hooks.add(mainHook);
        }

        Hook aliasHook = new Hook(this.plugin, this.handler, "mw");
        if (manager.getExpansion("mw") == null && manager.register(aliasHook)) {
            hooks.add(aliasHook);
        }
    }

    @Override
    public void disable() {
        // Unregister hooks
        LocalExpansionManager manager = PlaceholderAPIPlugin.getInstance().getLocalExpansionManager();
        for (Hook hook : hooks) {
            manager.unregister(hook);
        }
    }

    /**
     * This hook makes MyWorlds placeholders available in PAPI
     */
    private static class Hook extends PlaceholderExpansion {
        private final MyWorlds plugin;
        private final String identifier;
        private final MyWorldsPAPIHandler handler;

        public Hook(MyWorlds plugin, MyWorldsPAPIHandler handler, String identifier) {
            this.plugin = plugin;
            this.identifier = identifier;
            this.handler = handler;
        }

        @Override
        public String getIdentifier() {
            return this.identifier;
        }

        @Override
        public String getAuthor() {
            return this.plugin.getDescription().getAuthors().get(0);
        }

        @Override
        public String getVersion() {
            return this.plugin.getDescription().getVersion();
        }
        
        @Override
        public boolean persist() {
            return true;
        }

        @Override
        public List<String> getPlaceholders() {
            return this.handler.getPlaceholderNames();
        }

        @Override
        public String onRequest(final OfflinePlayer player, String identifier) {
            return this.handler.getPlaceholder(player, identifier);
        }
    }
}
