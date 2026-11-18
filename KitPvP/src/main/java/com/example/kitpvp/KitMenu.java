package com.example.kitpvp;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class KitMenu implements InventoryHolder {
    private final KitPvP plugin;
    private List<Kit> kits = new ArrayList<>();
    private Inventory inv;

    public KitMenu(KitPvP plugin) {
        this.plugin = plugin;
    }

    public void open(Player p) {
        PlayerData d = plugin.stats().get(p);
        kits = new ArrayList<>(plugin.kits().all());
        int size = Math.max(9, Math.min(54, ((kits.size() - 1) / 9 + 1) * 9));
        inv = Bukkit.createInventory(this, size, Text.mm("<dark_red><bold>Select a Kit"));
        for (int i = 0; i < kits.size() && i < 54; i++) {
            inv.setItem(i, icon(p, d, kits.get(i)));
        }
        p.openInventory(inv);
    }

    public Kit kitAt(int slot) {
        return slot >= 0 && slot < kits.size() ? kits.get(slot) : null;
    }

    private ItemStack icon(Player p, PlayerData d, Kit k) {
        ItemStack it = new ItemStack(k.icon);
        ItemMeta m = it.getItemMeta();
        m.displayName(Text.noItalic(k.display));

        List<Component> lore = new ArrayList<>();
        for (String line : k.description) lore.add(Text.noItalic("<gray>" + line));
        lore.add(Component.empty());

        boolean selected = k.id.equals(d.selectedKit);
        if (!plugin.kits().hasPermission(p, k)) {
            lore.add(Text.noItalic("<red>\u2718 Requires a rank"));
        } else if (!plugin.kits().isUnlocked(p, d, k)) {
            lore.add(Text.noItalic("<yellow>Price: <gold>" + k.price + " coins"));
            lore.add(Text.noItalic("<green>Click to buy"));
        } else if (selected) {
            lore.add(Text.noItalic("<green>\u2714 Selected"));
        } else {
            lore.add(Text.noItalic("<green>Click to equip"));
        }
        m.lore(lore);
        m.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        if (selected) m.setEnchantmentGlintOverride(true);
        it.setItemMeta(m);
        return it;
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }
}
