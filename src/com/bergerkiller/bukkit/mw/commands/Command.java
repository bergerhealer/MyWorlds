package com.bergerkiller.bukkit.mw.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.bergerkiller.bukkit.common.MessageBuilder;
import com.bergerkiller.bukkit.common.utils.CommonUtil;
import com.bergerkiller.bukkit.common.utils.StringUtil;
import com.bergerkiller.bukkit.mw.Localization;
import com.bergerkiller.bukkit.mw.MyWorlds;
import com.bergerkiller.bukkit.mw.Permission;
import com.bergerkiller.bukkit.mw.Portal;
import com.bergerkiller.bukkit.mw.Util;
import com.bergerkiller.bukkit.mw.WorldManager;

public class Command {
		
	public String node;
	public Player player;
	public CommandSender sender;
	public String[] args;
	public String worldname;

	public Command(String node) {
		this.node = node;
	}
	
	public static boolean allowConsole(String node) {
		if (node.equals("world.setspawn")) return false;
		if (node.equals("world.spawn")) return false;
		return true;
	}
		
	public Command(CommandSender sender, String[] args) {
		this.args = args;
		this.sender = sender;
		if (sender instanceof Player) this.player = (Player) sender;
		this.node = null;
	}
	
	public void removeArg(int index) {
		String[] newargs = new String[args.length - 1];
		int ni = 0;
		for (int i = 0; i < args.length; i++) {
			if (i == index) continue;
			newargs[ni] = args[i];
			ni++;
		}
		this.args = newargs;
	}
	
	public boolean hasPermission() {
		return this.hasPermission(this.node);
	}
	public boolean hasPermission(String node) {
		if (this.player == null) {
			return allowConsole(node);
		} else {
			return Permission.has(this.player, node);
		}
	}
	
	public boolean handleWorld() {
		if (this.worldname == null) {
			locmessage("world.notfound");
		}
		return this.worldname != null;
	}
	
	public void messageNoSpout() {
		if (MyWorlds.isSpoutEnabled) return;
		this.message(ChatColor.YELLOW + "Note that Spout is not installed right now!");
	}
	public void message(String msg) {
		if (msg == null) return;
		CommonUtil.sendMessage(this.sender, msg);
	}
	public void locmessage(String node) {
		Localization.message(this.sender, node);
	}
	public void locmessage(String node, String def) {
		Localization.message(this.sender, node, def);
	}
	
	public void notifyConsole(String message) {
		Util.notifyConsole(sender, message);
	}
	
	public void showInv() {
		this.showInv(this.node);
	}
	public boolean showInv(String node) {
		message(ChatColor.RED + "Invalid arguments for this command!");
		return showUsage(node);
	}
	public void showUsage() {
		this.showUsage(this.node);
	}
	public boolean showUsage(String node) {
		if (hasPermission(node)) {
			locmessage("help." + node, "");
			return true;
		} else {
			return false;
		}
	}
	
	public void listPortals(String[] portals) {
		MessageBuilder builder = new MessageBuilder();
		builder.green("[Very near] ").dark_green("[Near] ").yellow("[Far] ");
		builder.red("[Other world] ").dark_red("[Unavailable]").newLine();
		builder.yellow("Available portals: ").white(portals.length, " Portal");
		if (portals.length != 1) builder.append('s');
		if (portals.length > 0) {
			builder.newLine().setIndent(2).setSeparator(ChatColor.WHITE, " / ");
			final Location ploc;
			if (sender instanceof Player) {
				ploc = ((Player) sender).getLocation();
			} else {
				ploc = null;
			}
			for (String portal : portals) {
				Location loc = Portal.getPortalLocation(portal, null);
				if (loc != null && ploc != null) {
					if (ploc.getWorld() == loc.getWorld()) {
						double d = ploc.distance(loc);
						if (d <= 10) {
							builder.green(portal);
						} else if (d <= 100) {
							builder.dark_green(portal);
						} else {
							builder.yellow(portal);
						}
					} else {
						builder.red(portal);
					}
				} else {
					builder.dark_red(portal);
				}
			}
		}
		builder.send(sender);
	}
	
