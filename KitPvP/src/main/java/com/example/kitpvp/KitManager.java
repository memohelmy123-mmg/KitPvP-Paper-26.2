package com.example.kitpvp;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import java.io.File;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class KitManager {
    private final KitPvP plugin;
    private final Map<String, Kit> kits = new LinkedHashMap<>();

    public KitManager(KitPvP plugin) {
        this.plugin = plugin;
    }

    public Collection<Kit> all() {
        return kits.values();
    }

    public Set<String> ids() {
        return kits.keySet();
    }

    public Kit get(String id) {
        return id == null ? null : kits.get(id.toLowerCase());
    }

    // ---------- loading ----------

    public void load() {
        kits.clear();
        File f = new File(plugin.getDataFolder(), "kits.yml");
        if (!f.exists()) plugin.saveResource("kits.yml", false);
        YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
        ConfigurationSection root = y.getConfigurationSection("kits");
        if (root == null) return;
        for (String id : root.getKeys(false)) {
            try {
                kits.put(id.toLowerCase(), parse(id.toLowerCase(), root.getConfigurationSection(id)));
            } catch (Exception e) {
                plugin.getLogger().warning("Could not load kit '" + id + "': " + e.getMessage());
            }
        }
        plugin.getLogger().info("Loaded " + kits.size() + " kits.");
    }

    private Kit parse(String id, ConfigurationSection s) {
        Kit k = new Kit(id);
        k.display = s.getString("display", id);
        Material icon = Material.matchMaterial(s.getString("icon", "CHEST"));
        k.icon = icon == null ? Material.CHEST : icon;
        k.price = s.getInt("price", 0);
        k.cooldown = s.getInt("cooldown", 0);
        k.permission = s.getString("permission");
        k.description = s.getStringList("description");

        ConfigurationSection armor = s.getConfigurationSection("armor");
        if (armor != null) {
            k.helmet = item(armor.getConfigurationSection("helmet"));
            k.chestplate = item(armor.getConfigurationSection("chestplate"));
            k.leggings = item(armor.getConfigurationSection("leggings"));
            k.boots = item(armor.getConfigurationSection("boots"));
        }
        k.offhand = item(s.getConfigurationSection("offhand"));

        ConfigurationSection items = s.getConfigurationSection("items");
        if (items != null) {
            for (String key : items.getKeys(false)) {
                ItemStack it = item(items.getConfigurationSection(key));
                if (it != null) k.items.add(it);
            }
        }

        ConfigurationSection eff = s.getConfigurationSection("effects");
        if (eff != null) {
            for (String key : eff.getKeys(false)) {
                PotionEffectType t = Registry.MOB_EFFECT.get(NamespacedKey.minecraft(key.toLowerCase()));
                if (t == null) {
                    plugin.getLogger().warning("Kit " + id + ": unknown effect '" + key + "'");
                    continue;
                }
                k.effects.add(new PotionEffect(t, PotionEffect.INFINITE_DURATION, eff.getInt(key, 0), false, false, true));
            }
        }
        return k;
    }

    private ItemStack item(ConfigurationSection s) {
        if (s == null) return null;
        Material m = Material.matchMaterial(s.getString("material", ""));
        if (m == null || m.isAir()) {
            plugin.getLogger().warning("Unknown material '" + s.getString("material") + "' in " + s.getCurrentPath());
            return null;
        }
        ItemStack it = new ItemStack(m, Math.max(1, s.getInt("amount", 1)));
        ItemMeta meta = it.getItemMeta();
        if (s.contains("name")) meta.displayName(Text.noItalic(s.getString("name")));
        List<String> lore = s.getStringList("lore");
        if (!lore.isEmpty()) meta.lore(lore.stream().map(Text::noItalic).toList());

        ConfigurationSection en = s.getConfigurationSection("enchants");
        if (en != null) {
            for (String key : en.getKeys(false)) {
                Enchantment e = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(key.toLowerCase()));
                if (e != null) meta.addEnchant(e, en.getInt(key, 1), true);
                else plugin.getLogger().warning("Unknown enchant '" + key + "' in " + s.getCurrentPath());
            }
        }
        if (s.getBoolean("unbreakable", true)) meta.setUnbreakable(true);

        if (meta instanceof PotionMeta pm && s.contains("potion")) {
            String potionId = s.getString("potion", "").toLowerCase();
            PotionType potion = Registry.POTION.get(NamespacedKey.minecraft(potionId));
            if (potion != null) {
                pm.setBasePotionType(potion);
            } else {
                plugin.getLogger().warning("Unknown potion type '" + potionId + "'");
            }
        }
        it.setItemMeta(meta);
        return it;
    }

    // ---------- permissions / unlocks ----------

    public boolean hasPermission(Player p, Kit k) {
        return k.permission == null || k.permission.isBlank() || p.hasPermission(k.permission);
    }

    public boolean isUnlocked(Player p, PlayerData d, Kit k) {
        if (!hasPermission(p, k)) return false;
        return k.price <= 0 || d.unlocked.contains(k.id) || p.hasPermission("kitpvp.kit." + k.id);
    }

    // ---------- equipping ----------

    public void equip(Player p, Kit k) {
        PlayerData d = plugin.stats().get(p);

        if (!isUnlocked(p, d, k)) {
            Text.send(p, "<red>You haven't unlocked that kit.");
            return;
        }
        if (plugin.getConfig().getBoolean("kits-only-in-spawn", true)
                && !plugin.isInSpawn(p) && !p.hasPermission("kitpvp.admin")) {
            Text.send(p, "<red>You can only change kits at spawn.");
            return;
        }
        long now = System.currentTimeMillis();
        Long until = d.cooldowns.get(k.id);
        if (until != null && until > now) {
            Text.send(p, "<red>That kit is on cooldown for " + ((until - now) / 1000 + 1) + "s.");
            return;
        }

        apply(p, k);
        d.selectedKit = k.id;
        if (k.cooldown > 0) d.cooldowns.put(k.id, now + k.cooldown * 1000L);
        Text.send(p, "<green>Equipped kit " + k.display + "<green>.");
    }

    /** Gives the kit with no checks. */
    public void apply(Player p, Kit k) {
        PlayerInventory inv = p.getInventory();
        inv.clear();
        for (PotionEffect e : p.getActivePotionEffects()) p.removePotionEffect(e.getType());

        for (ItemStack it : k.items) inv.addItem(it.clone());
        inv.setHelmet(copy(k.helmet));
        inv.setChestplate(copy(k.chestplate));
        inv.setLeggings(copy(k.leggings));
        inv.setBoots(copy(k.boots));
        inv.setItemInOffHand(copy(k.offhand));
        for (PotionEffect e : k.effects) p.addPotionEffect(e);

        p.setFireTicks(0);
        p.setHealth(p.getAttribute(Attribute.MAX_HEALTH).getValue());
        p.setFoodLevel(20);
        p.setSaturation(20f);
    }

    private ItemStack copy(ItemStack it) {
        return it == null ? null : it.clone();
    }
}
