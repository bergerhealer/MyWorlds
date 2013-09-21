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
public class NMSAttributes implements AttributesUtil {
	private Class<?> entityLivingType = CommonUtil.getNMSClass("EntityLiving");
	private Class<?> genericAttributesType = CommonUtil.getNMSClass("GenericAttributes");
	private Class<?> attributeMapBaseType = CommonUtil.getNMSClass("AttributeMapBase");
	private Class<?> iConsoleLogManagerType = CommonUtil.getNMSClass("IConsoleLogManager");
	private Class<?> nbtTagListType = CommonUtil.getNMSClass("NBTTagList");
	private FieldAccessor<Object> entityLivingAttrMap = new SafeField<Object>(entityLivingType, "d");
	private MethodAccessor<Object> getAttributesMap = new SafeMethod<Object>(entityLivingType, "aW");
	private MethodAccessor<Void> resetAttributes = new SafeMethod<Void>(entityLivingType, "ay");
	private MethodAccessor<Object> saveAttributes = new SafeMethod<Object>(genericAttributesType, "a", attributeMapBaseType);
	private MethodAccessor<Void> loadAttributes = new SafeMethod<Void>(genericAttributesType, "a", attributeMapBaseType, nbtTagListType, iConsoleLogManagerType);

	@Override
	public boolean isValid() {
		return getAttributesMap.isValid() && resetAttributes.isValid() && 
				saveAttributes.isValid() && loadAttributes.isValid() &&
				entityLivingAttrMap.isValid();
	}

	@Override
	public void resetAttributes(LivingEntity entity) {
		Object livingHandle = Conversion.toEntityHandle.convert(entity);

		// Clear old attributes and force a re-create
		entityLivingAttrMap.set(livingHandle, null);
		resetAttributes.invoke(livingHandle);
	}

	@Override
	public void addAttributes(LivingEntity entity, CommonTagList data) {
		Object map = getAttributesMap.invoke(Conversion.toEntityHandle.convert(entity));
		loadAttributes.invoke(null, map, data.getHandle(), null);
	}

	@Override
	public CommonTagList saveAttributes(LivingEntity entity) {
		Object map = getAttributesMap.invoke(Conversion.toEntityHandle.convert(entity));
		return (CommonTagList) CommonTag.create(saveAttributes.invoke(null, map));
	}
}
