package com.bergerkiller.bukkit.mw.version;

import org.bukkit.entity.LivingEntity;

import com.bergerkiller.bukkit.common.conversion.Conversion;
import com.bergerkiller.bukkit.common.nbt.CommonTag;
import com.bergerkiller.bukkit.common.nbt.CommonTagList;
import com.bergerkiller.bukkit.common.reflection.FieldAccessor;
import com.bergerkiller.bukkit.common.reflection.MethodAccessor;
import com.bergerkiller.bukkit.common.reflection.SafeField;
import com.bergerkiller.bukkit.common.reflection.SafeMethod;
import com.bergerkiller.bukkit.common.utils.CommonUtil;

/**
 * Version-bound attribute utilities
 */
public class NMSAttributes {
	private static Class<?> entityLivingType = CommonUtil.getNMSClass("EntityLiving");
	private static Class<?> genericAttributesType = CommonUtil.getNMSClass("GenericAttributes");
	private static Class<?> attributeMapBaseType = CommonUtil.getNMSClass("AttributeMapBase");
	private static Class<?> iConsoleLogManagerType = CommonUtil.getNMSClass("IConsoleLogManager");
	private static Class<?> nbtTagListType = CommonUtil.getNMSClass("NBTTagList");
	private static FieldAccessor<Object> entityLivingAttrMap = new SafeField<Object>(entityLivingType, "d");
	private static MethodAccessor<Object> getAttributesMap = new SafeMethod<Object>(entityLivingType, "aW");
	private static MethodAccessor<Void> resetAttributes = new SafeMethod<Void>(entityLivingType, "ay");
	private static MethodAccessor<Object> saveAttributes = new SafeMethod<Object>(genericAttributesType, "a", attributeMapBaseType);
	private static MethodAccessor<Void> loadAttributes = new SafeMethod<Void>(genericAttributesType, "a", attributeMapBaseType, nbtTagListType, iConsoleLogManagerType);

	/**
	 * Checks whether this utility class can be used at all without causing trouble
	 * 
	 * @return True if valid, False if invalid
	 */
	public static boolean isValid() {
		return getAttributesMap.isValid() && resetAttributes.isValid() && 
				saveAttributes.isValid() && loadAttributes.isValid() &&
				entityLivingAttrMap.isValid();
	}

	/**
	 * Resets all attributes set for an Entity to the defaults
	 * 
	 * @param entity to reset
	 */
	public static void resetAttributes(LivingEntity entity) {
		Object livingHandle = Conversion.toEntityHandle.convert(entity);

		// Clear old attributes and force a re-create
		entityLivingAttrMap.set(livingHandle, null);
		resetAttributes.invoke(livingHandle);
	}

	/**
	 * Loads the attributes for an Entity, applying the new attributes to the entity
	 * 
	 * @param entity to load
	 * @param data to load from
	 */
	public static void addAttributes(LivingEntity entity, CommonTagList data) {
		Object map = getAttributesMap.invoke(Conversion.toEntityHandle.convert(entity));
		loadAttributes.invoke(null, map, data.getHandle(), null);
	}

	/**
	 * Saves the current attributes of an Entity to a new CommonTagList
	 * 
	 * @param entity to save
	 * @return CommonTagList containing the saved data
	 */
	public static CommonTagList saveAttributes(LivingEntity entity) {
		Object map = getAttributesMap.invoke(Conversion.toEntityHandle.convert(entity));
		return (CommonTagList) CommonTag.create(saveAttributes.invoke(null, map));
	}
}
