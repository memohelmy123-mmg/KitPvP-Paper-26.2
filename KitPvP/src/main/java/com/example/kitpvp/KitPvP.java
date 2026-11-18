package com.example.kitpvp;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class KitPvP extends JavaPlugin {
    private KitManager kits;
    private StatsManager stats;
    private ScoreboardManager scoreboards;
    private CombatListener combat;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        stats = new StatsManager(this);
        kits = new KitManager(this);
        kits.load();
        scoreboards = new ScoreboardManager(this);
        combat = new CombatListener(this);

        var pm = getServer().getPluginManager();
        pm.registerEvents(combat, this);
        pm.registerEvents(new PlayerListener(this), this);
        pm.registerEvents(new MenuListener(this), this);

        KitPvPCommands cmds = new KitPvPCommands(this);
        for (String name : List.of("kit", "spawn", "stats", "top", "kitadmin")) {
            PluginCommand pc = getCommand(name);
            if (pc != null) {
                pc.setExecutor(cmds);
                pc.setTabCompleter(cmds);
            }
        }

        scoreboards.start();
        Bukkit.getScheduler().runTaskTimer(this, () -> stats.saveAll(), 6000L, 6000L); // autosave 5 min

        // support /reload: set up players who are already online
        for (Player p : Bukkit.getOnlinePlayers()) {
            stats.get(p);
            scoreboards.create(p);
        }
        getLogger().info("KitPvP enabled.");
    }

    @Override
    public void onDisable() {
        if (stats != null) stats.saveAll();
    }

    public KitManager kits() { return kits; }
    public StatsManager stats() { return stats; }
    public ScoreboardManager scoreboards() { return scoreboards; }
    public CombatListener combat() { return combat; }

    public Location getSpawn() {
        Location l = getConfig().getLocation("spawn");
        return l != null ? l : Bukkit.getWorlds().get(0).getSpawnLocation();
    }

    public void setSpawn(Location loc) {
        getConfig().set("spawn", loc);
        saveConfig();
    }

    public boolean isInSpawn(Player p) {
        Location s = getSpawn();
        Location l = p.getLocation();
        if (s.getWorld() == null || !s.getWorld().equals(l.getWorld())) return false;
        double r = getConfig().getDouble("spawn-radius", 15);
        return s.distanceSquared(l) <= r * r;
    }
}
