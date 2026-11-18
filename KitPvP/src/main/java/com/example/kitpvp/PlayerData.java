package com.example.kitpvp;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PlayerData {
    public final UUID uuid;
    public String name;
    public int kills, deaths, streak, bestStreak, coins;
    public String selectedKit;
    public final Set<String> unlocked = new HashSet<>();
    public final Map<String, Long> cooldowns = new HashMap<>(); // not saved

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
    }

    public double kdr() {
        return deaths == 0 ? kills : Math.round((double) kills / deaths * 100.0) / 100.0;
    }
}
