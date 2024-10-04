package org.cockshott.commandshop;

import lombok.Getter;
import org.cockshott.commandshop.commands.ShopCommand;
import org.cockshott.commandshop.managers.DatabaseManager;
import org.cockshott.commandshop.managers.EconomyManager;
import org.cockshott.commandshop.managers.ItemManager;
import org.cockshott.commandshop.managers.ShopManager;
import org.cockshott.commandshop.utils.Config;

import com.zjyl1994.minecraftplugin.multicurrency.MultiCurrencyPlugin;

import javax.sql.DataSource;

import org.bukkit.plugin.java.JavaPlugin;
import org.cockshott.commandshop.utils.TranslationLoader;
import org.cockshott.commandshop.utils.TranslationManager;

public class CommandShopPlugin extends JavaPlugin {
    @Getter
    private DatabaseManager databaseManager;
    private EconomyManager economyManager;
    private ShopManager shopManager;
    @Getter
    private TranslationManager translationManager;
    private Config config;
    private static CommandShopPlugin instance;

    @Override
    public void onEnable() {
        // 加载配置
        this.saveDefaultConfig();
        this.config = new Config(this);
        instance = this;

        DataSource hikari = MultiCurrencyPlugin.getInstance().getHikari();

        // 初始化管理器
        this.databaseManager = new DatabaseManager(this,hikari);
        this.economyManager = new EconomyManager(this, databaseManager);
        this.shopManager = new ShopManager(this, databaseManager, economyManager);
        this.translationManager = new TranslationManager(this);


        // 注册命令
        ShopCommand shopCommand = new ShopCommand(this, shopManager);
        this.getCommand("shop").setExecutor(shopCommand);
        this.getCommand("shangdian").setExecutor(shopCommand);
        this.getCommand("sd").setExecutor(shopCommand);

        getLogger().info("CommandShop插件已启用！");
    }

    @Override
    public void onDisable() {
        getLogger().info("CommandShop插件已禁用！");
        if (translationManager != null) {
            translationManager.clearCache();
        }
    }

    public Config getPluginConfig() {
        return config;
    }

    public static CommandShopPlugin getInstance() {
        return instance;
    }

}
