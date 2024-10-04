package org.cockshott.commandshop.managers;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.cockshott.commandshop.models.ShopItem;
import org.cockshott.commandshop.utils.ItemSerializer;


public class ItemManager {
     public static ItemStack getItemStack(ShopItem shopItem) {
        Material material = Material.valueOf(shopItem.getType());
        ItemStack itemStack = new ItemStack(material);
        String hashcode = shopItem.getHash();
        if (!hashcode.isEmpty()) {
            ItemMeta meta = ItemSerializer.deserializeItemMeta(hashcode);
            itemStack.setItemMeta(meta);
        }
        return itemStack;
    }
    
    public static boolean hasEnoughItems(Player player, ItemStack item, int amount) {
        int count = 0;
        for (ItemStack is : player.getInventory().getContents()) {
            if (is != null && is.isSimilar(item)) {
                count += is.getAmount();
            }
        }
        return count >= amount;
    }

    public static boolean hasEnoughSpace(Player player, ItemStack item, int amount) {
        int freeSpace = 0;
        for (ItemStack is : player.getInventory().getStorageContents()) {
//            String itemString = is.toString();
            if (is == null) {
                // 发现一个空槽位，直接返回 true
                return true;
            } else if (is.isSimilar(item)) {
                freeSpace += is.getMaxStackSize() - is.getAmount();
                if (freeSpace >= amount) {
                    // 找到足够的空间，返回 true
                    return true;
                }
            }
        }

        // 如果没有足够的空间，将物品丢出去
        World world = player.getWorld();
        Location dropLocation = player.getLocation();

        int remainingAmount = amount;
        while (remainingAmount > 0) {
            ItemStack dropItem = item.clone();
            world.dropItemNaturally(dropLocation, dropItem);
            remainingAmount += -1;
        }

        player.sendMessage(ChatColor.YELLOW + "物品已送达，请即时拾取。");
        return false;
    }

    public static void removeItems(Player player, ItemStack item, int amount) {
        ItemStack[] contents = player.getInventory().getContents();
        int remaining = amount;
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack is = contents[i];
            if (is != null && is.isSimilar(item)) {
                if (is.getAmount() > remaining) {
                    is.setAmount(is.getAmount() - remaining);
                    remaining = 0;
                } else {
                    remaining -= is.getAmount();
                    player.getInventory().setItem(i, null);
                }
            }
        }
        player.updateInventory();
    }

    public static void addItems(Player player, ItemStack item, int amount) {
//        ItemStack toGive = item.clone();
//        toGive.setAmount(amount);
//        player.getInventory().addItem(toGive);
//        player.updateInventory();
        World world = player.getWorld();
        Location dropLocation = player.getLocation();

        ItemStack dropItem = item.clone();
        dropItem.setAmount(amount);
        world.dropItemNaturally(dropLocation, dropItem);

        player.sendMessage(ChatColor.YELLOW + "物品已送达，请即时拾取。");
    }
}
