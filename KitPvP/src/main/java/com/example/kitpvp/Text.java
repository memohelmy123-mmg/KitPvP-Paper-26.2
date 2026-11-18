package com.example.kitpvp;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;

public final class Text {
    private static final MiniMessage MM = MiniMessage.miniMessage();
    public static final String PREFIX = "<dark_gray>[<red>KitPvP</red>]</dark_gray> ";

    private Text() {}

    public static Component mm(String s) {
        return MM.deserialize(s);
    }

    public static Component noItalic(String s) {
        return mm(s).decoration(TextDecoration.ITALIC, false);
    }

    public static String strip(String s) {
        return MM.stripTags(s);
    }

    public static void send(CommandSender to, String msg) {
        to.sendMessage(mm(PREFIX + msg));
    }
}
