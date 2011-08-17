package com.bergerkiller.bukkit.mw;

import java.io.*;
import java.util.ArrayList;
import java.util.Random;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.entity.Player;

import com.bergerkiller.bukkit.mw.Tag.Type;

public class WorldManager {
	
	private static File serverfolder;
	public static File getServerFolder() {
		if (serverfolder == null) serverfolder = MyWorlds.plugin.getDataFolder().getAbsoluteFile().getParentFile().getParentFile();
		return serverfolder;
	}
	
	public static long getSeed(String seed) {
		long seedval = 0;
		try {
			seedval = Long.parseLong(seed);
		} catch (Exception ex) {
			seedval = seed.hashCode();
		}
		return seedval;
	}
	public static long getRandomSeed(String seed) {
		long seedval = getSeed(seed);
		if (seedval == 0) {
			seedval = new Random().nextLong();
		} 
		return seedval;
	}
	
	public static Environment getEnvironment(String worldname) {
		for (Environment env : Environment.values()) {
			if (worldname.toUpperCase().endsWith("_" + env.toString())) {
				return env;
			}
		}
		return Environment.NORMAL;
	}
	
	
	public static boolean generateData(String worldname, String seed) {
		Tag data = new Tag(Type.TAG_Compound, "Data", new Tag[] {
				new Tag(Type.TAG_Byte, "thundering", (byte) 0), 
				new Tag(Type.TAG_Long, "LastPlayed", System.currentTimeMillis()),
				new Tag(Type.TAG_Long, "RandomSeed", getRandomSeed(seed)), 
				new Tag(Type.TAG_Int, "version", 19132), 
				new Tag(Type.TAG_Long, "Time", 0L),
				new Tag(Type.TAG_Byte, "raining", (byte) 0), 
				new Tag(Type.TAG_Int, "SpawnX", 0), 
				new Tag(Type.TAG_Int, "thunderTime", (int) 200000000), 
				new Tag(Type.TAG_Int, "SpawnY", 64), 
				new Tag(Type.TAG_Int, "SpawnZ", 0), 
				new Tag(Type.TAG_String, "LevelName", worldname),
				new Tag(Type.TAG_Long, "SizeOnDisk", getWorldSize(worldname)),
				new Tag(Type.TAG_Long, "rainTime", 50000L), 
				new Tag(Type.TAG_End, null, null)});
		Tag finaltag = new Tag(Type.TAG_Compound, null, new Tag[] {data, new Tag(Type.TAG_End, null, null)});
				
		//write the data
	    return setData(worldname, finaltag);
	}
	public static Tag getData(String worldname) {
		File f = getDataFile(worldname);
		if (!f.exists()) return null;
		try {
			FileInputStream fis = new FileInputStream(f);
			Tag t = Tag.readFrom(fis);
			fis.close();
			return t;
		} catch (Exception ex) {
			return null;
		}
	}
	public static boolean setData(String worldname, Tag data) {
    	File datafile = getDataFile(worldname);
    	try {
    		FileOutputStream erasor = new FileOutputStream(datafile);
    		erasor.write((new String()).getBytes());
    		erasor.close();

			OutputStream s = new FileOutputStream(datafile);
			data.writeTo(s);
			s.close();
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public static File getDataFolder(String worldname) {
		return new File(getServerFolder() + File.separator + worldname);
	}
	public static File getDataFile(String worldname) {
		return new File(getDataFolder(worldname) + File.separator + "level.dat");
	}

	public static long getWorldSize(String worldname) {
		return getFolderSize(getDataFolder(worldname));
	}
	public static WorldInfo getInfo(String worldname) {
		try {
			Tag t = getData(worldname);
			if (t != null) {
				WorldInfo info = new WorldInfo();
				info.name = t.findTagByName("LevelName").getValue().toString();
				info.seed = (Long) t.findTagByName("RandomSeed").getValue();
				info.size = (Long) t.findTagByName("SizeOnDisk").getValue();
				if (info.size == 0) info.size = getWorldSize(worldname);
				return info;
			}
		} catch (Exception ex) {}
		World w = getWorld(worldname);
		if (w == null) return null;
		WorldInfo info = new WorldInfo();
		info.name = w.getName();
		info.seed = w.getSeed();
		info.size = getWorldSize(worldname);
		return info;
	}
	public static String[] getWorlds() {
		ArrayList<String> rval = new ArrayList<String>();
		for (String world : getServerFolder().list()) {
			if (getDataFile(world).exists()) {
				rval.add(world);
			}
		}
		return rval.toArray(new String[0]);
	}
	public static String matchWorld(String matchname) {
		String[] worldnames = getWorlds();
		for (String worldname : worldnames) {
			if (worldname.equalsIgnoreCase(matchname)) return worldname;
		}
		matchname = matchname.toLowerCase();
		for (String worldname : worldnames) {
			if (worldname.toLowerCase().contains(matchname)) return worldname;
		}
		return null;
	}
	public static World getWorld(String worldname) {
		if (worldname == null) return null;
		try {
			return Bukkit.getServer().getWorld(worldname);
		} catch (Exception ex) {
			return null;
		}

	}
	
	public static boolean isBroken(String worldname) {
		if (getData(worldname) == null) {
			return getWorld(worldname) == null;
		}
		return true;
	}
	public static boolean isLoaded(String worldname) {
		return getWorld(worldname) != null;
	}
	public static boolean worldExists(String worldname) {
		return getDataFile(worldname).exists();
	}
	
	public static World getOrCreateWorld(String worldname) {
		World w = getWorld(worldname);
		if (w != null) return w;
		return createWorld(worldname, null);
	}
	public static World createWorld(String worldname, String seed) {
		try {
			MyWorlds.log(Level.INFO, "[MyWorlds] Loading or creating world: " + worldname);
			Environment env = getEnvironment(worldname);
			if (seed == null || seed == "") {
				return Bukkit.getServer().createWorld(worldname, env);
			} else {
				return Bukkit.getServer().createWorld(worldname, env, getSeed(seed));
			}
		} catch (Exception ex) {
			return null;
		}
	}
	
	private static boolean delete(File folder) {
		if (folder.isDirectory()) {
			for (File f : folder.listFiles()) {
				if (!delete(f)) return false;
			}
		}
		return folder.delete();
	}
    private static boolean copy(File sourceLocation , File targetLocation) {
    	try {
            if (sourceLocation.isDirectory()) {
                if (!targetLocation.exists()) {
                    targetLocation.mkdir();
                }
                for (String child : sourceLocation.list()) {
                    if (!copy(new File(sourceLocation, child), new File(targetLocation, child))) {
                    	return false;
                    }
                }
            } else {
                InputStream in = new FileInputStream(sourceLocation);
                OutputStream out = new FileOutputStream(targetLocation);
                // Copy the bits from instream to outstream
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                in.close();
                out.close();
            }
            return true;
    	} catch (IOException ex) {
    		ex.printStackTrace();
    		return false;
    	}
    }
    private static boolean rename(File sourceLocation, File targetLocation) {
    	return sourceLocation.renameTo(targetLocation);
    }
	private static long getFolderSize(File folder) {
		if (!folder.exists()) return 0;
		if (folder.isDirectory()) {
			long size = 0;
			for (File subfile : folder.listFiles()) {
				size += getFolderSize(subfile);
			}
			return size;
		} else {
			return folder.length();
		}
	}
    private static boolean renameWorld(String worldname, String newname) {
    	if (isLoaded(worldname)) return false;
    	Tag t = getData(worldname);
    	if (t == null) return false;
    	t = t.findTagByName("Data");
    	if (t == null || t.getType() != Type.TAG_Compound) return false;
    	int i = 0;
    	for (Tag tt : (Tag[]) t.getValue()) {
    		if (tt.getName().equals("LevelName")) {
    			t.removeTag(i);
    			t.insertTag(new Tag(Type.TAG_String, "LevelName", newname), i);
    			break;
    		}
    		i++;
    	}
    	return setData(worldname, t);
    }
    
	public static boolean deleteWorld(String worldname) {
		return delete(getDataFolder(worldname));
	}
	public static boolean copyWorld(String worldname, String newname) {
		if (!copy(getDataFolder(worldname), getDataFolder(newname))) return false;;
		renameWorld(newname, newname);
		File uid = new File(getDataFolder(newname) + File.separator + "uid.dat");
		if (uid.exists()) uid.delete();
		return true;
	}
	
	public static void evacuate(World world, String message) {
		Player[] players = world.getPlayers().toArray(new Player[0]);
		if (players.length == 0) return;
		Location to = null;
		for (String name : Portal.getPortals(world)) {
			Portal p = Portal.get(name);
			if (p != null) {
				to = p.getDestination();
				if (to != null && to.getWorld() != world) {
					break;
				}
			}
		}
		if (to == null) {
			//find a possible world as hideout for those allowed
			for (World w : Bukkit.getServer().getWorlds()) {
				if (w != world) {
					to = w.getSpawnLocation();
					break;
				}
			}
			//evacuate
			for (Player p : players) {
				if (to != null && (Permission.has(p, "world.spawn") || Permission.has(p, "tpp"))) {
					p.teleport(to);
					p.sendMessage(ChatColor.RED + message);
				} else {
					p.kickPlayer(message);
				}
			}
		} else {
			for (Player p : players) {
				p.teleport(to);
				p.sendMessage(ChatColor.RED + message);
			}
		}
	}
	public static void teleportToClonedWorld(World from, World cloned) {
		for (Player p : from.getPlayers().toArray(new Player[0])) {
			Location loc = p.getLocation();
			loc.setWorld(cloned);
			p.teleport(loc);
		}
	}
	public static void teleportToWorldSpawn(World from, World to) {
		for (Player p : from.getPlayers().toArray(new Player[0])) {
			p.teleport(to.getSpawnLocation());
		}
	}
}