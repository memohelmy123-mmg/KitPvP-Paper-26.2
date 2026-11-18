package com.example.kitpvp;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class PlayerListener implements Listener {
    private final KitPvP plugin;

    public PlayerListener(KitPvP plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        e.joinMessage(null);
        PlayerData d = plugin.stats().get(p);

        p.teleport(plugin.getSpawn());
        p.getInventory().clear();

        Kit k = plugin.kits().get(d.selectedKit);
        if (k != null && plugin.kits().isUnlocked(p, d, k)) {
            plugin.kits().apply(p, k);
        } else {
            Text.send(p, "<yellow>Use <gold>/kit<yellow> to pick a kit!");
        }
        plugin.scoreboards().create(p);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        e.setRespawnLocation(plugin.getSpawn());
        Player p = e.getPlayer();
        if (!plugin.getConfig().getBoolean("auto-kit-on-respawn", true)) return;

        Bukkit.getScheduler().runTask(plugin, () -> {
            PlayerData d = plugin.stats().get(p);
            Kit k = plugin.kits().get(d.selectedKit);
            if (k != null && plugin.kits().isUnlocked(p, d, k)) plugin.kits().apply(p, k);
        });
    }

    @EventHandler
    public void onHunger(FoodLevelChangeEvent e) {
        if (plugin.getConfig().getBoolean("no-hunger", true) && e.getEntity() instanceof Player) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        if (!e.getPlayer().hasPermission("kitpvp.build")) e.setCancelled(true);
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        if (!e.getPlayer().hasPermission("kitpvp.build")) e.setCancelled(true);
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        if (!e.getPlayer().hasPermission("kitpvp.build")) e.setCancelled(true);
    }
}
