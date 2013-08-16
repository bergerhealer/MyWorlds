package com.bergerkiller.bukkit.mw.commands;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.bergerkiller.bukkit.common.MessageBuilder;
import com.bergerkiller.bukkit.common.utils.CommonUtil;
import com.bergerkiller.bukkit.common.utils.StringUtil;
import com.bergerkiller.bukkit.common.utils.WorldUtil;
import com.bergerkiller.bukkit.mw.Localization;
import com.bergerkiller.bukkit.mw.MyWorlds;
import com.bergerkiller.bukkit.mw.Permission;
import com.bergerkiller.bukkit.mw.Portal;
import com.bergerkiller.bukkit.mw.WorldManager;
import com.bergerkiller.bukkit.mw.WorldMode;

public class Command {
	public Permission permission;
	public String commandNode;
	public String command;
	public Player player;
	public CommandSender sender;
	public String[] args;
	public String worldname;
	public WorldMode forcedWorldMode;

	public Command(Permission permission, String commandNode) {
		this.permission = permission;
		this.commandNode = commandNode;
	}

	public void init(CommandSender sender, String[] args) {
		this.args = args;
		this.sender = sender;
		if (sender instanceof Player) {
			this.player = (Player) sender;
		}
	}

	/**
	 * Whether the console can use this Command
	 * 
	 * @return True if the console can use it, False if not
	 */
	public boolean allowConsole() {
		return true;
	}

	/**
	 * Removes a single argument from the arguments of this command and returns it
	 * 
	 * @param index of the argument to remove
	 * @return removed argument
	 */
	public String removeArg(int index) {
		String value = this.args[index];
		this.args = StringUtil.remove(this.args, index);
		return value;
	}

	public boolean hasPermission() {
		if (this.permission == null) {
			return true;
		}
		if (this.player == null) {
			return this.allowConsole();
		} else {
			return this.permission.has(this.player);
		}
	}

	public boolean handleWorld() {
		if (this.worldname == null) {
			locmessage(Localization.WORLD_NOTFOUND);
		}
		return this.worldname != null;
	}
	
	public void messageNoSpout() {
		if (MyWorlds.isSpoutPluginEnabled) return;
		this.message(ChatColor.YELLOW + "Note that Spout is not installed right now!");
	}
	public void message(String msg) {
		if (msg == null) return;
		CommonUtil.sendMessage(this.sender, msg);
	}

	public void locmessage(Localization node, String... arguments) {
		node.message(this.sender, arguments);
	}

	public void logAction(String action) {
		MyWorlds.plugin.logAction(this.sender, action);
	}

	public boolean showInv() {
		return this.showInv(this.commandNode);
	}

	public boolean showInv(String node) {
		message(ChatColor.RED + "Invalid arguments for this command!");
		return showUsage(node);
	}

	public boolean showUsage() {
		return showUsage(this.commandNode);
	}

