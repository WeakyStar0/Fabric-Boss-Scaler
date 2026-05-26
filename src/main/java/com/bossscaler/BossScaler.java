package com.bossscaler;

import com.bossscaler.command.BossScalerCommand;
import com.bossscaler.config.BossScalerConfig;
import com.bossscaler.data.BossState;
import com.bossscaler.data.BossStateManager;
import com.bossscaler.util.BossHelper;
import com.bossscaler.compat.SpellResistanceCompat;
import com.bossscaler.util.ProximityTracker;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BossScaler implements ModInitializer {

    public static final String MOD_ID = "bossscaler";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final int TICK_INTERVAL = 20;

    /**
     * Per-entity reentrance guards to prevent infinite damage() loops.
     * Using a Set of UUIDs rather than static booleans so two different bosses
     * being damaged in the same tick don't block each other.
     */
    private static final Set<UUID> processingBossDamage   = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Set<UUID> processingPlayerDamage  = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private int serverTickCounter = 0;

    @Override
    public void onInitialize() {
        LOGGER.info("[BossScaler] Initializing...");

        BossScalerConfig.get();
        SpellResistanceCompat.init();

        // ── Register /bossscaler reload ───────────────────────────────────
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                BossScalerCommand.register(dispatcher));

        // ── Server tick: proximity tracking once per second across all worlds ─
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (++serverTickCounter >= TICK_INTERVAL) {
                serverTickCounter = 0;
                for (ServerWorld world : server.getWorlds()) {
                    ProximityTracker.tick(world);
                }
            }
        });

        // ── Track attacks + apply damage reduction to bosses ──────────────
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!BossHelper.isBoss(entity)) return true;
            UUID bossId = entity.getUuid();
            if (processingBossDamage.contains(bossId)) return true; // break recursion
            if (!(entity.getWorld() instanceof ServerWorld serverWorld)) return true;

            float baseHealth = (float) entity.getAttributeValue(
                    net.minecraft.entity.attribute.EntityAttributes.GENERIC_MAX_HEALTH);
            BossState state = BossStateManager.getOrCreate(bossId, baseHealth);

            if (source.getAttacker() instanceof PlayerEntity player) {
                state.addAttackedPlayer(player.getUuid());
                state.markAttacked();
            }

            int playersInRange = ProximityTracker.countPlayersInScalingRange(entity, serverWorld);
            if (playersInRange > 1) {
                double reduction = BossScalerConfig.get().damageReductionPerPlayer * (playersInRange - 1);
                reduction = Math.min(reduction, BossScalerConfig.get().maxDamageReduction);
                float reducedAmount = amount * (float)(1.0 - reduction);
                processingBossDamage.add(bossId);
                try {
                    entity.damage(source, reducedAmount);
                } finally {
                    processingBossDamage.remove(bossId);
                }
                return false;
            }

            return true;
        });

        // ── Apply damage bonus when boss hits a player ────────────────────
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayerEntity)) return true;
            if (!(source.getAttacker() instanceof LivingEntity attacker)) return true;
            if (!BossHelper.isBoss(attacker)) return true;
            UUID bossId = attacker.getUuid();
            if (processingPlayerDamage.contains(bossId)) return true; // break recursion
            if (!(attacker.getWorld() instanceof ServerWorld serverWorld)) return true;

            int playersInRange = ProximityTracker.countPlayersInScalingRange(attacker, serverWorld);
            if (playersInRange <= 1) return true;

            double bonus = BossScalerConfig.get().damageBonusPerPlayer * (playersInRange - 1);
            bonus = Math.min(bonus, BossScalerConfig.get().maxDamageBonus);
            float boostedAmount = amount * (float)(1.0 + bonus);
            processingPlayerDamage.add(bossId);
            try {
                entity.damage(source, boostedAmount);
            } finally {
                processingPlayerDamage.remove(bossId);
            }
            return false;
        });

        LOGGER.info("[BossScaler] Ready! Bosses: {}", BossScalerConfig.get().bossList);
    }
}
