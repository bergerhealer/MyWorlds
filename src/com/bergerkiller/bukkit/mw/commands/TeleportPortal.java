package com.bergerkiller.bukkit.mw.commands;

import java.util.HashSet;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.bergerkiller.bukkit.mw.Localization;
import com.bergerkiller.bukkit.mw.Permission;
import com.bergerkiller.bukkit.mw.Portal;
import com.bergerkiller.bukkit.mw.WorldManager;

public class TeleportPortal extends Command {

	public TeleportPortal(CommandSender sender, String[] args) {
		super(sender, args);
		this.node = "tpp";
	}
	
	public void execute() {
		if (args.length >= 1) {
			Player[] targets = null;
			String dest = null;
			if (args.length > 1) {
				HashSet<Player> found = new HashSet<Player>();
				for (int i = 0; i < args.length - 1; i++) {
					Player player = Bukkit.getServer().getPlayer(args[i]);
					if (player == null) {
						message(ChatColor.RED + "Player '" + args[i] + "' has not been found!");
					} else {
						found.add(player);
					}
				}
				targets = found.toArray(new Player[0]);
				dest = args[args.length - 1];
			} else if (sender instanceof Player) {
				targets = new Player[] {(Player) sender};
				dest = args[0];
			} else {
				sender.sendMessage("This command is only for players!");
				return;
			}
			if (targets.length > 0) {
				//Get prefered world
				World world = targets[0].getWorld();
				if (player != null) world = player.getWorld();
				//Get portal
				Location tele = Portal.getPortalLocation(dest, world.getName(), true);
				if (tele != null && Portal.get(tele, 3) != null) {
					//Perform portal teleports
					int succcount = 0;
					for (Player target : targets) {
						if (Permission.canTeleportPortal(target, dest)) {
							if (Permission.handleTeleport(target, dest, tele)) {
								//Success
								succcount++;
							}
						} else {
							Localization.message(target, "portal.noaccess");
						}
					}
					if (targets.length > 1 || targets[0] != sender) {
						message(ChatColor.YELLOW.toString() + succcount + "/" + targets.length + 
								" Players have been teleported to portal '" + dest + "'!");
					}
				} else {
					//Match world
					String worldname = WorldManager.matchWorld(dest);
					if (worldname != null) {
						World w = WorldManager.getWorld(worldname);
						if (w != null) {
							//Perform world teleports
							int succcount = 0;
							for (Player target : targets) {
								if (Permission.canTeleportWorld(target, w.getName())) {
									if (Permission.handleTeleport(target, w.getSpawnLocation())) {
										//Success
										succcount++;
									}
								} else {
									Localization.message(target, "world.noaccess");
								}
							}
							if (targets.length > 1 || targets[0] != sender) {
								message(ChatColor.YELLOW.toString() + succcount + "/" + targets.length + 
										" Players have been teleported to world '" + w.getName() + "'!");
							}
						} else {
							message(ChatColor.YELLOW + "World '" + worldname + "' is not loaded!");
						}
					} else {
						Localization.message(sender, "portal.notfound");
						listPortals(Portal.getPortals());
					}
				}
			}
		} else {
			showInv(node);
		}	
	}
	
}
