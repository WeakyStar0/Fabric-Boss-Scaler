# Boss Scaler

A Fabric mod for Minecraft 1.20.1 that makes boss fights dynamically scale to the number of nearby players — rewarding group play without making solo runs trivial.

## Features

- **HP scaling** — Boss max health scales up as more players enter the fight radius. Each additional player adds a configurable percentage of the boss's base HP.
- **Damage reduction** — The boss takes less damage per hit when more players are present, keeping the fight from ending too quickly in large groups.
- **Damage bonus** — The boss deals more damage to players when more are nearby, raising the stakes.
- **Per-player loot** — When a boss dies, each qualifying player receives their own independent loot roll (togglable).
- **Smart loot qualification** — Players must have attacked the boss and/or be within loot radius at time of death to receive drops (configurable).
- **Pre-combat player timeout** — If a player triggers HP scaling by approaching a boss but then walks away before anyone attacks, their HP contribution is removed after a configurable delay. This prevents accidental scaling from passersby.
- **Hot config reload** — `/bossscaler reload` applies config changes without a server restart.

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) for Minecraft 1.20.1.
2. Install [Fabric API](https://modrinth.com/mod/fabric-api).
3. Drop `bossscaler-<version>.jar` into your `mods/` folder.

## Configuration

The config file is created automatically at `config/bossscaler.toml` on first run. All values can be changed and applied live with `/bossscaler reload`.

```toml
[bosses]
# Entity IDs of mobs treated as bosses.
boss_list = [
    "minecraft:warden",
    "minecraft:elder_guardian",
    "minecraft:ender_dragon",
    "minecraft:wither"
]

[scaling]
# Radius (blocks) within which a player causes HP scaling.
scaling_radius = 36.0
# HP multiplier added per extra player (e.g. 0.75 = +75% HP per extra player).
hp_multiplier = 0.75
# Seconds a player must stay outside scaling_radius before their HP contribution
# is removed (pre-combat only — once the boss is attacked, scaling locks in).
player_scale_timeout_seconds = 15

[damage]
# Damage reduction applied to boss per extra player in range.
damage_reduction_per_player = 0.02
# Damage bonus applied to boss attacks per extra player in range.
damage_bonus_per_player = 0.02
# Maximum total damage reduction (fraction, e.g. 0.20 = 20%).
max_damage_reduction = 0.20
# Maximum total damage bonus (fraction).
max_damage_bonus = 0.12

[loot]
# If true, each qualifying player receives their own loot roll on boss death.
shared_loot = true
# Radius (blocks) within which a player must be at boss death to qualify for loot.
loot_radius = 35.0
# If true, a player must ALSO have attacked the boss to qualify for loot.
strict_loot_radius = true
```

### Adding modded bosses

Add the entity's namespaced ID to `boss_list`:

```toml
boss_list = [
    "minecraft:warden",
    "minecraft:ender_dragon",
    "minecraft:wither",
    "minecraft:elder_guardian",
    "alexsmobs:void_worm",
    "iceandfire:fire_dragon"
]
```

## Commands

| Command | Permission | Description |
|---|---|---|
| `/bossscaler reload` | OP level 2 | Reloads `bossscaler.toml` from disk |

## How scaling works

### HP scaling

When a player comes within `scaling_radius` of a boss, the boss gains `hp_multiplier × base_hp` extra health. A second player joining doubles that bonus, a third triples it, and so on.

If no one has attacked yet and the player leaves for longer than `player_scale_timeout_seconds`, their contribution is removed and the boss HP adjusts back down. Once the fight starts (first hit), the scaling locks in — players can't cheese it by retreating.

### Damage

With N players in range, the boss takes `(N-1) × damage_reduction_per_player` less damage per hit (capped at `max_damage_reduction`), and deals `(N-1) × damage_bonus_per_player` extra damage to players (capped at `max_damage_bonus`).

### Loot

With `shared_loot = true` and more than one qualifying player, vanilla loot is suppressed and each qualifying player gets their own loot table roll, dropped at their position. With `strict_loot_radius = true`, players must both have hit the boss and be within `loot_radius` at the time of death.

With `shared_loot = false`, the vanilla single drop runs as normal.

## License

MIT — see LICENSE for details.
