package com.example.kitpvp;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ScoreboardManager {
    private static final int LINES = 8;
    private final KitPvP plugin;
    private final Map<UUID, Scoreboard> boards = new HashMap<>();

    public ScoreboardManager(KitPvP plugin) {
        this.plugin = plugin;
    }

    private boolean enabled() {
        return plugin.getConfig().getBoolean("scoreboard.enabled", true);
    }

    public void start() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) update(p);
        }, 20L, 20L);
    }

    public void create(Player p) {
        if (!enabled()) return;
        Scoreboard sb = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = sb.registerNewObjective("kitpvp", Criteria.DUMMY,
                Text.mm(plugin.getConfig().getString("scoreboard.title", "<red><bold>KITPVP")));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        for (int i = 0; i < LINES; i++) {
            String entry = "\u00A7" + Integer.toHexString(i);
            Team t = sb.registerNewTeam("line" + i);
            t.addEntry(entry);
            obj.getScore(entry).setScore(LINES - i);
        }
        boards.put(p.getUniqueId(), sb);
        p.setScoreboard(sb);
        update(p);
    }

    public void remove(Player p) {
        boards.remove(p.getUniqueId());
        p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    public void update(Player p) {
        if (!enabled()) return;
        Scoreboard sb = boards.get(p.getUniqueId());
        if (sb == null) {
            create(p);
            return;
        }
        PlayerData d = plugin.stats().get(p);
        Kit k = plugin.kits().get(d.selectedKit);
        String kitName = k == null ? "None" : Text.strip(k.display);

        String[] lines = {
                "",
                "<gray>Kills: <white>" + d.kills,
                "<gray>Deaths: <white>" + d.deaths,
                "<gray>Streak: <white>" + d.streak,
                "<gray>Coins: <gold>" + d.coins,
                "<gray>Kit: <white>" + kitName,
                "",
                plugin.getConfig().getString("scoreboard.footer", "<yellow>play.yourserver.net")
        };
        for (int i = 0; i < LINES; i++) {
            Team t = sb.getTeam("line" + i);
            if (t != null) t.prefix(Text.mm(lines[i]));
        }
    }
}
