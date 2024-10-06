package com.bergerkiller.bukkit.mw.commands;

import com.bergerkiller.bukkit.common.utils.ParseUtil;
import com.bergerkiller.bukkit.mw.Permission;
import com.bergerkiller.bukkit.mw.utils.EventListenerHook;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.PortalCreateEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorldDebug extends Command {
    private static final Map<String, EventDebugger<?>> eventNames = new HashMap<>();

    static {
        registerDetectCancel("portal_create", PortalCreateEvent.class, (listener, callEvent, event) -> {
            Entity entity = event.getEntity();
            String entityName = "[No Entity]";
            if (entity instanceof Player) {
                entityName = "Player " + ((Player) entity).getName();
            } else if (entity != null) {
                entityName = "Entity [" + entity.getType().name() + "]";
            }
            Bukkit.broadcastMessage("[MyWorlds] " + event.getClass().getSimpleName() +
                    " by " + entityName + " reason " + event.getReason().name() +
                    " was cancelled by plugin " + listener.getPlugin().getName());
        });
        registerDetectCancel("player_teleport", PlayerTeleportEvent.class, (listener, callEvent, event) -> {
            Location to = event.getTo();
            String toString = "[none]";
            if (to != null) {
                toString = "[x=" + to.getBlockX() + ", y=" + to.getBlockY() + ", z=" + to.getBlockZ() + "]";
            }
            Bukkit.broadcastMessage("[MyWorlds] " + event.getClass().getSimpleName() +
                    " of " + event.getPlayer().getName() + " to " + toString +
                    " was cancelled by plugin " + listener.getPlugin().getName());
        });
        registerDetectCancel("entity_teleport", EntityTeleportEvent.class, (listener, callEvent, event) -> {
            Entity entity = event.getEntity();
            String entityName = "[No Entity]";
            if (entity instanceof Player) {
                entityName = "Player " + ((Player) entity).getName();
            } else if (entity != null) {
                entityName = "Entity [" + entity.getType().name() + "]";
            }
            Location to = event.getTo();
            String toString = "[none]";
            if (to != null) {
                toString = "[x=" + to.getBlockX() + ", y=" + to.getBlockY() + ", z=" + to.getBlockZ() + "]";
            }
            Bukkit.broadcastMessage("[MyWorlds] " + event.getClass().getSimpleName() +
                    " of " + entityName + " to " + toString +
                    " was cancelled by plugin " + listener.getPlugin().getName());
        });
    }

    private static <T extends Event & Cancellable> void registerDetectCancel(String name, Class<T> eventClass, EventListenerHook.Handler<T> handler) {
        register(name, eventClass, (listener, callEvent, event) -> {
            boolean wasCancelled = event.isCancelled();
            callEvent.accept(event);
            if (!wasCancelled && event.isCancelled()) {
                handler.handle(listener, callEvent, event);
            }
        });
    }

    private static <T extends Event> void register(String name, Class<T> eventClass, EventListenerHook.Handler<T> handler) {
        eventNames.put(name, new EventDebugger<>(eventClass, handler));
    }

    public WorldDebug() {
        super(Permission.DEBUGGING, "world.debug.portalcreate");
    }

    @Override
    public void execute() {
        if (args.length == 0) {
            sender.sendMessage("/mw debug [eventname] - Debug events");
        } else {
            EventDebugger<?> debugger = eventNames.get(args[0]);
            if (debugger == null) {
                sender.sendMessage(ChatColor.RED + "Unknown event: " + args[0]);
            } else {
                String eventName = args[0];
                removeArg(0);
                boolean enabled;
                if (args.length == 0) {
                    enabled = true;
                } else {
                    enabled = ParseUtil.parseBool(args[0], false);
                }
                if (enabled) {
                    sender.sendMessage(ChatColor.YELLOW + "Debugging event " + eventName + ": " + ChatColor.GREEN + "Enabled");
                } else {
                    sender.sendMessage(ChatColor.YELLOW + "Debugging event " + eventName + ": " + ChatColor.RED + "Disabled");
                }
                if (enabled) {
                    debugger.hook();
                    sender.sendMessage(ChatColor.YELLOW + "A message will be broadcast when a " + debugger.eventClass.getSimpleName() +
                            " is cancelled by a plugin");
                    sender.sendMessage(ChatColor.YELLOW + "Use /mw debug event " + eventName + " false to turn off again");
                } else {
                    debugger.unhook();
                }
            }
        }
    }

    @Override
    public List<String> autocomplete() {
        if (args.length == 1) {
            return new ArrayList<>(eventNames.keySet());
        } else if (args.length == 2) {
            return Arrays.asList("yes", "no");
        } else {
            return Collections.emptyList();
        }
    }

    private static class EventDebugger<T extends Event> {
        public final Class<T> eventClass;
        public EventListenerHook.Handler<T> handler;

        public EventDebugger(Class<T> eventClass,  EventListenerHook.Handler<T> handler) {
            this.eventClass = eventClass;
            this.handler = handler;
        }

        public void hook() {
            EventListenerHook.hook(eventClass, handler);
        }

        public void unhook() {
            EventListenerHook.unhook(eventClass);
        }
    }
}
