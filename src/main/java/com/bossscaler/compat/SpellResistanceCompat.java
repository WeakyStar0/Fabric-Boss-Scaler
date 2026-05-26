package com.bossscaler.compat;

import com.bossscaler.BossScaler;
import com.bossscaler.config.BossScalerConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.UUID;

/**
 * Optional integration with Spell Power Attributes (spell_power mod).
 * Scales boss spell resistance based on the number of nearby players.
 *
 * This class never imports anything from spell_power at compile time —
 * it resolves the attribute purely by registry ID, so the mod loads
 * fine whether or not Spell Power is present.
 */
public class SpellResistanceCompat {

    /** Same UUID every time so we can find and replace our own modifier. */
    private static final UUID MODIFIER_UUID = UUID.fromString("a3f2c1d0-beef-4b0s-8c3e-bossscaler001");
    private static final String MODIFIER_NAME = "bossscaler:spell_resistance";
    private static final Identifier RESISTANCE_ID = new Identifier("spell_power", "resistance.generic");

    private static boolean available = false;
    private static EntityAttribute resistanceAttribute = null;

    /**
     * Call once during mod init. Checks if spell_power is loaded and caches
     * the attribute reference. Safe to call even if the mod is absent.
     */
    public static void init() {
        if (!FabricLoader.getInstance().isModLoaded("spell_power")) {
            BossScaler.LOGGER.info("[BossScaler] Spell Power not found — magic resistance scaling disabled.");
            return;
        }

        EntityAttribute attr = Registries.ATTRIBUTE.get(RESISTANCE_ID);
        if (attr == null) {
            BossScaler.LOGGER.warn("[BossScaler] Spell Power is loaded but '{}' attribute not found in registry. " +
                    "Magic resistance scaling disabled.", RESISTANCE_ID);
            return;
        }

        resistanceAttribute = attr;
        available = true;
        BossScaler.LOGGER.info("[BossScaler] Spell Power detected — magic resistance scaling enabled.");
    }

    public static boolean isAvailable() {
        return available;
    }

    /**
     * Apply scaled magic resistance to a boss based on player count.
     * Formula: base + (perPlayer × (players - 1)), capped at max.
     *
     * Uses a single ADDITION modifier keyed by MODIFIER_UUID so repeated
     * calls simply replace the previous value rather than stacking.
     *
     * Safe to call when Spell Power is absent (no-op).
     */
    public static void applyResistance(LivingEntity boss, int scaledPlayerCount) {
        if (!available) return;

        BossScalerConfig cfg = BossScalerConfig.get();
        if (!cfg.spellResistanceEnabled) return;

        EntityAttributeInstance instance = boss.getAttributeInstance(resistanceAttribute);
        if (instance == null) return; // boss type doesn't have this attribute registered

        // Remove old modifier if present
        instance.removeModifier(MODIFIER_UUID);

        double value = cfg.spellResistanceBase
                + cfg.spellResistancePerPlayer * Math.max(0, scaledPlayerCount - 1);
        value = Math.min(value, cfg.spellResistanceMax);

        instance.addPersistentModifier(new EntityAttributeModifier(
                MODIFIER_UUID,
                MODIFIER_NAME,
                value,
                EntityAttributeModifier.Operation.ADDITION
        ));
    }

    /**
     * Remove our modifier from a boss (e.g. on death / state cleanup).
     * Safe to call when Spell Power is absent (no-op).
     */
    public static void removeResistance(LivingEntity boss) {
        if (!available) return;
        EntityAttributeInstance instance = boss.getAttributeInstance(resistanceAttribute);
        if (instance != null) instance.removeModifier(MODIFIER_UUID);
    }
}
