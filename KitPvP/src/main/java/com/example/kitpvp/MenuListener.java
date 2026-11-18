package com.example.kitpvp;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public class MenuListener implements Listener {
    private final KitPvP plugin;

    public MenuListener(KitPvP plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof KitMenu) e.setCancelled(true);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof KitMenu menu)) return;
        e.setCancelled(true);
        if (e.getClickedInventory() != e.getInventory()) return;
        if (!(e.getWhoClicked() instanceof Player p)) return;

        Kit k = menu.kitAt(e.getSlot());
        if (k == null) return;

        KitManager kits = plugin.kits();
        PlayerData d = plugin.stats().get(p);

        if (!kits.hasPermission(p, k)) {
            Text.send(p, "<red>This kit requires a rank.");
            return;
        }
        if (!kits.isUnlocked(p, d, k)) {
            if (d.coins >= k.price) {
                d.coins -= k.price;
                d.unlocked.add(k.id);
                Text.send(p, "<green>Unlocked " + k.display + "<green> for <gold>" + k.price + "<green> coins!");
                new KitMenu(plugin).open(p);
            } else {
                Text.send(p, "<red>You need <gold>" + (k.price - d.coins) + "<red> more coins.");
            }
            return;
        }
        p.closeInventory();
        kits.equip(p, k);
    }
}
