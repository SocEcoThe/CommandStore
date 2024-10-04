package org.cockshott.commandshop.utils;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.cockshott.commandshop.CommandShopPlugin;

import java.util.Base64;
import java.util.Map;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;

public class ItemSerializer {
    // 获取hash
    public static String gethashString(ItemStack item) {
        ItemMeta itemMeta = item.getItemMeta();
        return serializeItemMeta(itemMeta);
    }

    // 序列化ItemMeta
    public static String serializeItemMeta(ItemMeta itemMeta) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            dataOutput.writeObject(itemMeta);
            dataOutput.close();
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    // 反序列化ItemMeta
    public static ItemMeta deserializeItemMeta(String itemMetaString) {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(itemMetaString));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            ItemMeta itemMeta = (ItemMeta) dataInput.readObject();
            dataInput.close();
            return itemMeta;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
