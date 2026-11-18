package com.example.kitpvp;

import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CombatListener implements Listener {
    private final KitPvP plugin;
    private final Map<UUID, Long> tagUntil = new HashMap<>();
    private final Map<UUID, UUID> lastAttacker = new HashMap<>();

    public CombatListener(KitPvP plugin) {
        this.plugin = plugin;
    }

    public boolean isTagged(Player p) {
        Long t = tagUntil.get(p.getUniqueId());
        return t != null && t > System.currentTimeMillis();
    }

    private void tag(Player p, Player other) {
        long ms = plugin.getConfig().getInt("combat-tag-seconds", 10) * 1000L;
        tagUntil.put(p.getUniqueId(), System.currentTimeMillis() + ms);
        lastAttacker.put(p.getUniqueId(), other.getUniqueId());
    }

    // ---------- damage ----------

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player victim)) return;

        if (e.getCause() != DamageCause.VOID && plugin.isInSpawn(victim)) {
            e.setCancelled(true);
            return;
        }
        if (e instanceof EntityDamageByEntityEvent by) {
            Player attacker = null;
            if (by.getDamager() instanceof Player p) attacker = p;
            else if (by.getDamager() instanceof Projectile pr && pr.getShooter() instanceof Player p) attacker = p;

            if (attacker == null || attacker.equals(victim)) return;
            if (plugin.isInSpawn(attacker)) {
                e.setCancelled(true);
                return;
            }
            tag(attacker, victim);
            tag(victim, attacker);
        }
    }

    // ---------- death ----------

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent e) {
        Player victim = e.getEntity();
        e.getDrops().clear();
        e.setDroppedExp(0);
        e.deathMessage(null);

        Player killer = victim.getKiller();
        if (killer == null && isTagged(victim)) {
            UUID id = lastAttacker.get(victim.getUniqueId());
            if (id != null) killer = Bukkit.getPlayer(id);
        }
        handleKill(killer, victim);

        tagUntil.remove(victim.getUniqueId());
        lastAttacker.remove(victim.getUniqueId());
        Bukkit.getScheduler().runTask(plugin, () -> victim.spigot().respawn());
    }

    private void handleKill(Player killer, Player victim) {
        StatsManager stats = plugin.stats();
        PlayerData vd = stats.get(victim);
        int lostStreak = vd.streak;
        vd.deaths++;
        vd.streak = 0;

        if (killer == null || killer.equals(victim)) {
            Text.send(victim, "<red>You died.");
            return;
        }

        var cfg = plugin.getConfig();
        PlayerData kd = stats.get(killer);
        kd.kills++;
        kd.streak++;
        kd.bestStreak = Math.max(kd.bestStreak, kd.streak);

        int bonusLevels = Math.min(kd.streak - 1, cfg.getInt("coins.max-streak-bonus-kills", 10));
        int reward = cfg.getInt("coins.per-kill", 10) + bonusLevels * cfg.getInt("coins.streak-bonus", 2);
        kd.coins += reward;

        Text.send(killer, "<green>You killed <white>" + victim.getName() + " <gray>(+<gold>" + reward + "<gray> coins)");
        Text.send(victim, "<red>You were killed by <white>" + killer.getName() + "<red>.");

        if (cfg.getBoolean("heal-on-kill", true)) {
            killer.setHealth(killer.getAttribute(Attribute.MAX_HEALTH).getValue());
        }

        int every = cfg.getInt("killstreak.announce-every", 5);
        if (every > 0 && kd.streak % every == 0) {
            Bukkit.broadcast(Text.mm(Text.PREFIX + "<gold>" + killer.getName() + " <yellow>is on a <gold>"
                    + kd.streak + " <yellow>killstreak!"));
        }
        if (every > 0 && lostStreak >= every) {
            Bukkit.broadcast(Text.mm(Text.PREFIX + "<gold>" + killer.getName() + " <yellow>ended <gold>"
                    + victim.getName() + "<yellow>'s <gold>" + lostStreak + " <yellow>killstreak!"));
        }
    }

    // ---------- quit (combat log) ----------

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        e.quitMessage(null);

        if (isTagged(p)) {
            UUID id = lastAttacker.get(p.getUniqueId());
            Player killer = id == null ? null : Bukkit.getPlayer(id);
            Bukkit.broadcast(Text.mm(Text.PREFIX + "<red>" + p.getName() + " combat logged!"));
            p.getInventory().clear();
            handleKill(killer, p);
            p.teleport(plugin.getSpawn());
        }
        tagUntil.remove(p.getUniqueId());
        lastAttacker.remove(p.getUniqueId());

        plugin.scoreboards().remove(p);
        plugin.stats().unload(p);
    }
}
