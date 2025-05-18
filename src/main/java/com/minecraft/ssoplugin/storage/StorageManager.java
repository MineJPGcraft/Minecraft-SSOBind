package com.minecraft.ssoplugin.storage;

import com.minecraft.ssoplugin.SSOPlugin;
import com.minecraft.ssoplugin.storage.providers.MySQLProvider;
import com.minecraft.ssoplugin.storage.providers.SQLiteProvider;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * 存储管理器类，负责管理数据存储
 */
public class StorageManager {
    
    private final SSOPlugin plugin;
    private StorageProvider provider;
    
    /**
     * 构造函数
     * @param plugin 插件实例
     */
    public StorageManager(SSOPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * 初始化存储管理器
     * @return 是否初始化成功
     */
    public boolean initialize() {
        String databaseType = plugin.getConfigManager().getDatabaseType();
        
        // 根据配置选择存储提供者
        if (databaseType.equalsIgnoreCase("mysql")) {
            provider = new MySQLProvider(plugin);
        } else {
            provider = new SQLiteProvider(plugin);
        }
        
        // 初始化存储提供者
        boolean success = provider.initialize();
        if (!success) {
            plugin.log(Level.SEVERE, "无法初始化数据库提供者: " + databaseType);
            return false;
        }
        
        return true;
    }
    
    /**
     * 关闭存储管理器
     */
    public void close() {
        if (provider != null) {
            provider.close();
        }
    }
    
    /**
     * 保存绑定信息
     * @param playerUuid 玩家UUID
     * @param playerName 玩家名称
     * @param ssoId SSO ID
     * @param accessToken 访问令牌
     * @param refreshToken 刷新令牌
     * @param expiresIn 过期时间（秒）
     * @param userData 用户数据（JSON字符串）
     * @return 是否保存成功
     */
    public boolean saveBinding(UUID playerUuid, String playerName, String ssoId, 
                              String accessToken, String refreshToken, long expiresIn, String userData) {
        return provider.saveBinding(playerUuid, playerName, ssoId, accessToken, refreshToken, expiresIn, userData);
    }
    
    /**
     * 获取玩家绑定信息
     * @param playerUuid 玩家UUID
     * @return 绑定信息，如果不存在则返回null
     */
    public Map<String, Object> getBinding(UUID playerUuid) {
        return provider.getBinding(playerUuid);
    }
    
    /**
     * 获取SSO ID绑定信息
     * @param ssoId SSO ID
     * @return 绑定信息，如果不存在则返回null
     */
    public Map<String, Object> getBindingBySsoId(String ssoId) {
        return provider.getBindingBySsoId(ssoId);
    }
    
    /**
     * 删除绑定信息
     * @param playerUuid 玩家UUID
     * @return 是否删除成功
     */
    public boolean deleteBinding(UUID playerUuid) {
        return provider.deleteBinding(playerUuid);
    }
    
    /**
     * 获取所有绑定信息
     * @param page 页码（从1开始）
     * @param pageSize 每页大小
     * @return 绑定信息列表
     */
    public List<Map<String, Object>> getAllBindings(int page, int pageSize) {
        return provider.getAllBindings(page, pageSize);
    }
    
    /**
     * 检查玩家是否已绑定
     * @param playerUuid 玩家UUID
     * @return 是否已绑定
     */
    public boolean isPlayerBound(UUID playerUuid) {
        return provider.isPlayerBound(playerUuid);
    }
    
    /**
     * 检查SSO ID是否已绑定
     * @param ssoId SSO ID
     * @return 是否已绑定
     */
    public boolean isSSoIdBound(String ssoId) {
        return provider.isSSoIdBound(ssoId);
    }
    
    /**
     * 更新访问令牌
     * @param playerUuid 玩家UUID
     * @param accessToken 新的访问令牌
     * @param refreshToken 新的刷新令牌
     * @param expiresIn 过期时间（秒）
     * @return 是否更新成功
     */
    public boolean updateToken(UUID playerUuid, String accessToken, String refreshToken, long expiresIn) {
        return provider.updateToken(playerUuid, accessToken, refreshToken, expiresIn);
    }
    
    /**
     * 更新玩家名称
     * @param playerUuid 玩家UUID
     * @param playerName 新的玩家名称
     * @return 是否更新成功
     */
    public boolean updatePlayerName(UUID playerUuid, String playerName) {
        return provider.updatePlayerName(playerUuid, playerName);
    }
}
