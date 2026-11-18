package com.example.kitpvp;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class StatsManager {
    private final KitPvP plugin;
    private final File file;
    private final YamlConfiguration yaml;
    private final Map<UUID, PlayerData> cache = new HashMap<>();

    public StatsManager(KitPvP plugin) {
        this.plugin = plugin;
        plugin.getDataFolder().mkdirs();
        this.file = new File(plugin.getDataFolder(), "data.yml");
        this.yaml = YamlConfiguration.loadConfiguration(file);
    }

    public PlayerData get(Player p) {
        return cache.computeIfAbsent(p.getUniqueId(), id -> load(p));
    }

    private PlayerData load(Player p) {
        PlayerData d = new PlayerData(p.getUniqueId());
        String path = "players." + d.uuid + ".";
        d.name = p.getName();
        if (!yaml.contains(path + "name")) {
            d.coins = plugin.getConfig().getInt("coins.starting", 0);
            return d;
        }
        d.kills = yaml.getInt(path + "kills");
        d.deaths = yaml.getInt(path + "deaths");
        d.streak = yaml.getInt(path + "streak");
        d.bestStreak = yaml.getInt(path + "beststreak");
        d.coins = yaml.getInt(path + "coins");
        d.selectedKit = yaml.getString(path + "kit");
        d.unlocked.addAll(yaml.getStringList(path + "unlocked"));
        return d;
    }

    private void write(PlayerData d) {
        String path = "players." + d.uuid + ".";
        yaml.set(path + "name", d.name);
        yaml.set(path + "kills", d.kills);
        yaml.set(path + "deaths", d.deaths);
        yaml.set(path + "streak", d.streak);
        yaml.set(path + "beststreak", d.bestStreak);
        yaml.set(path + "coins", d.coins);
        yaml.set(path + "kit", d.selectedKit);
        yaml.set(path + "unlocked", new ArrayList<>(d.unlocked));
    }

    private void flush() {
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save data.yml: " + e.getMessage());
        }
    }

    public void unload(Player p) {
        PlayerData d = cache.remove(p.getUniqueId());
        if (d != null) {
            d.name = p.getName();
            write(d);
            flush();
        }
    }

    public void saveAll() {
        for (PlayerData d : cache.values()) write(d);
        flush();
    }

    /** stat: kills, deaths, streak, coins */
    public List<Map.Entry<String, Integer>> top(String stat, int n) {
        for (PlayerData d : cache.values()) write(d);
        String key = stat.equals("streak") ? "beststreak" : stat;
        List<Map.Entry<String, Integer>> list = new ArrayList<>();
        ConfigurationSection sec = yaml.getConfigurationSection("players");
        if (sec != null) {
            for (String id : sec.getKeys(false)) {
                list.add(Map.entry(sec.getString(id + ".name", "?"), sec.getInt(id + "." + key)));
            }
        }
        list.sort((x, y) -> Integer.compare(y.getValue(), x.getValue()));
        return list.subList(0, Math.min(n, list.size()));
    }
}
