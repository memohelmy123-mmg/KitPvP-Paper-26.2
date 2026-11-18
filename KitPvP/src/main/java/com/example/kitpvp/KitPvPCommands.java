package com.example.kitpvp;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class KitPvPCommands implements TabExecutor {
    private final KitPvP plugin;

    public KitPvPCommands(KitPvP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command cmd, @NotNull String label, @NotNull String[] a) {
        switch (cmd.getName().toLowerCase()) {
            case "kit" -> kit(s, a);
            case "spawn" -> spawn(s);
            case "stats" -> stats(s, a);
            case "top" -> top(s, a);
            case "kitadmin" -> admin(s, a);
        }
        return true;
    }

    // ---------- /kit ----------
    private void kit(CommandSender s, String[] a) {
        if (!(s instanceof Player p)) {
            Text.send(s, "<red>Players only.");
            return;
        }
        if (a.length == 0) {
            new KitMenu(plugin).open(p);
            return;
        }
        Kit k = plugin.kits().get(a[0]);
        if (k == null) {
            Text.send(p, "<red>Unknown kit. Use <white>/kit<red> to see them all.");
            return;
        }
        plugin.kits().equip(p, k);
    }

    // ---------- /spawn ----------
    private void spawn(CommandSender s) {
        if (!(s instanceof Player p)) {
            Text.send(s, "<red>Players only.");
            return;
        }
        if (plugin.combat().isTagged(p)) {
            Text.send(p, "<red>You can't use /spawn while in combat!");
            return;
        }
        p.teleport(plugin.getSpawn());
        Text.send(p, "<green>Teleported to spawn.");
    }

    // ---------- /stats ----------
    private void stats(CommandSender s, String[] a) {
        Player t;
        if (a.length > 0) {
            t = Bukkit.getPlayerExact(a[0]);
            if (t == null) {
                Text.send(s, "<red>That player isn't online.");
                return;
            }
        } else if (s instanceof Player p) {
            t = p;
        } else {
            Text.send(s, "<red>Specify a player.");
            return;
        }
        PlayerData d = plugin.stats().get(t);
        s.sendMessage(Text.mm("<dark_gray>--- <red>Stats: " + t.getName() + "<dark_gray> ---"));
        s.sendMessage(Text.mm("<gray>Kills: <white>" + d.kills + "  <gray>Deaths: <white>" + d.deaths
                + "  <gray>K/D: <white>" + d.kdr()));
        s.sendMessage(Text.mm("<gray>Streak: <white>" + d.streak + " <gray>(best <white>" + d.bestStreak + "<gray>)"));
        s.sendMessage(Text.mm("<gray>Coins: <gold>" + d.coins));
    }

    // ---------- /top ----------
    private void top(CommandSender s, String[] a) {
        String stat = a.length > 0 ? a[0].toLowerCase() : "kills";
        if (!List.of("kills", "deaths", "streak", "coins").contains(stat)) stat = "kills";
        List<Map.Entry<String, Integer>> list = plugin.stats().top(stat, 10);
        s.sendMessage(Text.mm("<dark_gray>--- <red>Top " + stat + "<dark_gray> ---"));
        int i = 1;
        for (Map.Entry<String, Integer> e : list) {
            s.sendMessage(Text.mm("<gray>#" + i++ + " <white>" + e.getKey() + " <dark_gray>- <yellow>" + e.getValue()));
        }
    }

    // ---------- /kitadmin ----------
    private void admin(CommandSender s, String[] a) {
        if (!s.hasPermission("kitpvp.admin")) {
            Text.send(s, "<red>No permission.");
            return;
        }
        String sub = a.length > 0 ? a[0].toLowerCase() : "";
        switch (sub) {
            case "reload" -> {
                plugin.reloadConfig();
                plugin.kits().load();
                Text.send(s, "<green>Reloaded config and kits.");
            }
            case "setspawn" -> {
                if (!(s instanceof Player p)) {
                    Text.send(s, "<red>Players only.");
                    return;
                }
                plugin.setSpawn(p.getLocation());
                Text.send(p, "<green>Spawn set.");
            }
            case "givecoins" -> {
                if (a.length < 3) {
                    Text.send(s, "<red>/kitadmin givecoins <player> <amount>");
                    return;
                }
                Player t = Bukkit.getPlayerExact(a[1]);
                if (t == null) {
                    Text.send(s, "<red>Player not online.");
                    return;
                }
                try {
                    int amt = Integer.parseInt(a[2]);
                    plugin.stats().get(t).coins += amt;
                    Text.send(s, "<green>Gave " + amt + " coins to " + t.getName() + ".");
                } catch (NumberFormatException ex) {
                    Text.send(s, "<red>Invalid number.");
                }
            }
            case "unlock" -> {
                if (a.length < 3) {
                    Text.send(s, "<red>/kitadmin unlock <player> <kit>");
                    return;
                }
                Player t = Bukkit.getPlayerExact(a[1]);
                Kit k = plugin.kits().get(a[2]);
                if (t == null || k == null) {
                    Text.send(s, "<red>Unknown player or kit.");
                    return;
                }
                plugin.stats().get(t).unlocked.add(k.id);
                Text.send(s, "<green>Unlocked " + k.id + " for " + t.getName() + ".");
            }
            default -> Text.send(s, "<yellow>/kitadmin <reload|setspawn|givecoins|unlock>");
        }
    }

    // ---------- tab complete ----------
    @Override
    public List<String> onTabComplete(@NotNull CommandSender s, @NotNull Command cmd, @NotNull String label, @NotNull String[] a) {
        String n = cmd.getName().toLowerCase();
        if (n.equals("kit") && a.length == 1) return filter(plugin.kits().ids(), a[0]);
        if (n.equals("top") && a.length == 1) return filter(List.of("kills", "deaths", "streak", "coins"), a[0]);
        if (n.equals("stats") && a.length == 1) return filter(players(), a[0]);
        if (n.equals("kitadmin")) {
            if (a.length == 1) return filter(List.of("reload", "setspawn", "givecoins", "unlock"), a[0]);
            if (a.length == 2 && (a[0].equalsIgnoreCase("givecoins") || a[0].equalsIgnoreCase("unlock")))
                return filter(players(), a[1]);
            if (a.length == 3 && a[0].equalsIgnoreCase("unlock")) return filter(plugin.kits().ids(), a[2]);
        }
        return List.of();
    }

    private List<String> players() {
        return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
    }

    private List<String> filter(Collection<String> opts, String prefix) {
        return opts.stream().filter(o -> o.toLowerCase().startsWith(prefix.toLowerCase())).sorted().toList();
    }
}
