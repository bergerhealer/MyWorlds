package com.bergerkiller.bukkit.mw.external;

import java.util.logging.Level;

import org.bukkit.entity.Player;
import org.getspout.spoutapi.SpoutManager;
import org.getspout.spoutapi.block.SpoutWeather;
import org.getspout.spoutapi.player.SpoutPlayer;

import com.bergerkiller.bukkit.mw.MyWorlds;

/**
 * Deals with all SpoutPlugin related calls.
 * Note: uses deprecated methods and types, if no longer supported, feature is broken.
 */
@SuppressWarnings("deprecation")
public class SpoutPluginHandler {

	/**
	 * Sets the weather type shown to SpoutCraft players
	 * 
	 * @param player to set it for
	 * @param showRain - whether rain is shown
	 * @param showSnow - whether snow is shown
	 */
	public static void setPlayerWeather(Player player, boolean showRain, boolean showSnow) {
		try {
			SpoutPlayer sp = SpoutManager.getPlayer(player);
			if (sp.isSpoutCraftEnabled()) {
				SpoutWeather w = SpoutWeather.NONE;
				if (player.getWorld().hasStorm()) {
					if (showRain && showSnow) {
						w = SpoutWeather.RESET;
					} else if (showRain) {
						w = SpoutWeather.RAIN;
					} else if (showSnow) {
						w = SpoutWeather.SNOW;
					}
				}
				SpoutManager.getBiomeManager().setPlayerWeather(SpoutManager.getPlayer(player), w);
			}
		} catch (Throwable t) {
			MyWorlds.isSpoutPluginEnabled = false;
			MyWorlds.plugin.log(Level.SEVERE, "An error occured while using SpoutPlugin, SpoutPlugin is no longer used in MyWorlds from now on:");
			t.printStackTrace();
		}
	}
}
