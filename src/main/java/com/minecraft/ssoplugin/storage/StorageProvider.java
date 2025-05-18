package com.minecraft.ssoplugin.storage;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 存储提供者接口，定义数据存储的方法
 */
public interface StorageProvider {
    
    /**
     * 初始化存储提供者
     * @return 是否初始化成功
     */
    boolean initialize();
    
    /**
     * 关闭存储提供者
     */
    void close();
    
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
    boolean saveBinding(UUID playerUuid, String playerName, String ssoId, 
                       String accessToken, String refreshToken, long expiresIn, String userData);
    
    /**
     * 获取玩家绑定信息
     * @param playerUuid 玩家UUID
     * @return 绑定信息，如果不存在则返回null
     */
    Map<String, Object> getBinding(UUID playerUuid);
    
    /**
     * 获取SSO ID绑定信息
     * @param ssoId SSO ID
     * @return 绑定信息，如果不存在则返回null
     */
    Map<String, Object> getBindingBySsoId(String ssoId);
    
    /**
     * 删除绑定信息
     * @param playerUuid 玩家UUID
     * @return 是否删除成功
     */
    boolean deleteBinding(UUID playerUuid);
    
    /**
     * 获取所有绑定信息
     * @param page 页码（从1开始）
     * @param pageSize 每页大小
     * @return 绑定信息列表
     */
    List<Map<String, Object>> getAllBindings(int page, int pageSize);
    
    /**
     * 检查玩家是否已绑定
     * @param playerUuid 玩家UUID
     * @return 是否已绑定
     */
    boolean isPlayerBound(UUID playerUuid);
    
    /**
     * 检查SSO ID是否已绑定
     * @param ssoId SSO ID
     * @return 是否已绑定
     */
    boolean isSSoIdBound(String ssoId);
    
    /**
     * 更新访问令牌
     * @param playerUuid 玩家UUID
     * @param accessToken 新的访问令牌
     * @param refreshToken 新的刷新令牌
     * @param expiresIn 过期时间（秒）
     * @return 是否更新成功
     */
    boolean updateToken(UUID playerUuid, String accessToken, String refreshToken, long expiresIn);
    
    /**
     * 更新玩家名称
     * @param playerUuid 玩家UUID
     * @param playerName 新的玩家名称
     * @return 是否更新成功
     */
    boolean updatePlayerName(UUID playerUuid, String playerName);
}
