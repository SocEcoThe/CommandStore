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

    // 获取item翻译名称
    public static String getTranslate(ItemStack itemStack) {
        // 添加 null 检查
        if (itemStack == null || itemStack.getType() == null) {
            return "Unknown Item";
        }

        // 如果物品的元数据（如自定义名称）不为空，那么返回该自定义名称
        if (itemStack.getItemMeta() != null && !isEmpty(itemStack.getItemMeta().getDisplayName())) {
            return itemStack.getItemMeta().getDisplayName();
        }

        // 如果没有自定义名称，则从map中获取物品的默认翻译名称
        File modFolder = new File("mods");
        CommandShopPlugin plugin = CommandShopPlugin.getInstance();
        TranslationManager manager = new TranslationManager(plugin, modFolder);
        Map<String, String> map = manager.getMergedTranslations(manager.loadOriginalTranslations());

        try {
            Material material = itemStack.getType();
            String namespace = material.getKey().getNamespace();
            String key = material.getKey().getKey();

            return map.getOrDefault(
                    "block." + namespace + "." + key,
                    map.getOrDefault("item." + namespace + "." + key, key));
        } catch (IllegalArgumentException e) {
            // 处理 Legacy Material
            plugin.getLogger().warning("Cannot get key for item: " + itemStack.getType());
            return itemStack.getType().name();
        }
    }

    public static boolean isEmpty(String s) {
        return s == null || s.length() == 0;
    }
}
