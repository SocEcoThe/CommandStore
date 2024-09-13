package org.cockshott.commandshop.utils;

import org.cockshott.commandshop.CommandShopPlugin;
import org.bukkit.configuration.file.FileConfiguration;

public class Config {
    private final CommandShopPlugin plugin;
    private FileConfiguration config;

    public Config(CommandShopPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
        loadDefaults();
    }

    private void loadDefaults() {
        config.addDefault("messages.prefix", "&7[&6商店&7] ");
        config.addDefault("messages.no_permission", "&c你没有权限执行此命令。");
        config.addDefault("settings.items_per_page", 10);
        config.options().copyDefaults(true);
        plugin.saveConfig();
    }

    public String getMessage(String path) {
        return config.getString("messages." + path, "");
    }

    public int getItemsPerPage() {
        return config.getInt("settings.items_per_page", 10);
    }
}
