package com.bergerkiller.bukkit.mw.listeners;

import com.bergerkiller.bukkit.common.Common;
import com.bergerkiller.bukkit.common.utils.CommonUtil;
import com.bergerkiller.bukkit.mw.MWPlayerDataController;
import com.bergerkiller.bukkit.mw.MyWorlds;

import java.util.logging.Level;

/**
 * Stores all the event listeners used by MyWorlds. Put here so that all of the
 * listener classes don't have to be made public.
 */
public class MWListeners {
    private final MyWorlds plugin;
    public final MWListener_Main main;
    public final MWListener_Post post;
    private MWListener_PlayerChat chatListener = null; // Only initialized if used
    private MWPlayerDataController dataController;

    public MWListeners(MyWorlds plugin) {
        this.plugin = plugin;
        this.main = new MWListener_Main(this, plugin);
        this.post = new MWListener_Post(plugin);
    }

    public void enable() {
        plugin.register(main);
        if (Common.evaluateMCVersion(">=", "1.17")) {
            if (MWListener_Paper_AsyncSpawnLocation.AVAILABLE) {
                try {
                    MWListener_Paper_AsyncSpawnLocation listener = new MWListener_Paper_AsyncSpawnLocation();
                    listener.register(plugin);
                } catch (Throwable t) {
                    plugin.getLogger().log(Level.SEVERE, "Failed to listen for Paper async spawn location", t);
                }
            } else {
                plugin.register(new MWListener_Respawn_1_17(plugin));
            }
        } else {
            // Here, the player data controller handles it by applying the world as part of player data
            // On these old versions we can handle the player loading itself, so we can set hasPlayedBefore and such.
        }
        if (Common.hasCapability("Common:SignEditTextEvent")) {
            plugin.register(new MWListener_SignEdit_BKCL(main));
        } else {
            plugin.register(new MWListener_SignEdit_Legacy(main));
        }
        plugin.register(post);
    }

    public void setChatListenerEnabled(boolean enabled) {
        if (enabled && chatListener == null) {
            plugin.register(chatListener = new MWListener_PlayerChat());
        } else if (!enabled && chatListener != null) {
            CommonUtil.unregisterListener(chatListener);
            chatListener = null;
        }
    }
}
