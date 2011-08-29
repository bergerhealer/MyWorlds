package com.bergerkiller.bukkit.mw;

import java.util.HashSet;

import org.bukkit.World;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.event.weather.WeatherListener;

public class MWWeatherListener extends WeatherListener {
	
	public static HashSet<String> holdWorlds = new HashSet<String>();
	
	public static void setWeather(World w, boolean storm, boolean hold) {
		holdWorld(w.getName(), hold);
		ignoreChanges = true;
		w.setStorm(storm);
		ignoreChanges = false;
	}
	public static void holdWorld(String worldname, boolean hold) {
		if (hold) {
			holdWorlds.add(worldname.toLowerCase());
		} else {
			holdWorlds.remove(worldname.toLowerCase());
		}
	}
	public static boolean isHolding(String worldname) {
		return holdWorlds.contains(worldname.toLowerCase());
	}
	
	public static boolean ignoreChanges = false;
	public void onWeatherChange(WeatherChangeEvent event) {
		if (!ignoreChanges && isHolding(event.getWorld().getName())) {
			event.setCancelled(true);
		}
	}
	
}
