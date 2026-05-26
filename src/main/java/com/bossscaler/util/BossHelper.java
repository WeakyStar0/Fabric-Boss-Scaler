package com.bossscaler.util;

import com.bossscaler.config.BossScalerConfig;
import net.minecraft.entity.LivingEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public class BossHelper {

    public static boolean isBoss(LivingEntity entity) {
        Identifier entityId = Registries.ENTITY_TYPE.getId(entity.getType());
        return BossScalerConfig.get().bossList.contains(entityId.toString());
    }
}
