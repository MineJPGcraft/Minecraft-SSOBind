package com.minecraft.ssoplugin.oauth;

import com.minecraft.ssoplugin.SSOPlugin;
import com.minecraft.ssoplugin.oauth.providers.GenericOAuthProvider;
import com.minecraft.ssoplugin.storage.StorageManager;
import org.bukkit.entity.Player;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * OAuth管理器类，负责处理OAuth认证流程
 */
public class OAuthManager {
    
    private final SSOPlugin plugin;
    private final Map<String, PendingAuth> pendingAuths;
    private OAuthProvider provider;
    
    /**
     * 构造函数
     * @param plugin 插件实例
     */
    public OAuthManager(SSOPlugin plugin) {
        this.plugin = plugin;
        this.pendingAuths = new ConcurrentHashMap<>();
        initProvider();
    }
    
    /**
     * 初始化OAuth提供者
     */
    private void initProvider() {
        String providerType = plugin.getConfigManager().getOAuthProvider();
        
        // 目前只支持通用OAuth提供者
        // 未来可以根据providerType创建不同的提供者实现
        provider = new GenericOAuthProvider(plugin);
    }
    
    /**
     * 生成授权URL
     * @param player 玩家
     * @return 授权URL
     */
    public String generateAuthUrl(Player player) {
        // 生成状态参数，用于防止CSRF攻击
        String state = generateState(player.getUniqueId());
        
        // 将状态参数与玩家UUID关联，存储在待处理认证映射中
        pendingAuths.put(state, new PendingAuth(player.getUniqueId(), System.currentTimeMillis()));
        
        // 清理过期的待处理认证
        cleanupPendingAuths();
        
        // 生成授权URL
        return provider.generateAuthUrl(state);
    }
    
    /**
     * 处理OAuth回调
     * @param code 授权码
     * @param state 状态参数
     * @return 是否处理成功
     */
    public boolean handleCallback(String code, String state) {
        // 检查状态参数是否有效
        PendingAuth pendingAuth = pendingAuths.get(state);
        if (pendingAuth == null) {
            plugin.log(Level.WARNING, "无效的状态参数: " + state);
            return false;
        }
        
        // 移除待处理认证
        pendingAuths.remove(state);
        
        // 获取玩家UUID
        UUID playerUuid = pendingAuth.getPlayerUuid();
        
        try {
            // 使用授权码获取访问令牌
            OAuthTokenResponse tokenResponse = provider.getAccessToken(code);
            if (tokenResponse == null || tokenResponse.getAccessToken() == null) {
                plugin.log(Level.WARNING, "无法获取访问令牌");
                return false;
            }
            
            // 使用访问令牌获取用户信息
            JSONObject userInfo = provider.getUserInfo(tokenResponse.getAccessToken());
            if (userInfo == null) {
                plugin.log(Level.WARNING, "无法获取用户信息");
                return false;
            }
            
            // 提取用户ID
            String idField = plugin.getConfigManager().getIdField();
            if (!userInfo.has(idField)) {
                plugin.log(Level.WARNING, "用户信息中缺少ID字段: " + idField);
                return false;
            }
            
            String ssoId = userInfo.getString(idField);
            
            // 存储绑定信息
            StorageManager storageManager = plugin.getStorageManager();
            Player player = plugin.getServer().getPlayer(playerUuid);
            
            if (player != null) {
                // 检查该SSO ID是否已被其他玩家绑定
                if (storageManager.isSSoIdBound(ssoId) && !storageManager.isPlayerBound(playerUuid)) {
                    plugin.log(Level.WARNING, "SSO ID已被其他玩家绑定: " + ssoId);
                    
                    // 通知玩家
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        player.sendMessage(colorize(plugin.getConfigManager().getMessage("bind_fail")
                                .replace("%reason%", "此SSO账号已被其他玩家绑定")));
                    });
                    
                    return false;
                }
                
                // 存储绑定信息
                boolean success = storageManager.saveBinding(playerUuid, player.getName(), ssoId, 
                        tokenResponse.getAccessToken(), tokenResponse.getRefreshToken(), 
                        tokenResponse.getExpiresIn(), userInfo.toString());
                
                if (success) {
                    // 通知玩家绑定成功
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        // 提取用户名和邮箱
                        String username = extractField(userInfo, plugin.getConfigManager().getUsernameField(), "未知用户");
                        
                        player.sendMessage(colorize(plugin.getConfigManager().getMessage("bind_success")
                                .replace("%username%", username)));
                    });
                    
                    return true;
                } else {
                    // 通知玩家绑定失败
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        player.sendMessage(colorize(plugin.getConfigManager().getMessage("bind_fail")
                                .replace("%reason%", "数据库错误")));
                    });
                }
            }
            
            return false;
        } catch (Exception e) {
            plugin.log(Level.SEVERE, "处理OAuth回调时出错: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 从JSON对象中提取字段值
     * @param json JSON对象
     * @param fieldPath 字段路径
     * @param defaultValue 默认值
     * @return 字段值
     */
    private String extractField(JSONObject json, String fieldPath, String defaultValue) {
        if (fieldPath == null || fieldPath.isEmpty()) {
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
     * 生成状态参数
     * @param playerUuid 玩家UUID
     * @return 状态参数
     */
    private String generateState(UUID playerUuid) {
        return playerUuid.toString() + "-" + UUID.randomUUID().toString();
    }
    
    /**
     * 清理过期的待处理认证
     */
    private void cleanupPendingAuths() {
        long now = System.currentTimeMillis();
        long expirationTime = 10 * 60 * 1000; // 10分钟
        
        pendingAuths.entrySet().removeIf(entry -> 
            (now - entry.getValue().getTimestamp()) > expirationTime);
    }
    
    /**
     * 将颜色代码转换为Minecraft颜色
     * @param message 消息
     * @return 转换后的消息
     */
    private String colorize(String message) {
        return message.replace("&", "§");
    }
    
    /**
     * 待处理认证类
     */
    private static class PendingAuth {
        private final UUID playerUuid;
        private final long timestamp;
        
        public PendingAuth(UUID playerUuid, long timestamp) {
            this.playerUuid = playerUuid;
            this.timestamp = timestamp;
        }
        
        public UUID getPlayerUuid() {
            return playerUuid;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
    }
}
