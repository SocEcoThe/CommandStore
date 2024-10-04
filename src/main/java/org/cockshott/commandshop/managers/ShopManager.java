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

import java.util.*;
import java.math.BigDecimal;

public class ShopManager {
    private final CommandShopPlugin plugin;
    private final DatabaseManager databaseManager;
    private final EconomyManager economyManager;
    private Map<UUID, List<ShopItem>> lastSearchResults = new HashMap<>();

    public ShopManager(CommandShopPlugin plugin, DatabaseManager databaseManager, EconomyManager economyManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.economyManager = economyManager;
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

    //购买操作
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

        OperateResult result = economyManager.withdrawMoney(player.getName(), totalCost, currency,
                String.format("购买 %d 个 %s", amount, shopItem.getName())); 
        if (!result.getSuccess()) {
            player.sendMessage(MessageUtil.color("&c交易失败: " + result.getReason()));
            return;
        }

        ItemManager.addItems(player, itemToGive, amount);
        int stock = shopItem.getStock() - amount;
        databaseManager.updateItemStock(shopItem.getId(), stock);
        shopItem.setStock(stock);
        if (shopItem.getStock() == 0) {
            databaseManager.deleteShopItem(shopItem.getId());
        }

        player.sendMessage(MessageUtil.color(String.format("&a你成功购买了 %d 个 %s，共花费 %s %s。",
                amount, shopItem.getName(), totalCost, currency)));
        economyManager.depositMoney(shopItem.getSellName(), totalCost, currency,String.format("售出 %d 个 %s", amount, shopItem.getName()));
    }

    //出售操作
    public void sellItem(Player player, int amount, String currency, Integer price) {
        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        String itemString = itemInHand.toString();

        if (itemString.contains("AIR") || itemString.equals("ItemStack{AIR x 0}")) {
            player.sendMessage(MessageUtil.color("&c你必须手持要出售的物品。"));
            return;
        }

    
        if (!ItemManager.hasEnoughItems(player, itemInHand, amount)) {
            player.sendMessage(MessageUtil.color("&c你没有足够的物品来出售。"));
            return;
        }
    
        String itemName = getTranslate(itemInHand);
        String itemHash = ItemSerializer.gethashString(itemInHand);
    
        ShopItem newShopItem = new ShopItem(
            0, // ID 由数据库设置
            itemName,
            price,
            currency,
            amount,
            itemHash,
            extractItemName(itemString),
            player.getName()
        );
    
        boolean success = databaseManager.addShopItem(newShopItem);
        if (!success) {
            player.sendMessage(MessageUtil.color("&c出售物品时发生错误，请稍后再试。"));
            return;
        }
    
        ItemManager.removeItems(player, itemInHand, amount);
    
        player.sendMessage(MessageUtil.color(String.format("&a你成功上架了 %d 个 %s，单价 %d %s。",
            amount, itemName, price, currency)));
    }

    //玩家出售列表
    public List<ShopItem> listPlayerItems(Player player) {
        List<ShopItem> items = databaseManager.getPlayerItems(player.getName());
        if (items.isEmpty()) {
            player.sendMessage(MessageUtil.color("&c你当前没有出售中的商品。"));
        } else {
            player.sendMessage(MessageUtil.color("&6==== 你的商品列表 ===="));
            TableRenderer.renderPlayerItems(player, items);
        }
        return items;
    }

    //撤回出售
    public boolean withdrawItemByIndex(Player player, int index) {
        List<ShopItem> items = databaseManager.getPlayerItems(player.getName());
        if (index < 0 || index >= items.size()) {
            player.sendMessage(MessageUtil.color("&c无效的商品序号。"));
            return false;
        }
        ShopItem item = items.get(index);
        return withdrawItem(player, item.getId());
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

        boolean deleted = databaseManager.deleteShopItem(itemId);
        if (!deleted) {
            player.sendMessage(MessageUtil.color("&c撤回商品失败，请稍后再试。"));
            return false;
        }

        ItemManager.addItems(player, itemToGive, item.getStock());
        player.sendMessage(MessageUtil.color(String.format("&a你成功撤回了 %d 个 %s。",
            item.getStock(), item.getName())));

        return true;
    }

    public String getTranslate(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() == null) {
            return "Unknown Item";
        }

        if (itemStack.getItemMeta() != null && !isEmpty(itemStack.getItemMeta().getDisplayName())) {
            return itemStack.getItemMeta().getDisplayName();
        }

        try {
            String materialName = itemStack.toString();

            // 处理 LEGACY 物品
            if (materialName.startsWith("LEGACY_")) {
                materialName = materialName.substring(7); // 移除 "LEGACY_" 前缀
            }

            TranslationManager manager = CommandShopPlugin.getInstance().getTranslationManager();

            // 尝试获取翻译
            String translation = manager.getTranslation(extractItemName(materialName));

            return translation != null ? translation : materialName;
        } catch (Exception e) {
            plugin.getLogger().warning("获取项目翻译时出错： " + itemStack.getType());
            return itemStack.getType().name();
        }
    }

    private String extractItemName(String itemString) {
        // 移除所有空白字符
        itemString = itemString.replaceAll("\\s+", "");

        // 尝试匹配 ItemStack{NAME x quantity, ...} 格式
        if (itemString.startsWith("ItemStack{")) {
            int endIndex = itemString.indexOf('x');
            if (endIndex == -1) {
                endIndex = itemString.indexOf(',');
            }
            if (endIndex == -1) {
                endIndex = itemString.indexOf('}');
            }
            if (endIndex != -1) {
                return itemString.substring(10, endIndex); // 10 是 "ItemStack{" 的长度
            }
        }

        // 尝试匹配 {NAME} 格式
        if (itemString.startsWith("{") && itemString.endsWith("}")) {
            return itemString.substring(1, itemString.length() - 1);
        }

        // 如果上述都不匹配，尝试提取第一个大写字母开头的单词序列
        String[] parts = itemString.split("[^A-Z_]+");
        for (String part : parts) {
            if (!part.isEmpty()) {
                return part;
            }
        }

        // 如果所有方法都失败，返回原始字符串
        return itemString;
    }

    private static boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }
}
