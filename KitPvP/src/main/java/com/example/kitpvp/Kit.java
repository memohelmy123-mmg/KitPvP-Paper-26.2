package com.example.kitpvp;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.List;

public class Kit {
    public final String id;
    public String display;
    public Material icon = Material.CHEST;
    public int price;
    public int cooldown;
    public String permission;
    public List<String> description = new ArrayList<>();
    public final List<ItemStack> items = new ArrayList<>();
    public ItemStack helmet, chestplate, leggings, boots, offhand;
    public final List<PotionEffect> effects = new ArrayList<>();

    public Kit(String id) {
        this.id = id;
        this.display = id;
    }
}
