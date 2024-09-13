package org.cockshott.commandshop.utils;

import java.util.List;

import org.bukkit.entity.Player;
import org.cockshott.commandshop.models.ShopItem;

public class TableRenderer {
    public static void renderSearchResults(Player player, List<ShopItem> items) {
        player.sendMessage(MessageUtil.color("&8+----+----------------------+----------+---------+"));
        player.sendMessage(MessageUtil.color("&8| &f序号 &8| &f物品名称             &8| &f价格     &8| &f库存    &8|"));
        player.sendMessage(MessageUtil.color("&8+----+----------------------+----------+---------+"));
        
        for (int i = 0; i < items.size(); i++) {
            ShopItem item = items.get(i);
            player.sendMessage(MessageUtil.color(String.format("&8| &f%-4d&8| &f%-20s &8| &6%-8.2f &8| &a%-7d &8|", 
                i + 1, item.getName(), item.getPrice(), item.getStock())));
        }
        
        player.sendMessage(MessageUtil.color("&8+----+----------------------+----------+---------+"));
        player.sendMessage(MessageUtil.color("&a使用 /sd mai <序号> <数量> 购买物品"));
    }
}
