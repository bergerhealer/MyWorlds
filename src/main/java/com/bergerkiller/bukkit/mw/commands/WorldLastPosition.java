package com.bergerkiller.bukkit.mw.commands;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.bergerkiller.bukkit.common.wrappers.ChatText;
import com.bergerkiller.bukkit.mw.LastPlayerPositionList;
import com.bergerkiller.bukkit.mw.Localization;
import com.bergerkiller.bukkit.mw.MWPlayerDataController;
import com.bergerkiller.bukkit.mw.Permission;
import com.bergerkiller.bukkit.mw.Position;
import com.bergerkiller.bukkit.mw.Util;
import com.bergerkiller.bukkit.mw.WorldManager;

/**
 * Lists the last position the sending player, or player specified, has on all worlds known
 * to MyWorlds. Includes a quick clickable link to teleport to that last position.
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
            if (lastPosCmd.equalsIgnoreCase("list")) {
                executeList();
                return;
            } else if (lastPosCmd.equalsIgnoreCase("tp") || lastPosCmd.equalsIgnoreCase("teleport")) {
                executeTeleport();
                return;
            }
        }

        executeList();
        sender.sendMessage("");
        sender.sendMessage(ChatColor.RED + "Usage: " + commandRootLabel + " lastposition tp <worldname> [ofplayer]");
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
            } else if (args.length > 1) {
                return Collections.emptyList();
            }
        }
        return processBasicAutocomplete("list", "tp");
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
        for (LastPlayerPositionList.LastPosition lastPos : MWPlayerDataController.readLastPlayerPositions(player).all()) {
            Position pos = lastPos.getPosition();
            if (pos == null || pos.getWorld() == player.getWorld()) {
                continue;
            }

            ChatText message = ChatText.fromMessage(ChatColor.WHITE + "- ");
            message.append(formatDate(now, yesterday, lastPos.getTime()));
            message.append(ChatColor.WHITE + " | ");
            message.append(formatPosition(pos, player));
            message.sendTo(sender);
        }

        Location loc = player.getLocation();
        ChatText finalText = ChatText.fromMessage(ChatColor.WHITE + "- " +
                ChatColor.BLUE + "Now" + ChatColor.WHITE + " | ");
        finalText.append(formatPosition(new Position(loc), player));
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
            sender.sendMessage(ChatColor.YELLOW + "You are already in this world");
            return;
        }

        Location lastLoc;
        if (lastPosPlayer.getWorld() == world) {
            lastLoc = lastPosPlayer.getLocation();
        } else {
            lastLoc = MWPlayerDataController.readLastLocation(lastPosPlayer, world);
        }
        if (lastLoc == null) {
            sender.sendMessage(ChatColor.RED + "Player " + lastPosPlayer.getName() + " never visited world " + world.getName() + "!");
            return;
        }

        if (WorldManager.teleportToExact(player, lastLoc)) {
            message(ChatColor.YELLOW.toString() + "Teleporting to last position of player '" +
                    lastPosPlayer.getName() + "' on world '" + world.getName() + "'!");
        }
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
