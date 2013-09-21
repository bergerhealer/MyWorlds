package com.bergerkiller.bukkit.mw.version;

import org.bukkit.entity.LivingEntity;

import com.bergerkiller.bukkit.common.Common;
import com.bergerkiller.bukkit.common.nbt.CommonTagList;
import com.bergerkiller.bukkit.common.reflection.MethodAccessor;
import com.bergerkiller.bukkit.common.reflection.SafeMethod;
import com.bergerkiller.bukkit.common.utils.NBTUtil;

public class BKCAttributes implements AttributesUtil {
	private MethodAccessor<Void> resetAttributes;
	private MethodAccessor<Void> loadAttributes;
	private MethodAccessor<CommonTagList> saveAttributes;
	private final boolean valid;

	public BKCAttributes() {
		this.valid = Common.getVersion() >= 155;
		if (this.valid) {
			resetAttributes = new SafeMethod<Void>(NBTUtil.class, "resetAttributes", LivingEntity.class);
			loadAttributes = new SafeMethod<Void>(NBTUtil.class, "loadAttributes", LivingEntity.class, CommonTagList.class);
			saveAttributes = new SafeMethod<CommonTagList>(NBTUtil.class, "saveAttributes", LivingEntity.class);
		}
	}

	@Override
	public boolean isValid() {
		return this.valid;
	}

	@Override
	public void resetAttributes(LivingEntity entity) {
		this.resetAttributes.invoke(null, entity);
	}

	@Override
	public void addAttributes(LivingEntity entity, CommonTagList data) {
		this.loadAttributes.invoke(null, entity, data);
	}

	@Override
	public CommonTagList saveAttributes(LivingEntity entity) {
		return this.saveAttributes.invoke(null, entity);
	}
}