	public boolean showUsage(String commandNode) {
		if (hasPermission()) {
			this.sender.sendMessage(MyWorlds.plugin.getCommandUsage(commandNode));
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
			builder.setIndent(2).setSeparator(ChatColor.WHITE, " / ").newLine();
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

	/**
	 * Finds out the world to operate in, checking the command arguments if possible.
	 * 
	 * @param preceedingArgCount expected before the world argument
	 */
	public void genWorldname(int preceedingArgCount) {
		if (args.length > 0 && args.length > preceedingArgCount) {
			this.worldname = WorldManager.matchWorld(args[args.length - 1]);
			if (this.worldname != null) {
				return;
			}
		}
		if (player != null) {
			this.worldname = player.getWorld().getName();
		} else {
			this.worldname = WorldUtil.getWorlds().iterator().next().getName();
		}
	}

	/**
	 * Reads a World Mode set on the world name using the /-parameter
	 * For example, /world create world1/nether will read the nether forced mode.
	 */
	public void genForcedWorldMode() {
		int idx = this.worldname.indexOf('/');
		this.forcedWorldMode = null;
		if (idx != -1) {
			this.forcedWorldMode = WorldMode.get(this.worldname.substring(idx + 1), WorldMode.NORMAL);
			this.worldname = this.worldname.substring(0, idx);
		}
	}

	/**
	 * Extracts the generator name including generator arguments from the world name previously parsed<br>
	 * Requires a worldname to be generated first
	 * 
	 * @return generator name and arguments, or null if there are none
	 */
	public String getGeneratorName() {
		String gen = null;
		if (this.worldname.contains(":")) {
			String[] parts = this.worldname.split(":");
			if (parts.length == 2) {
				this.worldname = parts[0];
				gen = parts[1];
			} else {
				this.worldname = parts[0];
				gen = parts[1] + ":" + parts[2];
			}
		}
		return gen;
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
					rval = new WorldList();
				} else if (cmdLabel.equalsIgnoreCase("info")) {
					rval = new WorldInfo();
				} else if (cmdLabel.equalsIgnoreCase("i")) {
					rval = new WorldInfo();
				} else if (cmdLabel.equalsIgnoreCase("portals")) {
					rval = new WorldPortals();
				} else if (cmdLabel.equalsIgnoreCase("portal")) {
					rval = new WorldPortals();
				} else if (cmdLabel.equalsIgnoreCase("load")) {
					rval = new WorldLoad();
				} else if (cmdLabel.equalsIgnoreCase("unload")) {
					rval = new WorldUnload();
				} else if (cmdLabel.equalsIgnoreCase("create")) {
					rval = new WorldCreate();
				} else if (cmdLabel.equalsIgnoreCase("spawn")) {
					rval = new WorldSpawn();
				} else if (cmdLabel.equalsIgnoreCase("evacuate")) {
					rval = new WorldEvacuate();
				} else if (cmdLabel.equalsIgnoreCase("evac")) {
					rval = new WorldEvacuate();
				} else if (cmdLabel.equalsIgnoreCase("repair")) {
					rval = new WorldRepair();
				} else if (cmdLabel.equalsIgnoreCase("rep")) {
					rval = new WorldRepair();
				} else if (cmdLabel.equalsIgnoreCase("save")) {
					rval = new WorldSave();
				} else if (cmdLabel.equalsIgnoreCase("delete")) {
					rval = new WorldDelete();
				} else if (cmdLabel.equalsIgnoreCase("del")) {
					rval = new WorldDelete();
				} else if (cmdLabel.equalsIgnoreCase("copy")) {
					rval = new WorldCopy();
				} else if (cmdLabel.equalsIgnoreCase("togglepvp")) {
					rval = new WorldSetPVP();
				} else if (cmdLabel.equalsIgnoreCase("tpvp")) {
					rval = new WorldSetPVP();
				} else if (cmdLabel.equalsIgnoreCase("pvp")) {
					rval = new WorldSetPVP();		
				} else if (cmdLabel.equalsIgnoreCase("weather")) {
					rval = new WorldWeather();
				} else if (cmdLabel.equalsIgnoreCase("w")) {
					rval = new WorldWeather();
				} else if (cmdLabel.equalsIgnoreCase("time")) {
					rval = new WorldTime();
				} else if (cmdLabel.equalsIgnoreCase("t")) {
					rval = new WorldTime();
				} else if (cmdLabel.equalsIgnoreCase("allowspawn")) {
					rval = new WorldSpawning(true);
				} else if (cmdLabel.equalsIgnoreCase("denyspawn")) {
					rval = new WorldSpawning(false);
				} else if (cmdLabel.equalsIgnoreCase("spawnallow")) {
					rval = new WorldSpawning(true);
				} else if (cmdLabel.equalsIgnoreCase("spawndeny")) {
					rval = new WorldSpawning(false);
				} else if (cmdLabel.equalsIgnoreCase("allowspawning")) {
					rval = new WorldSpawning(true);
				} else if (cmdLabel.equalsIgnoreCase("denyspawning")) {
					rval = new WorldSpawning(false);
				} else if (cmdLabel.equalsIgnoreCase("setnetherportal")) {
					rval = new WorldSetNetherPortal();
				} else if (cmdLabel.equalsIgnoreCase("setendportal")) {
					rval = new WorldSetEnderPortal();
				} else if (cmdLabel.equalsIgnoreCase("setspawn")) {
					rval = new WorldSetSpawn();
				} else if (cmdLabel.equalsIgnoreCase("gamemode")) {
					rval = new WorldGamemode();
				} else if (cmdLabel.equalsIgnoreCase("setgamemode")) {
					rval = new WorldGamemode();
				} else if (cmdLabel.equalsIgnoreCase("gm")) {
					rval = new WorldGamemode();
				} else if (cmdLabel.equalsIgnoreCase("setgm")) {
					rval = new WorldGamemode();
				} else if (cmdLabel.equalsIgnoreCase("generators")) {
					rval = new WorldListGenerators();
				} else if (cmdLabel.equalsIgnoreCase("gen")) {
					rval = new WorldListGenerators();
				} else if (cmdLabel.equalsIgnoreCase("listgenerators")) {
					rval = new WorldListGenerators();
				} else if (cmdLabel.equalsIgnoreCase("listgen")) {
					rval = new WorldListGenerators();
				} else if (cmdLabel.equalsIgnoreCase("togglespawnloaded")) {
					rval = new WorldSetSpawnLoaded();
				} else if (cmdLabel.equalsIgnoreCase("spawnloaded")) {
					rval = new WorldSetSpawnLoaded();
				} else if (cmdLabel.equalsIgnoreCase("keepspawnloaded")) {
					rval = new WorldSetSpawnLoaded();
				} else if (cmdLabel.equalsIgnoreCase("difficulty")) {
					rval = new WorldDifficulty();
				} else if (cmdLabel.equalsIgnoreCase("difficult")) {
					rval = new WorldDifficulty();
				} else if (cmdLabel.equalsIgnoreCase("diff")) {
					rval = new WorldDifficulty();
				} else if (cmdLabel.equalsIgnoreCase("op")) {
					rval = new WorldOpping(true);
				} else if (cmdLabel.equalsIgnoreCase("deop")) {
					rval = new WorldOpping(false);
				} else if (cmdLabel.equalsIgnoreCase("setsave")) {
					rval = new WorldSetSaving();
				} else if (cmdLabel.equalsIgnoreCase("setsaving")) {
					rval = new WorldSetSaving();
				} else if (cmdLabel.equalsIgnoreCase("saving")) {
					rval = new WorldSetSaving();
				} else if (cmdLabel.equalsIgnoreCase("autosave")) {
					rval = new WorldSetSaving();
				} else if (cmdLabel.equalsIgnoreCase("config")) {
					rval = new WorldConfig();
				} else if (cmdLabel.equalsIgnoreCase("cfg")) {
					rval = new WorldConfig();
				} else if (cmdLabel.equalsIgnoreCase("reloadwhenempty")) {
					rval = new WorldReloadWE();
				} else if (cmdLabel.equalsIgnoreCase("reloadwe")) {
					rval = new WorldReloadWE();
				} else if (cmdLabel.equalsIgnoreCase("reloadempty")) {
					rval = new WorldReloadWE();
				} else if (cmdLabel.equalsIgnoreCase("reloadnoplayers")) {
					rval = new WorldReloadWE();
				} else if (cmdLabel.equalsIgnoreCase("formsnow")) {
					rval = new WorldFormSnow();
				} else if (cmdLabel.equalsIgnoreCase("formice")) {
					rval = new WorldFormIce();
				} else if (cmdLabel.equalsIgnoreCase("showsnow")) {
					rval = new WorldShowSnow();
				} else if (cmdLabel.equalsIgnoreCase("showrain")) {
					rval = new WorldShowRain();
				} else if (cmdLabel.equalsIgnoreCase("teleport")) {
					rval = new TeleportPortal();
				} else if (cmdLabel.equalsIgnoreCase("tp")) {
					rval = new TeleportPortal();
				} else if (cmdLabel.equalsIgnoreCase("inventory")) {
					rval = new WorldInventory();
				} else if (cmdLabel.equalsIgnoreCase("inv")) {
					rval = new WorldInventory();
				} else if (cmdLabel.equalsIgnoreCase("togglerespawn")) {
					rval = new WorldSetForcedRespawn();
				} else if (cmdLabel.equalsIgnoreCase("forcedrespawn")) {
					rval = new WorldSetForcedRespawn();
				} else if (cmdLabel.equalsIgnoreCase("rememberlastpos")) {
					rval = new WorldSetRememberPlayerPos();
				} else if (cmdLabel.equalsIgnoreCase("setrememberlastpos")) {
					rval = new WorldSetRememberPlayerPos();
				} else if (cmdLabel.equalsIgnoreCase("rememberlastplayerpos")) {
					rval = new WorldSetRememberPlayerPos();
				} else if (cmdLabel.equalsIgnoreCase("setrememberlastplayerpos")) {
					rval = new WorldSetRememberPlayerPos();
				} else if (cmdLabel.equalsIgnoreCase("rememberlastposition")) {
					rval = new WorldSetRememberPlayerPos();
				} else if (cmdLabel.equalsIgnoreCase("setrememberlastposition")) {
					rval = new WorldSetRememberPlayerPos();
				} else if (cmdLabel.equalsIgnoreCase("rememberlastplayerposition")) {
					rval = new WorldSetRememberPlayerPos();
				} else if (cmdLabel.equalsIgnoreCase("setrememberlastplayerposition")) {
					rval = new WorldSetRememberPlayerPos();
				}
			}
		} else if (cmdLabel.equalsIgnoreCase("tpp")) {
			rval = new TeleportPortal();
		}
		if (rval == null) {
			rval = new Command(null, null);
			rval.init(sender, new String[] {cmdLabel});
			rval.execute();
		} else {
			rval.init(sender, args);
			if (!rval.hasPermission()) {
				if (rval.player == null) {
					rval.sender.sendMessage("This command is only for players!");
				} else {
					rval.locmessage(Localization.COMMAND_NOPERM);
				}
			} else {
				rval.execute();
			}
		}
	}

	public void execute() {
		//This is executed when no command was found
		Localization.COMMAND_UNKNOWN.message(sender, args[0]);
		Localization.COMMAND_HELP.message(sender);
	}
}