	public void genWorldname(int argindex) {
		if (argindex >= 0 && argindex < this.args.length) {
			this.worldname = WorldManager.matchWorld(args[argindex]);
			if (this.worldname != null) return;
		}
		if (player != null) {
			this.worldname = player.getWorld().getName();
		} else {
			this.worldname = Bukkit.getServer().getWorlds().get(0).getName();
		}
	}
			
	public static void execute(CommandSender sender, String cmdLabel, String[] args) {
		//generate a node from this command
		Command rval = null;
		if (cmdLabel.equalsIgnoreCase("world")
				|| cmdLabel.equalsIgnoreCase("myworlds")
				|| cmdLabel.equalsIgnoreCase("worlds")
				|| cmdLabel.equalsIgnoreCase("mw")) {
			if (args.length >= 1) {
				cmdLabel = args[0];
				args = StringUtil.remove(args, 0);
				if (cmdLabel.equalsIgnoreCase("list")) {
					rval = new WorldList(sender, args);
				} else if (cmdLabel.equalsIgnoreCase("info")) {
					rval = new WorldInfo(sender, args);
				} else if (cmdLabel.equalsIgnoreCase("i")) {
					rval = new WorldInfo(sender, args);
				} else if (cmdLabel.equalsIgnoreCase("portals")) {
					rval = new WorldPortals(sender, args);
				} else if (cmdLabel.equalsIgnoreCase("portal")) {
					rval = new WorldPortals(sender, args);
				} else if (cmdLabel.equalsIgnoreCase("load")) {
					rval = new WorldLoad(sender, args);
				} else if (cmdLabel.equalsIgnoreCase("unload")) {
					rval = new WorldUnload(sender, args);
				} else if (cmdLabel.equalsIgnoreCase("create")) {
					rval = new WorldCreate(sender, args);
				} else if (cmdLabel.equalsIgnoreCase("spawn")) {
					rval = new WorldSpawn(sender, args);
				} else if (cmdLabel.equalsIgnoreCase("evacuate")) {
					rval = new WorldEvacuate(sender, args);
				} else if (cmdLabel.equalsIgnoreCase("evac")) {
					rval = new WorldEvacuate(sender, args);
				} else if (cmdLabel.equalsIgnoreCase("repair")) {
					rval = new WorldRepair(sender, args);
				} else if (cmdLabel.equalsIgnoreCase("rep")) {
					rval = new WorldRepair(sender, args);
				} else if (cmdLabel.equalsIgnoreCase("save")) {
					rval = new WorldSave(sender, args);
				} else if (cmdLabel.equalsIgnoreCase("delete")) {
					rval = new WorldDelete(sender, args);
				} else if (cmdLabel.equalsIgnoreCase("del")) {
					rval = new WorldDelete(sender, args);
				} else if (cmdLabel.equalsIgnoreCase("copy")) {
					rval = new WorldCopy(sender, args);
				} else if (cmdLabel.equalsIgnoreCase("togglepvp")) {
					rval = new WorldTogglePVP(sender, args);
				} else if (cmdLabel.equalsIgnoreCase("tpvp")) {
					rval = new WorldTogglePVP(sender, args);
				} else if (cmdLabel.equalsIgnoreCase("pvp")) {
					rval = new WorldTogglePVP(sender, args);		
				} else if (cmdLabel.equalsIgnoreCase("weather")) {
					rval = new WorldWeather(sender, args);
				} else if (cmdLabel.equalsIgnoreCase("w")) {
					rval = new WorldWeather(sender, args);
				} else if (cmdLabel.equalsIgnoreCase("time")) {
					rval = new WorldTime(sender, args);
				} else if (cmdLabel.equalsIgnoreCase("t")) {
					rval = new WorldTime(sender, args);
				} else if (cmdLabel.equalsIgnoreCase("allowspawn")) {
					rval = new WorldSpawning(sender, args, true);
				} else if (cmdLabel.equalsIgnoreCase("denyspawn")) {
					rval = new WorldSpawning(sender, args, false);
				} else if (cmdLabel.equalsIgnoreCase("spawnallow")) {
					rval = new WorldSpawning(sender, args, true);
				} else if (cmdLabel.equalsIgnoreCase("spawndeny")) {
					rval = new WorldSpawning(sender, args, false);
				} else if (cmdLabel.equalsIgnoreCase("allowspawning")) {
					rval = new WorldSpawning(sender, args, true);
				} else if (cmdLabel.equalsIgnoreCase("denyspawning")) {
					rval = new WorldSpawning(sender, args, false);
				} else if (cmdLabel.equalsIgnoreCase("setportal")) {
					rval = new WorldSetPortal(sender, args);
				} else if (cmdLabel.equalsIgnoreCase("setdefaultportal")) {
					rval = new WorldSetPortal(sender, args);
				} else if (cmdLabel.equalsIgnoreCase("setdefportal")) {
					rval = new WorldSetPortal(sender, args);
				} else if (cmdLabel.equalsIgnoreCase("setdefport")) {
					rval = new WorldSetPortal(sender, args);
				} else if (cmdLabel.equalsIgnoreCase("setspawn")) {
					rval = new WorldSetSpawn(sender, args);
				} else if (cmdLabel.equalsIgnoreCase("gamemode")) {
					rval = new WorldGamemode(sender, args);
				} else if (cmdLabel.equalsIgnoreCase("setgamemode")) {
					rval = new WorldGamemode(sender, args);
				} else if (cmdLabel.equalsIgnoreCase("gm")) {
					rval = new WorldGamemode(sender, args);
				} else if (cmdLabel.equalsIgnoreCase("setgm")) {
					rval = new WorldGamemode(sender, args);
				} else if (cmdLabel.equalsIgnoreCase("generators")) {
					rval = new WorldListGenerators(sender, args);
				} else if (cmdLabel.equalsIgnoreCase("gen")) {
					rval = new WorldListGenerators(sender, args);
				} else if (cmdLabel.equalsIgnoreCase("listgenerators")) {
					rval = new WorldListGenerators(sender, args);
				} else if (cmdLabel.equalsIgnoreCase("listgen")) {
					rval = new WorldListGenerators(sender, args);
				} else if (cmdLabel.equalsIgnoreCase("togglespawnloaded")) {
					rval = new WorldToggleSpawnLoaded(sender, args);
				} else if (cmdLabel.equalsIgnoreCase("spawnloaded")) {
					rval = new WorldToggleSpawnLoaded(sender, args);
				} else if (cmdLabel.equalsIgnoreCase("keepspawnloaded")) {
					rval = new WorldToggleSpawnLoaded(sender, args);
				} else if (cmdLabel.equalsIgnoreCase("difficulty")) {
					rval = new WorldDifficulty(sender, args);
				} else if (cmdLabel.equalsIgnoreCase("difficult")) {
					rval = new WorldDifficulty(sender, args);
				} else if (cmdLabel.equalsIgnoreCase("diff")) {
					rval = new WorldDifficulty(sender, args);
				} else if (cmdLabel.equalsIgnoreCase("op")) {
					rval = new WorldOpping(sender, args, true);
				} else if (cmdLabel.equalsIgnoreCase("deop")) {
					rval = new WorldOpping(sender, args, false);
				} else if (cmdLabel.equalsIgnoreCase("setsave")) {
					rval = new WorldSetSaving(sender, args);
				} else if (cmdLabel.equalsIgnoreCase("setsaving")) {
					rval = new WorldSetSaving(sender, args);
				} else if (cmdLabel.equalsIgnoreCase("saving")) {
					rval = new WorldSetSaving(sender, args);
				} else if (cmdLabel.equalsIgnoreCase("autosave")) {
					rval = new WorldSetSaving(sender, args);
				} else if (cmdLabel.equalsIgnoreCase("config")) {
					rval = new WorldConfig(sender, args);
				} else if (cmdLabel.equalsIgnoreCase("cfg")) {
					rval = new WorldConfig(sender, args);
				} else if (cmdLabel.equalsIgnoreCase("reloadwhenempty")) {
					rval = new WorldReloadWE(sender, args);
				} else if (cmdLabel.equalsIgnoreCase("reloadwe")) {
					rval = new WorldReloadWE(sender, args);
				} else if (cmdLabel.equalsIgnoreCase("reloadempty")) {
					rval = new WorldReloadWE(sender, args);
				} else if (cmdLabel.equalsIgnoreCase("reloadnoplayers")) {
					rval = new WorldReloadWE(sender, args);
				} else if (cmdLabel.equalsIgnoreCase("formsnow")) {
					rval = new WorldFormSnow(sender, args);
				} else if (cmdLabel.equalsIgnoreCase("formice")) {
					rval = new WorldFormIce(sender, args);
				} else if (cmdLabel.equalsIgnoreCase("showsnow")) {
					rval = new WorldShowSnow(sender, args);
				} else if (cmdLabel.equalsIgnoreCase("showrain")) {
					rval = new WorldShowRain(sender, args);
				} else if (cmdLabel.equalsIgnoreCase("teleport")) {
					rval = new TeleportPortal(sender, args);
				} else if (cmdLabel.equalsIgnoreCase("tp")) {
					rval = new TeleportPortal(sender, args);
				} else if (cmdLabel.equalsIgnoreCase("inventory")) {
					rval = new WorldInventory(sender, args);
				} else if (cmdLabel.equalsIgnoreCase("inv")) {
					rval = new WorldInventory(sender, args);
				}
			}
		} else if (cmdLabel.equalsIgnoreCase("tpp")) {
			rval = new TeleportPortal(sender, args);
		}
		if (rval == null) {
			rval = new Command(sender, new String[] {cmdLabel});
			rval.execute();
		} else if (!rval.hasPermission()) {
			if (rval.player == null) {
				rval.sender.sendMessage("This command is only for players!");
			} else {
				rval.locmessage("command.nopermission");
			}
		} else {
			rval.execute();
		}
	}		
	
