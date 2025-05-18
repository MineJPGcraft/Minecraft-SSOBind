package com.minecraft.ssoplugin.commands;

import com.minecraft.ssoplugin.SSOPlugin;
import org.bukkit.command.PluginCommand;

/**
 * 命令管理器类，负责注册和管理插件命令
 */
public class CommandManager {
    
    private final SSOPlugin plugin;
    private final SSOBindCommand ssoBindCommand;
    
    /**
     * 构造函数
     * @param plugin 插件实例
     */
    public CommandManager(SSOPlugin plugin) {
        this.plugin = plugin;
        this.ssoBindCommand = new SSOBindCommand(plugin);
    }
    
    /**
     * 注册所有命令
     */
    public void registerCommands() {
        // 注册 ssobind 命令
        PluginCommand ssoBindCmd = plugin.getCommand("ssobind");
        if (ssoBindCmd != null) {
            ssoBindCmd.setExecutor(ssoBindCommand);
            ssoBindCmd.setTabCompleter(ssoBindCommand);
        } else {
            plugin.getLogger().severe("无法注册 ssobind 命令！");
        }
    }
}
