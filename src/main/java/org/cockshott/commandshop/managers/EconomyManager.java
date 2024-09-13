package org.cockshott.commandshop.managers;

import org.cockshott.commandshop.CommandShopPlugin;
import org.bukkit.entity.Player;

import com.zjyl1994.minecraftplugin.multicurrency.services.BankService;
import com.zjyl1994.minecraftplugin.multicurrency.utils.OperateResult;
import com.zjyl1994.minecraftplugin.multicurrency.utils.TxTypeEnum;


import java.math.BigDecimal;

public class EconomyManager {
    private final CommandShopPlugin plugin;
    private final DatabaseManager databaseManager;
    private static final String SHOP_ACCOUNT = "$SHOP";

    public EconomyManager(CommandShopPlugin plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    //钱是否足够
    public boolean hasEnoughMoney(Player player, BigDecimal amount, String currency) {
        OperateResult operateResult = BankService.queryCurrencyBalance(player.getName(), currency);
        if (operateResult.getSuccess()) {
            BigDecimal balance = (BigDecimal) operateResult.getData();
            return balance.compareTo(amount) >= 0;
        }
        return false;
    }

    //钱出
    public OperateResult withdrawMoney(String playerName, BigDecimal amount, String currency, String reason) {
        return BankService.transferTo(playerName, SHOP_ACCOUNT, currency, amount, TxTypeEnum.SHOP_TRADE_OUT, reason);
    }

    //钱进
    public OperateResult depositMoney(String playerName, BigDecimal amount, String currency, String reason) {
        return BankService.transferTo(SHOP_ACCOUNT, playerName, currency, amount, TxTypeEnum.SHOP_TRADE_IN, reason);
    }

    //余额查询
    public BigDecimal getBalance(Player player, String currency) {
        OperateResult operateResult = BankService.queryCurrencyBalance(player.getName(), currency);
        if (operateResult.getSuccess()) {
            return (BigDecimal) operateResult.getData();
        }
        return BigDecimal.ZERO;
    }
}
