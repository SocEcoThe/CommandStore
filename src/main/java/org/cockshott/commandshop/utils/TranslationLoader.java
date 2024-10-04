package org.cockshott.commandshop.utils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.bukkit.plugin.java.JavaPlugin;
import org.cockshott.commandshop.CommandShopPlugin;

import javax.sql.DataSource;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.Map;
import java.util.Objects;

public class TranslationLoader {
    private final JavaPlugin plugin;
    private final DataSource dataSource;
    private Map<String, String> translationsCache;

    public TranslationLoader(JavaPlugin plugin, DataSource dataSource) {
        this.plugin = plugin;
        this.dataSource = dataSource;
    }

    public void loadTranslations() {
        if (!isTableEmpty()) {
            plugin.getLogger().info("Translations already loaded, skipping.");
            return;
        }

        if (translationsCache.isEmpty()) {
            CommandShopPlugin.getInstance().getLogger().warning("translationsCache is empty. No data to insert.");
        }

        try (Connection conn = dataSource.getConnection()) {
            createTable(conn);
            loadJsonTranslations();
            insertTranslations(conn);
            clearCache(); // 清除缓存
            plugin.getLogger().info("Translations loaded successfully and cache cleared.");
        } catch (Exception e) {
            plugin.getLogger().severe("Error loading translations: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean isTableEmpty() {
        String sql = "SELECT COUNT(*) FROM translations";
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1) == 0;
            }
        } catch (Exception e) {
            // 如果表不存在，会抛出异常，则认为表为空
            return true;
        }
        return true;
    }

    private void createTable(Connection conn) throws Exception {
        String sql = "CREATE TABLE IF NOT EXISTS translations (" +
                "item_key VARCHAR(255) PRIMARY KEY, " +
                "value TEXT NOT NULL)";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    private void loadJsonTranslations() throws Exception {
        try (InputStreamReader reader = new InputStreamReader(
                plugin.getResource("zh_cn.json"), "UTF-8")) {
            Gson gson = new Gson();
            Type type = new TypeToken<Map<String, String>>(){}.getType();
            Map<String, String> loadedTranslations = gson.fromJson(reader, type);

            if (loadedTranslations == null || loadedTranslations.isEmpty()) {
                throw new Exception("No translations loaded from JSON file");
            }

            translationsCache = loadedTranslations;
            plugin.getLogger().info("Loaded " + translationsCache.size() + " translations from JSON");
        } catch (Exception e) {
            plugin.getLogger().severe("Error loading translations from JSON: " + e.getMessage());
            throw e;
        }
    }

    private boolean insertTranslations(Connection conn) throws SQLException {
        String sql = "INSERT INTO translations (item_key, value) VALUES (?, ?) " +
                "ON DUPLICATE KEY UPDATE value = VALUES(value)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            int count = 0;
            for (Map.Entry<String, String> entry : translationsCache.entrySet()) {
                pstmt.setString(1, entry.getKey());
                pstmt.setString(2, entry.getValue());
                pstmt.addBatch();
                if (++count % 1000 == 0) {
                    pstmt.executeBatch();
                }
            }
            pstmt.executeBatch();
            plugin.getLogger().info("Total " + count + " translations inserted or updated");
        }
        return true;
    }

    private void clearCache() {
        if (translationsCache != null) {
            translationsCache.clear();
            translationsCache = null;
            System.gc(); // 垃圾回收
        }
    }
}
