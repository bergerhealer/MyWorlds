package com.bergerkiller.bukkit.mw.commands;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.bergerkiller.bukkit.common.MessageBuilder;
import com.bergerkiller.bukkit.common.wrappers.ChatText;
import com.bergerkiller.bukkit.mw.Localization;
import com.bergerkiller.bukkit.mw.MWPlayerDataController;
import com.bergerkiller.bukkit.mw.Permission;
import com.bergerkiller.bukkit.mw.Position;
import com.bergerkiller.bukkit.mw.Util;
import com.bergerkiller.bukkit.mw.WorldConfig;
import com.bergerkiller.bukkit.mw.WorldConfigStore;
import com.bergerkiller.bukkit.mw.WorldManager;
import com.bergerkiller.bukkit.mw.playerdata.LastPlayerPositionList;

/**
 * Lists the last position the sending player, or player specified, has on all worlds known
 * to MyWorlds. Includes a quick clickable link to teleport to that last position.
 * Also sets up the rejoin world group configurations, which is used by /world rejoin
 * and rejoin portals.
 */
public class WorldLastPosition extends Command {
    private static final ZoneId UTC_ZONE = ZoneId.of("UTC");
    private static final DateTimeFormatter DATE_LONG_FORMAT = DateTimeFormatter.ofPattern("d MMM uuuu HH:mm:ss", Locale.ENGLISH);

    public WorldLastPosition() {
        super(Permission.COMMAND_LAST_POSITION, "world.lastposition");
    }

    @Override
    public void execute() {
        if (args.length > 0) {
            String lastPosCmd = this.removeArg(0);
            boolean isMerge = false;
            if (lastPosCmd.equalsIgnoreCase("list")) {
                executeList();
                return;
            } else if (lastPosCmd.equalsIgnoreCase("tp") || lastPosCmd.equalsIgnoreCase("teleport")) {
                executeTeleport();
                return;
            } else if (lastPosCmd.equalsIgnoreCase("split") || (isMerge = lastPosCmd.equalsIgnoreCase("merge"))) {
                if (isMerge && args.length == 0) {
                    sender.sendMessage(ChatColor.RED + "Specify at least two worlds to merge, or one world to show merge details about");
                    return;
                } else if (!isMerge && args.length == 0) {
                    sender.sendMessage(ChatColor.RED + "Specify at least one world to split off");
                    return;
                }
                List<WorldConfig> worlds = new ArrayList<>(args.length);
                for (String worldName : args) {
                    WorldConfig wc = WorldConfig.getIfExists(worldName);
                    if (wc == null || !wc.isLoaded()) {
                        Localization.WORLD_NOTLOADED.message(sender, worldName);
                        return;
                    }
                    worlds.add(wc);
                }
                if (isMerge) {
                    executeMerge(worlds.get(0), worlds.subList(1, worlds.size()));
                } else {
                    executeSplit(worlds);
                }
                return;
            }
        }

        executeList();
        sender.sendMessage("");
        sender.sendMessage(ChatColor.RED + "Usage:");
        sender.sendMessage(ChatColor.RED + "  /" + commandRootLabel + " lastposition list [player]");
        sender.sendMessage(ChatColor.RED + "  /" + commandRootLabel + " lastposition tp <worldname> [ofplayer]");
        sender.sendMessage(ChatColor.RED + "Merging and splitting last-position rejoin groups:");
        sender.sendMessage(ChatColor.RED + "  /" + commandRootLabel + " lastposition split <world> [world2...]");
        sender.sendMessage(ChatColor.RED + "  /" + commandRootLabel + " lastposition merge <world1> <w2> [w3...]");
        sender.sendMessage(ChatColor.RED + "(This is used for /world rejoin and rejoin portals)");
    }

