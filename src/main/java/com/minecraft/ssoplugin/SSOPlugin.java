package com.minecraft.ssoplugin;

import com.minecraft.ssoplugin.commands.CommandManager;
import com.minecraft.ssoplugin.config.ConfigManager;
import com.minecraft.ssoplugin.http.CallbackServer;
import com.minecraft.ssoplugin.oauth.OAuthManager;
import com.minecraft.ssoplugin.storage.StorageManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

/**
 * SSO绑定插件主类
 */
public class SSOPlugin extends JavaPlugin implements Listener {
    
    private ConfigManager configManager;
    private StorageManager storageManager;
    private OAuthManager oauthManager;
    private CommandManager commandManager;
    private CallbackServer callbackServer;
    
    @Override
    public void onEnable() {
        // 保存默认配置
        saveDefaultConfig();
        
        // 初始化配置管理器
        configManager = new ConfigManager(this);
        if (!configManager.load()) {
            getLogger().severe("加载配置失败，插件无法启动！");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // 初始化存储管理器
        storageManager = new StorageManager(this);
        if (!storageManager.initialize()) {
            getLogger().severe("初始化数据库失败，插件无法启动！");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // 初始化OAuth管理器
        oauthManager = new OAuthManager(this);
        
        // 初始化命令管理器
        commandManager = new CommandManager(this);
        commandManager.registerCommands();
        
        // 启动回调服务器
        callbackServer = new CallbackServer(this);
        if (!callbackServer.start()) {
            getLogger().severe("启动回调服务器失败，插件无法正常工作！");
        }
        
        // 注册事件监听器
        getServer().getPluginManager().registerEvents(this, this);
        
        getLogger().info("SSO绑定插件已启动！");
    }
    
    @Override
    public void onDisable() {
        // 关闭回调服务器
        if (callbackServer != null) {
            callbackServer.stop();
        }
        
        // 关闭存储管理器
        if (storageManager != null) {
            storageManager.close();
        }
        
        getLogger().info("SSO绑定插件已关闭！");
    }
    
    /**
     * 重新加载插件
     * @return 是否重新加载成功
     */
    public boolean reload() {
        // 重新加载配置
        reloadConfig();
        if (!configManager.load()) {
            getLogger().severe("重新加载配置失败！");
            return false;
        }
        
        // 重启回调服务器
        if (callbackServer != null) {
            callbackServer.stop();
        }
        
        callbackServer = new CallbackServer(this);
        if (!callbackServer.start()) {
            getLogger().severe("重启回调服务器失败！");
            return false;
        }
        
        return true;
    }
    
    /**
     * 记录日志
     * @param level 日志级别
     * @param message 日志消息
     */
    public void log(Level level, String message) {
        getLogger().log(level, message);
    }
    
    /**
     * 玩家加入事件处理
     * @param event 玩家加入事件
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // 更新玩家名称
        if (storageManager.isPlayerBound(player.getUniqueId())) {
            storageManager.updatePlayerName(player.getUniqueId(), player.getName());
        }
    }
    
    /**
     * 获取配置管理器
     * @return 配置管理器
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    /**
     * 获取存储管理器
     * @return 存储管理器
     */
    public StorageManager getStorageManager() {
        return storageManager;
    }
    
    /**
     * 获取OAuth管理器
     * @return OAuth管理器
     */
    public OAuthManager getOAuthManager() {
        return oauthManager;
    }
    
    /**
     * 获取回调服务器
     * @return 回调服务器
     */
    public CallbackServer getCallbackServer() {
        return callbackServer;
    }
}
