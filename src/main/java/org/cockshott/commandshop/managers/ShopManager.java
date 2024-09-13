package org.cockshott.commandshop.managers;

import org.cockshott.commandshop.CommandShopPlugin;
import org.cockshott.commandshop.models.ShopItem;
import org.cockshott.commandshop.models.SearchResult;
import org.cockshott.commandshop.utils.MessageUtil;
import org.cockshott.commandshop.utils.TableRenderer;
import org.cockshott.commandshop.utils.TranslationManager;
import org.cockshott.commandshop.utils.ItemSerializer;

import com.zjyl1994.minecraftplugin.multicurrency.utils.OperateResult;


import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.io.File;
import java.math.BigDecimal;

public class ShopManager {
    private final CommandShopPlugin plugin;
    private final DatabaseManager databaseManager;
    private final EconomyManager economyManager;
    private final ItemManager itemManager;
    private Map<UUID, List<ShopItem>> lastSearchResults = new HashMap<>();
    private Map<String, String> map;

    public ShopManager(CommandShopPlugin plugin, DatabaseManager databaseManager, EconomyManager economyManager,
            ItemManager itemManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.economyManager = economyManager;
        this.itemManager = itemManager;
        File modFolder = new File("mods");
        TranslationManager manager = new TranslationManager(plugin, modFolder);
        this.map = manager.getMergedTranslations(manager.loadOriginalTranslations());
    }

    public List<ShopItem> searchItems(Player player, String keyword, int page) {
        int itemsPerPage = plugin.getPluginConfig().getItemsPerPage();
        SearchResult searchResult = databaseManager.searchItems(keyword, page, itemsPerPage);

        List<ShopItem> items = searchResult.getItems();
        int totalItems = searchResult.getTotalItems();
        int totalPages = (int) Math.ceil((double) totalItems / itemsPerPage);
        lastSearchResults.put(player.getUniqueId(), items);

        if (items.isEmpty()) {
            player.sendMessage(MessageUtil.color("&c没有找到匹配的物品。"));
            return items;
        }

        player.sendMessage(MessageUtil.color("&6==== 搜索结果 ===="));
        player.sendMessage(MessageUtil.color(String.format("&7第 %d 页，共 %d 页", page, totalPages)));
        player.sendMessage(MessageUtil.color(String.format("&7共找到 %d 个物品", totalItems)));

        TableRenderer.renderSearchResults(player, items);

        if (page < totalPages) {
            player.sendMessage(MessageUtil.color(String.format("&a使用 /sd sou %s %d 查看下一页", keyword, page + 1)));
        }

        return items;
    }

    private List<ShopItem> getLastSearchResult(Player player) {
        return lastSearchResults.getOrDefault(player.getUniqueId(), new ArrayList<>());
    }

    public void buyItem(Player player, ShopItem shopItem, int amount) {
        if (amount > shopItem.getStock()) {
            player.sendMessage(MessageUtil.color("&c该物品库存不足。"));
            return;
        }

        BigDecimal totalCost = new BigDecimal(shopItem.getPrice()).multiply(BigDecimal.valueOf(amount));
        String currency = shopItem.getCurrency();

        if (!economyManager.hasEnoughMoney(player, totalCost, currency)) {
            player.sendMessage(MessageUtil.color("&c你没有足够的" + currency + "购买此物品。"));
            return;
        }

        ItemStack itemToGive = ItemManager.getItemStack(shopItem);
        if (itemToGive == null) {
            player.sendMessage(MessageUtil.color("&c无法创建物品，请联系管理员。"));
            return;
        }

        if (!itemManager.hasEnoughSpace(player, itemToGive, amount)) {
            player.sendMessage(MessageUtil.color("&c你的背包空间不足。"));
            return;
        }

        OperateResult result = economyManager.withdrawMoney(player.getName(), totalCost, currency,
                String.format("购买 %d 个 %s", amount, shopItem.getName())); 
        if (!result.getSuccess()) {
            player.sendMessage(MessageUtil.color("&c交易失败: " + result.getReason()));
            return;
        }

        itemManager.addItems(player, itemToGive, amount);
        databaseManager.updateItemStock(shopItem.getId(), -amount);
        shopItem.setStock(shopItem.getStock() - amount);
        if (shopItem.getStock() == 0) {
            databaseManager.deleteShopItem(shopItem.getId());
        }

        player.sendMessage(MessageUtil.color(String.format("&a你成功购买了 %d 个 %s，共花费 %s %s。",
                amount, shopItem.getName(), totalCost, currency)));
        economyManager.depositMoney(shopItem.getSellName(), totalCost, currency,String.format("售出 %d 个 %s", amount, shopItem.getName()));
    }

    public void sellItem(Player player, int amount, String currency, Integer price) {
        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        if (itemInHand == null || itemInHand.getType().isAir()) {
            player.sendMessage(MessageUtil.color("&c你必须手持要出售的物品。"));
            return;
        }
    
        if (!itemManager.hasEnoughItems(player, itemInHand, amount)) {
            player.sendMessage(MessageUtil.color("&c你没有足够的物品来出售。"));
            return;
        }
    
        String itemName = itemInHand.getType().name();
        String itemHash = ItemSerializer.gethashString(itemInHand);
    
        ShopItem newShopItem = new ShopItem(
            0, // ID 由数据库设置
            itemName,
            price,
            currency,
            amount,
            itemHash,
            itemInHand.getType().name(),
            player.getName()
        );
    
        boolean success = databaseManager.addShopItem(newShopItem);
        if (!success) {
            player.sendMessage(MessageUtil.color("&c出售物品时发生错误，请稍后再试。"));
            return;
        }
    
        itemManager.removeItems(player, itemInHand, amount);
    
        player.sendMessage(MessageUtil.color(String.format("&a你成功上架了 %d 个 %s，单价 %.2f %s。",
            amount, itemName, price, currency)));
    }

    public boolean withdrawItem(Player player, int itemId) {
        ShopItem item = databaseManager.getShopItem(itemId);
        if (item == null) {
            player.sendMessage(MessageUtil.color("&c该商品不存在。"));
            return false;
        }
    
        if (!item.getSellName().equals(player.getName())) {
            player.sendMessage(MessageUtil.color("&c你不是这个商品的卖家，无法撤回。"));
            return false;
        }
    
        ItemStack itemToGive = ItemManager.getItemStack(item);
        if (itemToGive == null) {
            player.sendMessage(MessageUtil.color("&c无法创建物品，请联系管理员。"));
            return false;
        }
    
        if (!itemManager.hasEnoughSpace(player, itemToGive, item.getStock())) {
            player.sendMessage(MessageUtil.color("&c你的背包空间不足，无法撤回商品。"));
            return false;
        }
    
        boolean deleted = databaseManager.deleteShopItem(itemId);
        if (!deleted) {
            player.sendMessage(MessageUtil.color("&c撤回商品失败，请稍后再试。"));
            return false;
        }
    
        itemManager.addItems(player, itemToGive, item.getStock());
        player.sendMessage(MessageUtil.color(String.format("&a你成功撤回了 %d 个 %s。", 
            item.getStock(), item.getName())));
    
        return true;
    }
}
