package com.bergerkiller.bukkit.mw.commands;

import com.bergerkiller.bukkit.common.utils.ParseUtil;
import com.bergerkiller.bukkit.mw.Permission;
import com.bergerkiller.bukkit.mw.utils.EventListenerHook;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.world.PortalCreateEvent;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorldDebug extends Command {
    private static final Map<String, Class<? extends Event>> eventNames = new HashMap<>();
    static {
        eventNames.put("portal_create", PortalCreateEvent.class);
    }

    public WorldDebug() {
        super(Permission.DEBUGGING, "world.debug.portalcreate");
    }

    @Override
    public void execute() {
        if (args.length == 0) {
            sender.sendMessage("/mw debug [eventname] - Debug events");
        } else {
            Class<? extends Event> eventClass = eventNames.get(args[0]);
            if (eventClass == null) {
                sender.sendMessage(ChatColor.RED + "Unknown event: " + args[0]);
            } else {
                executeDebugEvent(args[0], eventClass);
            }
        }
    }

    private void executeDebugEvent(String eventName, Class<? extends Event> eventClass) {
        removeArg(0);
        boolean enabled;
        if (args.length == 0) {
            enabled = true;
        } else {
            enabled = ParseUtil.parseBool(args[0], false);
        }
        sender.sendMessage(ChatColor.RED + eventClass.getSimpleName() + " debug mode: " + enabled);
        if (enabled) {
            EventListenerHook.hook(eventClass, (listener, callEvent, event) -> {
                Cancellable cancellable = (Cancellable) event;
                boolean wasCancelled = cancellable.isCancelled();
                callEvent.accept(event);
                if (
                        !wasCancelled && cancellable.isCancelled()
                ) {
                    Bukkit.broadcastMessage("[MyWorlds] " + eventClass.getSimpleName() +
                            " was cancelled by plugin " + listener.getPlugin().getName());
                }
            });
            sender.sendMessage(ChatColor.YELLOW + "A message will be broadcast when a " + eventClass.getSimpleName() +
                    " is cancelled by a plugin");
            sender.sendMessage(ChatColor.YELLOW + "Use /mw debug event " + eventName + " false to turn off again");
        } else {
            EventListenerHook.unhook(eventClass);
        }
    }

    @Override
    public List<String> autocomplete() {
        if (args.length == 1) {
            return Arrays.asList("portal_create");
        } else if (args.length == 2) {
            return Arrays.asList("yes", "no");
        } else {
            return Collections.emptyList();
        }
    }
}
