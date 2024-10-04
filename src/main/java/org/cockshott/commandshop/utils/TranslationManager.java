package org.cockshott.commandshop.utils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TranslationManager {
    private final JavaPlugin plugin;
    private Map<String, String> translations;

    public TranslationManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadTranslations();
    }

    private void loadTranslations() {
        try (InputStreamReader reader = new InputStreamReader(
                plugin.getResource("zh_cn.json"), "UTF-8")) {
            Gson gson = new Gson();
            Type type = new TypeToken<HashMap<String, String>>(){}.getType();
            translations = gson.fromJson(reader, type);
            plugin.getLogger().info("从 JSON 加载 " + translations.size() + " 个翻译");
        } catch (Exception e) {
            plugin.getLogger().severe("载入翻译时出错： " + e.getMessage());
            translations = new HashMap<>(); // 确保 translations 不为 null
        }
    }

    private static int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) {
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    dp[i][j] = min(dp[i - 1][j - 1]
                                    + (s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1),
                            dp[i - 1][j] + 1,
                            dp[i][j - 1] + 1);
                }
            }
        }

        return dp[s1.length()][s2.length()];
    }

    private static int min(int a, int b, int c) {
        return Math.min(Math.min(a, b), c);
    }

    public String getTranslation(String key) {
        // 尝试直接匹配
        String directMatch = translations.get(key);
        if (directMatch != null) {
            return directMatch;
        }

        // 如果没有直接匹配，则尝试相似度匹配
        String bestMatch = null;
        int minDistance = Integer.MAX_VALUE;
        String lowercaseKey = key.toLowerCase();

        for (Map.Entry<String, String> entry : translations.entrySet()) {
            String currentKey = entry.getKey().toLowerCase();
            int distance = levenshteinDistance(lowercaseKey, currentKey);
            if (distance < minDistance) {
                minDistance = distance;
                bestMatch = entry.getValue();
            }
        }

        // 如果找到了相似的键，返回对应的翻译
        if (bestMatch != null) {
            return bestMatch;
        }

        // 如果没有找到任何匹配，返回原始的键
        return key;
    }

    private static boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    public void clearCache() {
        translations.clear();
        plugin.getLogger().info("已清除翻译缓存");
    }
}

