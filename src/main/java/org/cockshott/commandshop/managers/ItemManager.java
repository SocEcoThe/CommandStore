package org.cockshott.commandshop.managers;

import org.bukkit.Material;
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
    
    public boolean hasEnoughItems(Player player, ItemStack item, int amount) {
        int count = 0;
        for (ItemStack is : player.getInventory().getContents()) {
            if (is != null && is.isSimilar(item)) {
                count += is.getAmount();
            }
        }
        return count >= amount;
    }

    public boolean hasEnoughSpace(Player player, ItemStack item, int amount) {
        int freeSpace = 0;
        for (ItemStack is : player.getInventory().getStorageContents()) {
            if (is == null || is.getType().isAir()) {
                freeSpace += item.getMaxStackSize();
            } else if (is.isSimilar(item)) {
                freeSpace += is.getMaxStackSize() - is.getAmount();
            }
        }
        return freeSpace >= amount;
    }

    public void removeItems(Player player, ItemStack item, int amount) {
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

    public void addItems(Player player, ItemStack item, int amount) {
        ItemStack toGive = item.clone();
        toGive.setAmount(amount);
        player.getInventory().addItem(toGive);
        player.updateInventory();
    }
}
