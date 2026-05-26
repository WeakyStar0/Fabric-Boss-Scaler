package com.bossscaler.data;

import java.util.*;

public class BossState {

    private final float baseMaxHealth;
    private boolean hasBeenAttacked = false;

    private final Set<UUID> scaledPlayers  = new HashSet<>();
    private final Set<UUID> attackedPlayers = new HashSet<>();
    private final Set<UUID> activeLootPlayers = new HashSet<>();

    /**
     * Tracks the server tick at which a scaled player first left the scaling radius.
     * Only relevant before combat (hasBeenAttacked == false).
     * Cleared when the player re-enters the radius.
     */
    private final Map<UUID, Long> outOfRangeSince = new HashMap<>();

    public BossState(float baseMaxHealth) {
        this.baseMaxHealth = baseMaxHealth;
    }

    // ── Base health ───────────────────────────────────────────────────────

    public float getBaseMaxHealth() { return baseMaxHealth; }

    // ── Combat flag ───────────────────────────────────────────────────────

    public boolean hasBeenAttacked() { return hasBeenAttacked; }
    public void markAttacked() { this.hasBeenAttacked = true; }

    // ── Scaled players ────────────────────────────────────────────────────

    public Set<UUID> getScaledPlayers() { return Collections.unmodifiableSet(scaledPlayers); }
    public boolean hasScaled(UUID id)   { return scaledPlayers.contains(id); }
    public int getScaledPlayerCount()   { return scaledPlayers.size(); }

    public void addScaledPlayer(UUID id) {
        scaledPlayers.add(id);
        outOfRangeSince.remove(id); // reset timeout if they came back
    }

    /**
     * Remove a player's HP contribution. Safe to call even if they were never scaled.
     */
    public void removeScaledPlayer(UUID id) {
        scaledPlayers.remove(id);
        outOfRangeSince.remove(id);
    }

    // ── Out-of-range timeout tracking ─────────────────────────────────────

    /**
     * Record the tick at which a scaled player left the radius.
     * Does nothing if they were not scaled or are already being tracked.
     */
    public void recordOutOfRange(UUID id, long currentTick) {
        if (scaledPlayers.contains(id) && !outOfRangeSince.containsKey(id)) {
            outOfRangeSince.put(id, currentTick);
        }
    }

    /**
     * If a player re-entered the radius, clear their timeout.
     */
    public void clearOutOfRange(UUID id) {
        outOfRangeSince.remove(id);
    }

    /**
     * Returns UUIDs of scaled players whose out-of-range duration exceeds the threshold.
     */
    public Set<UUID> getTimedOutPlayers(long currentTick, long timeoutTicks) {
        Set<UUID> timedOut = new HashSet<>();
        for (Map.Entry<UUID, Long> entry : outOfRangeSince.entrySet()) {
            if (currentTick - entry.getValue() >= timeoutTicks) {
                timedOut.add(entry.getKey());
            }
        }
        return timedOut;
    }

    // ── Attacked players ──────────────────────────────────────────────────

    public Set<UUID> getAttackedPlayers()   { return Collections.unmodifiableSet(attackedPlayers); }
    public boolean hasAttacked(UUID id)     { return attackedPlayers.contains(id); }
    public void addAttackedPlayer(UUID id) {
        attackedPlayers.add(id);
        outOfRangeSince.remove(id); // reset timeout if they're still contributing from range
    }

    // ── Active loot players ───────────────────────────────────────────────

    public Set<UUID> getActiveLootPlayers() { return Collections.unmodifiableSet(activeLootPlayers); }
    public void setActiveLootPlayers(Set<UUID> players) {
        activeLootPlayers.clear();
        activeLootPlayers.addAll(players);
    }
}
