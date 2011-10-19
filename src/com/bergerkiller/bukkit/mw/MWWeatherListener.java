package com.bergerkiller.bukkit.mw;

import org.bukkit.World;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.event.weather.WeatherListener;

public class MWWeatherListener extends WeatherListener {
		
	public static void setWeather(World w, boolean storm) {
		ignoreChanges = true;
		w.setStorm(storm);
		ignoreChanges = false;
	}
	
	public static boolean ignoreChanges = false;
	public void onWeatherChange(WeatherChangeEvent event) {
		if (!ignoreChanges && WorldConfig.get(event.getWorld()).holdWeather) {
			event.setCancelled(true);
		}
	}
	
}
