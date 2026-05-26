package com.bossscaler.config;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * TOML-based config for BossScaler.
 * Supports hot-reload via /bossscaler reload.
 */
public class BossScalerConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger("bossscaler");
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir().resolve("bossscaler.toml");

    private static BossScalerConfig instance;

    // ── Boss list ──────────────────────────────────────────────────────────
    public List<String> bossList = new ArrayList<>(List.of(
            "minecraft:wither",
            "minecraft:ender_dragon",
            "minecraft:warden",
            "minecraft:elder_guardian",
            "minecells:concierge",
            "minecells:conjunctivius",
            "illagerinvasion:invoker",
            "bosses_of_mass_destruction:lich",
            "bosses_of_mass_destruction:void_blossom",
            "twilightforest:naga",
            "twilightforest:lich",
            "twilightforest:minoshroom",
            "twilightforest:alpha_yeti",
            "twilightforest:knight_phantom",
            "twilightforest:hydra",
            "twilightforest:snow_queen",
            "twilightforest:ur_ghast",
            "reimaginingpotatoes:mega_spud",
            "aquamirae:captain_cornelia",
            "soulsweapons:draugr_boss",
            "soulsweapons:returning_knight",
            "soulsweapons:moonknight",
            "soulsweapons:chaos_monarch",
            "soulsweapons:accursed_lord_boss",
            "soulsweapons:night_prowler",
            "soulsweapons:day_stalker"
    ));

    // ── Scaling ────────────────────────────────────────────────────────────
    public double scalingRadius           = 36.0;
    public double hpMultiplier            = 0.75;

    // ── Damage ────────────────────────────────────────────────────────────
    public double damageReductionPerPlayer = 0.05;
    public double damageBonusPerPlayer     = 0.02;
    public double maxDamageReduction       = 0.40;
    public double maxDamageBonus           = 0.16;

    // ── Loot ──────────────────────────────────────────────────────────────
    public boolean sharedLoot       = true;
    public double  lootRadius       = 35.0;
    public boolean strictLootRadius = true;

    // ── Player timeout ────────────────────────────────────────────────────
    /** Seconds a scaled player must be outside scalingRadius before their HP contribution is removed (pre-combat only). */
    public int playerScaleTimeoutSeconds = 15;

    // ── Spell resistance (optional: requires Spell Engine and Spell Power Attributes mod) ──
    public boolean spellResistanceEnabled   = true;
    public double  spellResistanceBase      = 10.0;
    public double  spellResistancePerPlayer = 5.0;
    public double  spellResistanceMax       = 35.0;

    // ──────────────────────────────────────────────────────────────────────

    public static BossScalerConfig get() {
        if (instance == null) instance = load();
        return instance;
    }

    /** Reloads config from disk and replaces the singleton. */
    public static BossScalerConfig reload() {
        instance = load();
        LOGGER.info("[BossScaler] Config reloaded.");
        return instance;
    }

    public static BossScalerConfig load() {
        if (!CONFIG_PATH.toFile().exists()) {
            BossScalerConfig defaults = new BossScalerConfig();
            defaults.save();
            return defaults;
        }
        try {
            BossScalerConfig cfg = parse(CONFIG_PATH);
            LOGGER.info("[BossScaler] Config loaded from {}", CONFIG_PATH);
            return cfg;
        } catch (IOException e) {
            LOGGER.error("[BossScaler] Failed to load config, using defaults: {}", e.getMessage());
            return new BossScalerConfig();
        }
    }

    // ── TOML writer ───────────────────────────────────────────────────────

    public void save() {
        try (BufferedWriter w = Files.newBufferedWriter(CONFIG_PATH, StandardCharsets.UTF_8)) {
            w.write("# BossScaler Configuration\n");
            w.write("# All changes take effect after /bossscaler reload (or server restart).\n\n");

            w.write("[bosses]\n");
            w.write("# Entity IDs of mobs treated as bosses.\n");
            w.write("boss_list = [\n");
            for (int i = 0; i < bossList.size(); i++) {
                w.write("    \"" + bossList.get(i) + "\"");
                if (i < bossList.size() - 1) w.write(",");
                w.write("\n");
            }
            w.write("]\n\n");

            w.write("[scaling]\n");
            w.write("# Radius (blocks) within which a player causes HP scaling.\n");
            w.write("scaling_radius = " + scalingRadius + "\n");
            w.write("# HP multiplier added per extra player (e.g. 0.75 = +75% HP per extra player).\n");
            w.write("hp_multiplier = " + hpMultiplier + "\n");
            w.write("# Seconds a player must stay outside scaling_radius before their HP contribution is removed (pre-combat only).\n");
            w.write("player_scale_timeout_seconds = " + playerScaleTimeoutSeconds + "\n\n");

            w.write("[damage]\n");
            w.write("# Damage reduction applied to boss per extra player in range.\n");
            w.write("damage_reduction_per_player = " + damageReductionPerPlayer + "\n");
            w.write("# Damage bonus applied to boss attacks per extra player in range.\n");
            w.write("damage_bonus_per_player = " + damageBonusPerPlayer + "\n");
            w.write("# Maximum total damage reduction (fraction, e.g. 0.20 = 20%).\n");
            w.write("max_damage_reduction = " + maxDamageReduction + "\n");
            w.write("# Maximum total damage bonus (fraction).\n");
            w.write("max_damage_bonus = " + maxDamageBonus + "\n\n");

            w.write("[loot]\n");
            w.write("# If true, each qualifying player receives their own loot roll on boss death.\n");
            w.write("shared_loot = " + sharedLoot + "\n");
            w.write("# Radius (blocks) within which a player must be at boss death to qualify for loot.\n");
            w.write("loot_radius = " + lootRadius + "\n");
            w.write("# If true, a player must ALSO have attacked the boss to qualify for loot.\n");
            w.write("strict_loot_radius = " + strictLootRadius + "\n\n");

            w.write("[spell_resistance]\n");
            w.write("# Requires Spell Power Attributes mod to be present. No effect otherwise.\n");
            w.write("# If true, bosses gain spell resistance scaled to nearby player count.\n");
            w.write("enabled = " + spellResistanceEnabled + "\n");
            w.write("# Base resistance value with 1 player present.\n");
            w.write("base = " + spellResistanceBase + "\n");
            w.write("# Resistance added per extra player beyond the first.\n");
            w.write("per_player = " + spellResistancePerPlayer + "\n");
            w.write("# Maximum resistance value regardless of player count.\n");
            w.write("max = " + spellResistanceMax + "\n");

        } catch (IOException e) {
            LOGGER.error("[BossScaler] Failed to save config: {}", e.getMessage());
        }
    }

    // ── TOML parser  ───────────────────────────

    private static BossScalerConfig parse(Path path) throws IOException {
        BossScalerConfig cfg = new BossScalerConfig();
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);

        boolean inBossList = false;
        List<String> bossListAccum = new ArrayList<>();

        for (String raw : lines) {
            String line = raw.strip();

            if (!inBossList && (line.isEmpty() || line.startsWith("#"))) continue;

            if (inBossList) {
                if (line.equals("]") || line.equals("],")) {
                    inBossList = false;
                    cfg.bossList = new ArrayList<>(bossListAccum);
                } else {
                    String entry = line.replaceAll(",.*$", "").replace("\"", "").strip();
                    if (!entry.isEmpty() && !entry.startsWith("#")) {
                        bossListAccum.add(entry);
                    }
                }
                continue;
            }

            if (line.startsWith("[")) continue;

            if (!line.contains("=")) continue;
            String key = line.substring(0, line.indexOf('=')).strip();
            String val = line.substring(line.indexOf('=') + 1).strip();
            val = val.replaceAll("#.*$", "").strip();

            if (val.startsWith("[")) {
                if (val.endsWith("]")) {
                    cfg.bossList = parseInlineStringArray(val);
                } else {
                    inBossList = true;
                    bossListAccum = new ArrayList<>();
                    String rest = val.substring(1).strip();
                    if (!rest.isEmpty() && !rest.startsWith("#")) {
                        String entry = rest.replaceAll(",.*$", "").replace("\"", "").strip();
                        if (!entry.isEmpty()) bossListAccum.add(entry);
                    }
                }
                continue;
            }

            switch (key) {
                case "scaling_radius"               -> cfg.scalingRadius               = parseDouble(val, cfg.scalingRadius);
                case "hp_multiplier"                -> cfg.hpMultiplier                = parseDouble(val, cfg.hpMultiplier);
                case "player_scale_timeout_seconds" -> cfg.playerScaleTimeoutSeconds   = parseInt(val, cfg.playerScaleTimeoutSeconds);
                case "damage_reduction_per_player"  -> cfg.damageReductionPerPlayer    = parseDouble(val, cfg.damageReductionPerPlayer);
                case "damage_bonus_per_player"      -> cfg.damageBonusPerPlayer        = parseDouble(val, cfg.damageBonusPerPlayer);
                case "max_damage_reduction"         -> cfg.maxDamageReduction          = parseDouble(val, cfg.maxDamageReduction);
                case "max_damage_bonus"             -> cfg.maxDamageBonus              = parseDouble(val, cfg.maxDamageBonus);
                case "shared_loot"                  -> cfg.sharedLoot                  = Boolean.parseBoolean(val);
                case "loot_radius"                  -> cfg.lootRadius                  = parseDouble(val, cfg.lootRadius);
                case "strict_loot_radius"           -> cfg.strictLootRadius            = Boolean.parseBoolean(val);
                case "enabled"                      -> cfg.spellResistanceEnabled      = Boolean.parseBoolean(val);
                case "base"                         -> cfg.spellResistanceBase         = parseDouble(val, cfg.spellResistanceBase);
                case "per_player"                   -> cfg.spellResistancePerPlayer    = parseDouble(val, cfg.spellResistancePerPlayer);
                case "max"                          -> cfg.spellResistanceMax          = parseDouble(val, cfg.spellResistanceMax);
            }
        }
        return cfg;
    }

    private static List<String> parseInlineStringArray(String val) {
        String inner = val.replaceAll("^\\[|]$", "");
        List<String> result = new ArrayList<>();
        for (String part : inner.split(",")) {
            String s = part.replace("\"", "").strip();
            if (!s.isEmpty()) result.add(s);
        }
        return result;
    }

    private static double parseDouble(String val, double fallback) {
        try { return Double.parseDouble(val); }
        catch (NumberFormatException e) { return fallback; }
    }

    private static int parseInt(String val, int fallback) {
        try { return Integer.parseInt(val); }
        catch (NumberFormatException e) { return fallback; }
    }
}