    @Override
    public List<String> autocomplete() {
        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("list")) {
                return processPlayerNameAutocomplete();
            } else if (args[0].equalsIgnoreCase("tp") || args[0].equalsIgnoreCase("teleport")) {
                if (args.length <= 2) {
                    return processWorldNameAutocomplete();
                } else {
                    return processPlayerNameAutocomplete();
                }
            } else if (args[0].equalsIgnoreCase("split") || args[0].equalsIgnoreCase("merge")) {
                return processWorldNameAutocomplete();
            } else if (args.length > 1) {
                return Collections.emptyList();
            }
        }
        return processBasicAutocomplete("list", "tp", "split", "merge");
    }

    private void executeList() {
        Player player;
        if (args.length > 0) {
            player = Util.parsePlayerName(sender, args[0]);
            if (player == null) {
                return;
            }
        } else if (sender instanceof Player) {
            player = (Player) sender;
        } else {
            sender.sendMessage(ChatColor.RED + "Please specify player name");
            return;
        }

        sender.sendMessage(ChatColor.YELLOW + "Last positions of player " +
                ChatColor.WHITE + player.getName() + ChatColor.YELLOW + ":");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime yesterday = now.minusDays(1);
        for (LastPlayerPositionList.LastPosition lastPos : MWPlayerDataController.readLastPlayerPositions(player).all(false)) {
            Position pos = lastPos.getPosition();
            if (pos == null || pos.getWorld() == player.getWorld()) {
                continue;
            }

            ChatText message = ChatText.fromMessage(ChatColor.WHITE + "- ");
            message.append(formatDate(now, yesterday, lastPos.getTime()));
            message.append(ChatColor.WHITE + " | ");
            message.append(formatPosition(pos, player));
            if (lastPos.hasDied()) {
                message.append(ChatColor.WHITE + " | ");
                message.append(ChatColor.RED + "Died");
            }
            message.sendTo(sender);
        }

        Location loc = player.getLocation();
        ChatText finalText = ChatText.fromMessage(ChatColor.WHITE + "- " +
                ChatColor.BLUE + "Now" + ChatColor.WHITE + " | ");
        finalText.append(formatPosition(new Position(loc), player));
        if (player.isDead()) {
            finalText.append(ChatColor.WHITE + " | ");
            finalText.append(ChatColor.RED + "Died");
        }
        finalText.sendTo(sender);
    }

    private void executeTeleport() {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command is only for players!");
            return;
        }
        if (args.length == 0) {
            showInv();
            return;
        }

        this.worldname = WorldManager.matchWorld(this.removeArg(0));
        if (!this.handleWorld()) {
            return;
        }
        World world = WorldManager.getWorld(worldname);
        if (world == null) {
            Localization.WORLD_NOTLOADED.message(sender, worldname);
            return;
        }

        // If a player name is specified, use that player's last position on that world instead
        // of the sender
        Player lastPosPlayer = player;
        if (args.length > 0 && (lastPosPlayer = Util.parsePlayerName(this.sender, args[0])) == null) {
            return;
        }

        // Skip if target and sender are the same and already on this world
        if (lastPosPlayer == player && lastPosPlayer.getWorld() == world) {
            Localization.COMMAND_LASTPOSITION_TP_SAME.message(sender, lastPosPlayer.getName(), world.getName());
            return;
        }

        Location lastLoc;
        if (lastPosPlayer.getWorld() == world) {
            lastLoc = lastPosPlayer.getLocation();
        } else {
            lastLoc = MWPlayerDataController.readLastLocation(lastPosPlayer, world);
        }
        if (lastLoc == null) {
            Localization.COMMAND_LASTPOSITION_TP_NEVERVISITED.message(sender, lastPosPlayer.getName(), world.getName());
            return;
        }

        if (WorldManager.teleportToExact(player, lastLoc)) {
            Localization.COMMAND_LASTPOSITION_TP_SUCCESS.message(sender, lastPosPlayer.getName(), world.getName());
        }
    }

    private void executeSplit(List<WorldConfig> worlds) {
        for (WorldConfig world : worlds) {
            WorldConfig rejoinWorld = WorldConfigStore.findRejoin(world);
            if (rejoinWorld.rejoinGroup.isEmpty()) {
                continue;
            }

            List<String> tmp = new ArrayList<>(rejoinWorld.rejoinGroup);
            if (rejoinWorld == world) {
                // Migrate rejoin group to the first world in the list we can use
                for (WorldConfig alt : rejoinWorld.getRejoinGroupWorldConfigs()) {
                    if (alt != rejoinWorld) {
                        tmp.remove(alt.worldname);
                        alt.rejoinGroup = Collections.unmodifiableList(tmp);
                        break;
                    }
                }

                // Clear rejoin group of target world
                rejoinWorld.rejoinGroup = Collections.emptyList();
            } else {
                // Remove from list
                tmp.remove(world.worldname);
                rejoinWorld.rejoinGroup = Collections.unmodifiableList(tmp);
            }
        }

        sender.sendMessage(ChatColor.YELLOW + "Worlds have been removed from any last-position rejoin groups");
    }

    private void executeMerge(WorldConfig first, List<WorldConfig> withWorlds) {
        // Resolve the first element into an existing group, if any
        first = WorldConfig.findRejoin(first);

        // Merge the 'with' entries into first. Also resolve rejoin groups of these,
        // and split them if different
        for (WorldConfig withInp : withWorlds) {
            WorldConfig with = WorldConfig.findRejoin(withInp);
            if (with == first) {
                continue; // Already merged
            }

            // Merge it in
            {
                List<String> newRejoinGroup = new ArrayList<>(first.rejoinGroup);
                newRejoinGroup.add(with.worldname);
                newRejoinGroup.addAll(with.rejoinGroup);
                first.rejoinGroup = Collections.unmodifiableList(newRejoinGroup);
            }

            // Clear previous group, is now merged into the other one
            with.rejoinGroup = Collections.emptyList();
        }

        MessageBuilder builder = new MessageBuilder();
        builder.newLine().yellow("These worlds are now part of a last-position rejoin group:");
        builder.newLine().yellow("Main world: ").green(first.worldname);
        if (first.rejoinGroup.isEmpty()) {
            builder.newLine().red("There are no secondary worlds");
        } else {
            builder.newLine().yellow("Secondary worlds: ");
            builder.setSeparator(ChatColor.WHITE, " / ").setIndent(2).newLine();
            for (String worldName : first.rejoinGroup) {
                builder.green(worldName);
            }
        }
        builder.send(sender);
    }

    private ChatText formatPosition(Position pos, Player player) {
        boolean loaded = pos.isWorldLoaded();

        String worldStr = (loaded ? ChatColor.GREEN : ChatColor.RED).toString() +
                pos.getWorldName() + " ";
        ChatText msg = ChatText.fromMessage(worldStr + ChatColor.WHITE.toString() +
                pos.getBlockX() + "/" + pos.getBlockY() + "/" + pos.getBlockZ());
        if (pos.isWorldLoaded()) {
            String playerPart = (args.length == 0) ? "" : (" " + player.getName());
            msg.setClickableRunCommand("/" + this.commandRootLabel + " lastposition tp " + pos.getWorldName() + playerPart);
            msg.setHoverText(ChatColor.WHITE + "Rejoin " + pos.getWorldName());
        }
        return msg;
    }

    private static ChatText formatDate(LocalDateTime now, LocalDateTime yesterday, long timeMillis) {
        if (timeMillis == 0) {
            return ChatText.fromMessage(ChatColor.RED + "Unknown");
        }

        // Decode UTC millis since epoch. Then adjust for system timezone.
        // Users can change the server default timezone using environment flags
        LocalDateTime date = LocalDateTime.ofInstant(Instant.ofEpochMilli(timeMillis), UTC_ZONE)
                .atZone(UTC_ZONE)
                .withZoneSameInstant(ZoneId.systemDefault())
                .toLocalDateTime();

        ChatText msg = formatDateBase(now, yesterday, date);
        msg.setHoverText(ChatColor.GREEN + date.format(DATE_LONG_FORMAT));
        return msg;
    }

    private static ChatText formatDateBase(LocalDateTime now, LocalDateTime yesterday, LocalDateTime date) {
        if (date.isBefore(now)) {
            Duration timeAgo = Duration.between(date, now);

            // Try to format as x minutes/seconds ago for < one hour
            long timeAgoMinutes = timeAgo.toMinutes();
            if (timeAgoMinutes < 60) {
                long totalSeconds = timeAgo.toMillis() / 1000;
                if (totalSeconds < 60) {
                    return ChatText.fromMessage(ChatColor.GREEN + Long.toString(totalSeconds) + " sec. ago");
                } else {
                    return ChatText.fromMessage(ChatColor.GREEN + Long.toString(timeAgoMinutes) + " min. ago");
                }
            }

            // Show hours:minutes only for today/yesterday
            if (date.toLocalDate().equals(now.toLocalDate())) {
                return ChatText.fromMessage(ChatColor.YELLOW + "Today " + formatTime(date));
            } else if (date.toLocalDate().equals(yesterday.toLocalDate())) {
                return ChatText.fromMessage(ChatColor.GOLD + "Yesterday " + formatTime(date));
            }
        }

        // Show simple date
        String month = date.getMonth().name().substring(0, 3).toLowerCase(Locale.ENGLISH);
        return ChatText.fromMessage(ChatColor.RED.toString() + date.getDayOfMonth() +
                " " + month + " " + date.getYear());
    }

    private static String formatTime(LocalDateTime time) {
        int hour = time.getHour();
        int minute = time.getMinute();

        String hourStr = (hour < 10) ? ("0" + Integer.toString(hour)) : Integer.toString(hour);
        String minuteStr = (minute < 10) ? ("0" + Integer.toString(minute)) : Integer.toString(minute);
        return hourStr + ":" + minuteStr;
    }
}
