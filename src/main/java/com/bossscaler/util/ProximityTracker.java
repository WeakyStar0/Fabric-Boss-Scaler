package com.bossscaler.util;

import com.bossscaler.compat.SpellResistanceCompat;
import com.bossscaler.config.BossScalerConfig;
import com.bossscaler.data.BossState;
import com.bossscaler.data.BossStateManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.*;

public class ProximityTracker {

    /** Monotonically increasing tick counter driven by the server tick event. */
    private static long serverTick = 0;

    public static void tick(ServerWorld world) {
        serverTick++;

        BossScalerConfig config = BossScalerConfig.get();
        double scalingRadius = config.scalingRadius;
        double lootRadius    = config.lootRadius;
        // The tracker runs once per second, so 1 tracker tick ≈ 1 real second.
        long timeoutTicks    = Math.max(1, config.playerScaleTimeoutSeconds);

        world.iterateEntities().forEach(entity -> {
            if (!(entity instanceof LivingEntity boss)) return;
            if (!BossHelper.isBoss(boss)) return;
            if (!boss.isAlive()) return;

            UUID bossId     = boss.getUuid();
            float currentMax = (float) boss.getAttributeValue(EntityAttributes.GENERIC_MAX_HEALTH);
            BossState state  = BossStateManager.getOrCreate(bossId, currentMax);

            List<ServerPlayerEntity> nearbyScaling = world.getPlayers(
                    p -> p.squaredDistanceTo(boss) <= scalingRadius * scalingRadius
            );
            Set<UUID> nearbyIds = new HashSet<>();
            for (ServerPlayerEntity p : nearbyScaling) nearbyIds.add(p.getUuid());

            boolean hpChanged = false;

            // ── New players entering the radius ──────────────────────────
            for (ServerPlayerEntity player : nearbyScaling) {
                UUID pid = player.getUuid();
                if (!state.hasScaled(pid)) {
                    state.addScaledPlayer(pid);
                    hpChanged = true;
                } else {
                    state.clearOutOfRange(pid);
                }
            }

            // ── Handle players who left the radius ───────────────────────
            if (!state.hasBeenAttacked()) {
                for (UUID scaled : state.getScaledPlayers()) {
                    if (!nearbyIds.contains(scaled)) {
                        state.recordOutOfRange(scaled, serverTick);
                    }
                }

                Set<UUID> timedOut = state.getTimedOutPlayers(serverTick, timeoutTicks);
                for (UUID pid : timedOut) {
                    state.removeScaledPlayer(pid);
                    hpChanged = true;
                }
            }

            if (hpChanged) {
                applyScaledHealth(boss, state);
                // Spell resistance mirrors HP scaling player count
                SpellResistanceCompat.applyResistance(boss, state.getScaledPlayerCount());
            }

            // ── Update loot-eligible players ─────────────────────────────
            if (config.strictLootRadius) {
                Set<UUID> activeLoot = new HashSet<>();
                List<ServerPlayerEntity> nearbyLoot = world.getPlayers(
                        p -> p.squaredDistanceTo(boss) <= lootRadius * lootRadius
                );
                for (ServerPlayerEntity player : nearbyLoot) {
                    if (state.hasAttacked(player.getUuid())) activeLoot.add(player.getUuid());
                }
                state.setActiveLootPlayers(activeLoot);
            }
        });
    }

    public static void applyScaledHealth(LivingEntity boss, BossState state) {
        float base        = state.getBaseMaxHealth();
        int players       = state.getScaledPlayerCount();
        double multiplier = BossScalerConfig.get().hpMultiplier;

        float newMax = (float) (base * (1.0 + multiplier * Math.max(0, players - 1)));
        float oldMax = (float) boss.getAttributeValue(EntityAttributes.GENERIC_MAX_HEALTH);
        float diff   = newMax - oldMax;

        var instance = boss.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (instance != null) instance.setBaseValue(newMax);

        if (diff > 0) {
            boss.heal(diff);
        } else if (diff < 0) {
            if (boss.getHealth() > newMax) {
                boss.setHealth(newMax);
            }
        }
    }

    public static int countPlayersInScalingRange(LivingEntity boss, ServerWorld world) {
        double radius = BossScalerConfig.get().scalingRadius;
        return world.getPlayers(p -> p.squaredDistanceTo(boss) <= radius * radius).size();
    }
}
