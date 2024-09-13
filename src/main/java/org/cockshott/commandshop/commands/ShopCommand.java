package org.cockshott.commandshop.commands;

import org.cockshott.commandshop.CommandShopPlugin;
import org.cockshott.commandshop.managers.ShopManager;
import org.cockshott.commandshop.models.ShopItem;
import org.cockshott.commandshop.utils.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ShopCommand implements CommandExecutor, TabCompleter {
    private final CommandShopPlugin plugin;
    private final ShopManager shopManager;
    private HashMap<Player,List<ShopItem>> searcItems;

    public ShopCommand(CommandShopPlugin plugin, ShopManager shopManager) {
        this.plugin = plugin;
        this.shopManager = shopManager;
        this.searcItems = new HashMap<>();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageUtil.color("&c该命令只能由玩家使用。"));
            return true;
        }

        Player player = (Player) sender;

        if (args.length < 1) {
            showMenu(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();


        switch (subCommand) {
            case "sou":
            case "ss":
            case "search":
                return handleSearch(player, args);
            case "mai":
            case "gm":
            case "buy":
                return handleBuy(player, args);
            case "chu":
            case "cs":
            case "sell":
                return handleSell(player, args);
            case "che":
            case "chehui":
            case "withdraw":
                return handleWithdraw(player, args);
            case "bangzhu":
            case "bz":
            case "help":
                showHelp(player);
                return true;
            default:
                player.sendMessage(MessageUtil.color("&c未知的命令。请使用 /sd bangzhu 查看帮助。"));
                return true;
        }
    }

    private void showMenu(Player player) {
        player.sendMessage(MessageUtil.color("&6==== 商店菜单 ===="));
        player.sendMessage(MessageUtil.color("&e1. 搜索物品"));
        player.sendMessage(MessageUtil.color("&e2. 购买物品"));
        player.sendMessage(MessageUtil.color("&e3. 出售物品"));
        player.sendMessage(MessageUtil.color("&e4. 查看帮助"));
        player.sendMessage(MessageUtil.color("&7请输入对应的数字或使用 /sd <命令> 来执行操作。"));
    }

    private boolean handleSearch(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(MessageUtil.color("&c用法: /sd sou <关键词> [页码]"));
            return true;
        }
        String keyword = args[1];
        int page = args.length > 2 ? Integer.parseInt(args[2]) : 1;
        searcItems.put(player, shopManager.searchItems(player, keyword, page));
        return true;
    }

    private boolean handleBuy(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(MessageUtil.color("&c用法: /sd mai <物品序号> <数量>"));
            return true;
        }
        int itemIndex;
        try {
            itemIndex = Integer.parseInt(args[1]) - 1;
        } catch (NumberFormatException e) {
            player.sendMessage(MessageUtil.color("&c请输入有效的物品序号。"));
            return true;
        }
        int amount = Integer.parseInt(args[2]);
        shopManager.buyItem(player, searcItems.get(player).get(itemIndex), amount);
        return true;
    }

    private boolean handleSell(Player player, String[] args) {
        if (args.length < 4) {
            player.sendMessage(MessageUtil.color("&c用法: /sd chu <数量> <货币> <价格>"));
            return true;
        }
        int amount;
        int price;
        try {
            amount = Integer.parseInt(args[1]);
            price = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            player.sendMessage(MessageUtil.color("&c数量和价格必须是整数。"));
            return true;
        }
        String currency = args[2];
        
        if (price <= 0) {
            player.sendMessage(MessageUtil.color("&c价格必须大于0。"));
            return true;
        }

        shopManager.sellItem(player, amount, currency, price);
        return true;
    }

    private boolean handleWithdraw(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(MessageUtil.color("&c用法: /sd che <物品ID>"));
            return true;
        }
        int itemId;
        try {
            itemId = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(MessageUtil.color("&c物品ID必须是一个整数。"));
            return true;
        }

        shopManager.withdrawItem(player, itemId);
        return true;
    }

    private void showHelp(Player player) {
        player.sendMessage(MessageUtil.color("&6==== 商店帮助 ===="));
        player.sendMessage(MessageUtil.color("&e/sd sou <关键词> [页码] &7- 搜索物品"));
        player.sendMessage(MessageUtil.color("&e/sd mai <物品序号> <数量> &7- 搜索后才可使用，根据序号购买物品"));
        player.sendMessage(MessageUtil.color("&e/sd chu <数量> <货币> <价格> &7- 出售手中的物品"));
        player.sendMessage(MessageUtil.color("&e/sd che <物品ID> &7- 撤回已上架的物品"));
        player.sendMessage(MessageUtil.color("&e/sd bangzhu &7- 显示此帮助信息"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            String[] subCommands = {"sou", "ss", "mai", "gm", "chu", "cs", "che", "chehui", "bangzhu", "bz"};
            for (String subCommand : subCommands) {
                if (subCommand.startsWith(input)) {
                    completions.add(subCommand);
                }
            }
        }
        return completions;
    }
}