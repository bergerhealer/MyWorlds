package com.bergerkiller.bukkit.mw.listeners;

import com.bergerkiller.bukkit.common.utils.CommonUtil;
import com.bergerkiller.bukkit.common.utils.LogicUtil;
import com.bergerkiller.bukkit.mw.MyWorlds;
import com.bergerkiller.bukkit.mw.WorldConfig;
import com.bergerkiller.mountiplex.reflection.util.FastMethod;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;

/**
 * Used on Paper 1.21.9+-ish to handle the initial spawn location for players.
 * This one is async, avoiding a warning about the sync event.
 */
public class MWListener_Paper_AsyncSpawnLocation implements Listener, EventExecutor {
    private static final Class<? extends Event> ASYNC_SPAWN_LOCATION_TYPE = LogicUtil.unsafeCast(CommonUtil.getClass(
            "io.papermc.paper.event.player.AsyncPlayerSpawnLocationEvent"));
    public static boolean AVAILABLE = ASYNC_SPAWN_LOCATION_TYPE != null;
    private final FastMethod<Void> setSpawnLocationMethod = new FastMethod<>();
    private final FastMethod<Boolean> isNewPlayerMethod = new FastMethod<>();

    public MWListener_Paper_AsyncSpawnLocation() throws Throwable {
        // public void setSpawnLocation(final Location location)
        setSpawnLocationMethod.init(ASYNC_SPAWN_LOCATION_TYPE.getMethod("setSpawnLocation", Location.class));
        setSpawnLocationMethod.forceInitialization();
        // public boolean isNewPlayer()
        isNewPlayerMethod.init(ASYNC_SPAWN_LOCATION_TYPE.getMethod("isNewPlayer"));
        isNewPlayerMethod.forceInitialization();
    }

    public void register(MyWorlds plugin) {
        Bukkit.getPluginManager().registerEvent(ASYNC_SPAWN_LOCATION_TYPE, this, EventPriority.LOWEST, this, plugin);
    }

    @Override
    public void execute(Listener listener, Event event) throws EventException {
        if (isNewPlayerMethod.invoke(event)) {
            setSpawnLocationMethod.invoke(event, WorldConfig.getMain().getSpawnLocation());
        }
    }
}
