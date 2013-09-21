package com.bergerkiller.bukkit.mw.version;

import org.bukkit.entity.LivingEntity;

import com.bergerkiller.bukkit.common.nbt.CommonTagList;

public interface AttributesUtil {

	/**
	 * Checks whether this utility class can be used at all without causing trouble
	 * 
	 * @return True if valid, False if invalid
	 */
	public boolean isValid();

	/**
	 * Resets all attributes set for an Entity to the defaults
	 * 
	 * @param entity to reset
	 */
	public void resetAttributes(LivingEntity entity);

	/**
	 * Loads the attributes for an Entity, applying the new attributes to the entity
	 * 
	 * @param entity to load
	 * @param data to load from
	 */
	public void addAttributes(LivingEntity entity, CommonTagList data);

	/**
	 * Saves the current attributes of an Entity to a new CommonTagList
	 * 
	 * @param entity to save
	 * @return CommonTagList containing the saved data
	 */
	public CommonTagList saveAttributes(LivingEntity entity);
}
