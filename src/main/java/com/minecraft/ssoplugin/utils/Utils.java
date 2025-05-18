package com.minecraft.ssoplugin.utils;

import org.bukkit.ChatColor;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 工具类，提供各种实用方法
 */
public class Utils {
    
    /**
     * 将颜色代码转换为Minecraft颜色
     * @param message 消息
     * @return 转换后的消息
     */
    public static String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    
    /**
     * 从JSON对象中提取字段值
     * @param json JSON对象
     * @param fieldPath 字段路径
     * @param defaultValue 默认值
     * @return 字段值
     */
    public static String extractField(JSONObject json, String fieldPath, String defaultValue) {
        if (fieldPath == null || fieldPath.isEmpty() || json == null) {
            return defaultValue;
        }
        
        // 处理嵌套字段路径，如 "profile.display_name"
        String[] parts = fieldPath.split("\\.");
        JSONObject current = json;
        
        for (int i = 0; i < parts.length - 1; i++) {
            if (!current.has(parts[i])) {
                return defaultValue;
            }
            
            Object obj = current.get(parts[i]);
            if (!(obj instanceof JSONObject)) {
                return defaultValue;
            }
            
            current = (JSONObject) obj;
        }
        
        String lastPart = parts[parts.length - 1];
        if (!current.has(lastPart)) {
            return defaultValue;
        }
        
        return current.getString(lastPart);
    }
    
    /**
     * 构建URL查询参数
     * @param params 参数映射
     * @return URL编码的查询字符串
     */
    public static String buildQueryParams(Map<String, String> params) {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (first) {
                first = false;
            } else {
                builder.append("&");
            }
            
            try {
                builder.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8.name()))
                       .append("=")
                       .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8.name()));
            } catch (UnsupportedEncodingException e) {
                // 这不应该发生，因为UTF-8总是支持的
                throw new RuntimeException("不支持的编码: UTF-8", e);
            }
        }
        
        return builder.toString();
    }
    
    /**
     * 解析查询参数
     * @param query 查询字符串
     * @return 参数映射
     */
    public static Map<String, String> parseQueryParams(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null || query.isEmpty()) {
            return params;
        }
        
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            if (idx > 0) {
                String key = pair.substring(0, idx);
                String value = pair.substring(idx + 1);
                params.put(key, value);
            }
        }
        
        return params;
    }
}
