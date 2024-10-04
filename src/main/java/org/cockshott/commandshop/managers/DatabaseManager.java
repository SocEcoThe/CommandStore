package org.cockshott.commandshop.managers;

import org.cockshott.commandshop.CommandShopPlugin;
import org.cockshott.commandshop.models.ShopItem;
import org.cockshott.commandshop.utils.DatabaseUtils;
import org.cockshott.commandshop.models.SearchResult;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import java.sql.Statement;
import javax.sql.DataSource;

public class DatabaseManager {
    private final CommandShopPlugin plugin;
    private final DataSource dataSource;

    public DatabaseManager(CommandShopPlugin plugin, DataSource dataSource) {
        this.plugin = plugin;
        this.dataSource = dataSource;
        initializeTables();
    }

    private void initializeTables() {
        String[] create = {
            "CREATE TABLE IF NOT EXISTS `shop_items` (" +
                "`id` INT AUTO_INCREMENT NOT NULL," +
                "`name` VARCHAR(100) NOT NULL," +
                "`price` INT NOT NULL," +
                "`currency` VARCHAR(20) NOT NULL," +
                "`stock` INT NOT NULL," +
                "`hash` VARCHAR(255) NOT NULL," +
                "`type` VARCHAR(50) NOT NULL," +
                "`S_name` VARCHAR(16) NOT NULL," +
                "PRIMARY KEY (`id`)," +
                "INDEX `idx_shop_items_name` (`name`)," +
                "INDEX `idx_shop_items_price` (`price`)," +
                "INDEX `idx_shop_items_type` (`type`)," +
                "INDEX `idx_shop_items_seller` (`S_name`)" +
            ") ENGINE=INNODB DEFAULT CHARSET=utf8 COMMENT='命令商店表';"
        };
    
        try {
            DatabaseUtils.executeTransaction(dataSource, connection -> {
                try (Statement stmt = connection.createStatement()) {
                    for (String sql : create) {
                        stmt.execute(sql);
                    }
                    plugin.getLogger().info("成功创建或更新 shop_items 表");
                    return null;
                }
            });
        } catch (SQLException e) {
            plugin.getLogger().severe("初始化数据库表时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public ShopItem getShopItem(int itemId) {
        String sql = "SELECT * FROM shop_items WHERE id = ?";

        try {
            return DatabaseUtils.executeQuery(dataSource, connection -> {
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setInt(1, itemId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            return new ShopItem(
                                rs.getInt("id"),
                                rs.getString("name"),
                                rs.getInt("price"),
                                rs.getString("currency"),
                                rs.getInt("stock"),
                                rs.getString("hash"),
                                rs.getString("type"),
                                rs.getString("S_name")
                            );
                        }
                        return null;
                    }
                }
            });
        } catch (SQLException e) {
            plugin.getLogger().severe("从数据库获取商品时出错: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public boolean deleteShopItem(int itemId) {
        String sql = "DELETE FROM shop_items WHERE id = ?";

        try {
            return DatabaseUtils.executeTransaction(dataSource, connection -> {
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setInt(1, itemId);
                    int affectedRows = stmt.executeUpdate();
                    return affectedRows > 0;
                }
            });
        } catch (SQLException e) {
            plugin.getLogger().severe("删除商品时出错 (ID: " + itemId + "): " + e.getMessage());
            return false;
        }
    }

    public SearchResult searchItems(String keyword, int page, int itemsPerPage) {
        String sql =
            "WITH SearchResults AS (" +
            "    SELECT *, COUNT(*) OVER() as total_count " +
            "    FROM shop_items " +
            "    WHERE name LIKE ? " +
            ") " +
            "SELECT * FROM SearchResults " +
            "ORDER BY price ASC " +
            "LIMIT ? OFFSET ?";

        try {
            return DatabaseUtils.executeQuery(dataSource, connection -> {
                List<ShopItem> items = new ArrayList<>();
                int totalItems = 0;
                int offset = (page - 1) * itemsPerPage;

                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setString(1, "%" + keyword + "%");
                    ps.setInt(2, itemsPerPage);
                    ps.setInt(3, offset);

                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            if (totalItems == 0) {
                                totalItems = rs.getInt("total_count");
                            }

                            items.add(new ShopItem(
                                rs.getInt("id"),
                                rs.getString("name"),
                                rs.getInt("price"),
                                rs.getString("currency"),
                                rs.getInt("stock"),
                                rs.getString("hash"),
                                rs.getString("type"),
                                rs.getString("S_name")
                            ));
                        }
                    }
                }
                return new SearchResult(items, totalItems);
            });
        } catch (SQLException e) {
            plugin.getLogger().severe("搜索物品时出错: " + e.getMessage());
            return new SearchResult(new ArrayList<>(), 0);
        }
    }

    public boolean updateItemStock(int itemId, int newAmount) {
        String sql = "UPDATE shop_items SET stock = ? WHERE id = ?";

        try {
            return DatabaseUtils.executeTransaction(dataSource, connection -> {
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setInt(1, newAmount);
                    stmt.setInt(2, itemId);
                    int affectedRows = stmt.executeUpdate();
                    return affectedRows > 0;
                }
            });
        } catch (SQLException e) {
            plugin.getLogger().severe("更新物品库存时出错: " + e.getMessage());
            return false;
        }
    }

    public boolean addShopItem(ShopItem item) {
        String sql = "INSERT INTO shop_items (name, price, currency, stock, hash, type, S_name) VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try {
            return DatabaseUtils.executeTransaction(dataSource, connection -> {
                try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    stmt.setString(1, item.getName());
                    stmt.setInt(2, item.getPrice());
                    stmt.setString(3, item.getCurrency());
                    stmt.setInt(4, item.getStock());
                    stmt.setString(5, item.getHash());
                    stmt.setString(6, item.getType());
                    stmt.setString(7, item.getSellName());
                    
                    int affectedRows = stmt.executeUpdate();
                    if (affectedRows == 0) {
                        throw new SQLException("Creating shop item failed, no rows affected.");
                    }
    
                    try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            item.setId(generatedKeys.getInt(1));
                        } else {
                            throw new SQLException("Creating shop item failed, no ID obtained.");
                        }
                    }
    
                    return true;
                }
            });
        } catch (SQLException e) {
            plugin.getLogger().severe("添加商品时出错: " + e.getMessage());
            return false;
        }
    }

    public List<ShopItem> getPlayerItems(String playerName) {
        String sql = "SELECT * FROM shop_items WHERE S_name = ? ORDER BY id DESC";
        List<ShopItem> items = new ArrayList<>();

        try {
            return DatabaseUtils.executeQuery(dataSource, connection -> {
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setString(1, playerName);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            items.add(new ShopItem(
                                    rs.getInt("id"),
                                    rs.getString("name"),
                                    rs.getInt("price"),
                                    rs.getString("currency"),
                                    rs.getInt("stock"),
                                    rs.getString("hash"),
                                    rs.getString("type"),
                                    rs.getString("S_name")
                            ));
                        }
                    }
                }
                return items;
            });
        } catch (SQLException e) {
            plugin.getLogger().severe("获取玩家商品时出错: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public boolean itemExists(int itemId) throws SQLException {
        String sql = "SELECT 1 FROM shop_items WHERE id = ?";
        return DatabaseUtils.executeQuery(dataSource, connection -> {
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setInt(1, itemId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        });
    }
}