	public void execute() {
		//This is executed when no command was found
		boolean hac = false; //has available commands
		if (showUsage("world.repair")) hac = true;
		if (showUsage("world.delete")) hac = true;
		if (showUsage("world.rename")) hac = true;
		if (showUsage("world.copy")) hac = true;
		if (showUsage("world.save")) hac = true;
		if (showUsage("world.load")) hac = true;
		if (showUsage("world.unload")) hac = true;
		if (showUsage("world.reloadwhenempty")) hac = true;
		if (showUsage("world.create")) hac = true;
		if (showUsage("world.listgenerators")) hac = true;
		if (showUsage("world.weather")) hac = true;
		if (showUsage("world.time")) hac = true;
		if (showUsage("world.spawn")) hac = true;
		if (showUsage("world.setspawn")) hac = true;
		if (showUsage("world.list")) hac = true;
		if (showUsage("world.info")) hac = true;
		if (showUsage("world.portals")) hac = true;
		if (showUsage("world.gamemode")) hac = true;
		if (showUsage("world.togglepvp")) hac = true;
		if (showUsage("world.op")) hac = true;
		if (showUsage("world.deop")) hac = true;
		if (showUsage("world.allowspawn")) hac = true;
		if (showUsage("world.denyspawn")) hac = true;
		if (showUsage("world.formsnow")) hac = true;
		if (showUsage("world.formoce")) hac = true;
		if (showUsage("tpp")) hac = true;
		if (hac) {
			message(ChatColor.RED + "Unknown command: " + args[0]);
		} else {
			locmessage("command.nopermission");
		}
	}

}
