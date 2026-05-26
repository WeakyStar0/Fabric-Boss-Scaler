package com.bossscaler.data;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BossStateManager {

    private static final ConcurrentHashMap<UUID, BossState> states = new ConcurrentHashMap<>();

    public static boolean has(UUID id)                          { return states.containsKey(id); }
    public static BossState get(UUID id)                        { return states.get(id); }

    public static BossState getOrCreate(UUID id, float baseMaxHealth) {
        return states.computeIfAbsent(id, k -> new BossState(baseMaxHealth));
    }

    public static void remove(UUID id) { states.remove(id); }
    public static void clear()         { states.clear(); }
}
