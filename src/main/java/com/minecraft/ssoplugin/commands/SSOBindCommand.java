package com.minecraft.ssoplugin.commands;

import com.minecraft.ssoplugin.SSOPlugin;
import com.minecraft.ssoplugin.oauth.OAuthManager;
import com.minecraft.ssoplugin.storage.StorageManager;
import com.minecraft.ssoplugin.utils.Utils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.json.JSONObject;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;


/**
 * SSO绑定指令处理类
 */
public class SSOBindCommand implements CommandExecutor, TabCompleter {
    
    private final SSOPlugin plugin;
    
    /**
     * 构造函数
     * @param plugin 插件实例
     */
    public SSOBindCommand(SSOPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // 检查是否为玩家
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c此命令只能由玩家执行！");
            return true;
        }
        
        Player player = (Player) sender;
        
        // 处理子命令
        if (args.length > 0) {
            String subCommand = args[0].toLowerCase();
            
            switch (subCommand) {
                case "unbind":
                    // 检查权限
                    if (!player.hasPermission("ssoplugin.unbind")) {
                        player.sendMessage(Utils.colorize(plugin.getConfigManager().getMessage("no_permission")));
                        return true;
                    }
                    
                    // 处理解绑命令
                    handleUnbindCommand(player, args);
                    return true;
                    
                case "reload":
                    // 检查权限
                    if (!player.hasPermission("ssoplugin.admin")) {
                        player.sendMessage(Utils.colorize(plugin.getConfigManager().getMessage("no_permission")));
                        return true;
                    }
                    
                    // 处理重载命令
                    handleReloadCommand(player);
                    return true;
                    
                case "status":
                    // 检查权限
                    if (!player.hasPermission("ssoplugin.admin")) {
                        player.sendMessage(Utils.colorize(plugin.getConfigManager().getMessage("no_permission")));
                        return true;
                    }
                    
                    // 处理状态命令
                    handleStatusCommand(player);
                    return true;
                    
                case "list":
                    // 检查权限
                    if (!player.hasPermission("ssoplugin.admin")) {
                        player.sendMessage(Utils.colorize(plugin.getConfigManager().getMessage("no_permission")));
                        return true;
                    }
                    
                    // 处理列表命令
                    handleListCommand(player, args);
                    return true;
                    
                case "info":
                    // 检查权限
                    if (!player.hasPermission("ssoplugin.admin")) {
                        player.sendMessage(Utils.colorize(plugin.getConfigManager().getMessage("no_permission")));
                        return true;
                    }
                    
                    // 处理信息命令
                    handleInfoCommand(player, args);
                    return true;
                    
                default:
                    // 未知子命令
                    player.sendMessage("§c未知子命令！使用 /ssobind 查看帮助。");
                    return true;
            }
        }
        
        // 检查权限
        if (!player.hasPermission("ssoplugin.bind")) {
            player.sendMessage(Utils.colorize(plugin.getConfigManager().getMessage("no_permission")));
            return true;
        }
        
        // 处理主命令（查看绑定状态或获取绑定地址）
        handleMainCommand(player);
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // 子命令补全
            List<String> subCommands = new ArrayList<>();
            
            // 添加所有玩家可用的子命令
            if (sender.hasPermission("ssoplugin.unbind")) {
                subCommands.add("unbind");
            }
            
            // 添加管理员子命令
            if (sender.hasPermission("ssoplugin.admin")) {
                subCommands.add("reload");
                subCommands.add("status");
                subCommands.add("list");
                subCommands.add("info");
            }
            
            // 过滤匹配的子命令
            String input = args[0].toLowerCase();
            completions.addAll(subCommands.stream()
                    .filter(s -> s.startsWith(input))
                    .collect(Collectors.toList()));
        } else if (args.length == 2) {
            // 第二个参数补全
            if (args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("unbind")) {
                if (sender.hasPermission("ssoplugin.admin")) {
                    // 获取在线玩家列表
                    String input = args[1].toLowerCase();
                    plugin.getServer().getOnlinePlayers().forEach(player -> {
                        String name = player.getName();
                        if (name.toLowerCase().startsWith(input)) {
                            completions.add(name);
                        }
                    });
                }
            }
        }
        
        return completions;
    }
    
    /**
     * 处理主命令
     * @param player 玩家
     */
    private void handleMainCommand(Player player) {
        StorageManager storageManager = plugin.getStorageManager();
        UUID playerUuid = player.getUniqueId();
        
        // 检查玩家是否已绑定
        if (storageManager.isPlayerBound(playerUuid)) {
            // 获取绑定信息
            Map<String, Object> binding = storageManager.getBinding(playerUuid);
            if (binding != null) {
                // 提取用户数据
                String userData = (String) binding.get("user_data");
                JSONObject userDataJson = new JSONObject(userData);
                
                // 提取用户名和邮箱
                String username = Utils.extractField(userDataJson, plugin.getConfigManager().getUsernameField(), "未知用户");
                String email = Utils.extractField(userDataJson, plugin.getConfigManager().getEmailField(), "未知邮箱");
                
                // 显示已绑定消息
                player.sendMessage(Utils.colorize(plugin.getConfigManager().getMessage("already_bound")
                        .replace("%username%", username)
                        .replace("%email%", email)));
            } else {
                // 数据库中有记录但无法获取详细信息，可能是数据库错误
                player.sendMessage("§c无法获取绑定信息，请联系管理员。");
            }
        } else {
            // 玩家未绑定，生成授权URL
            OAuthManager oauthManager = plugin.getOAuthManager();
            String authUrl = oauthManager.generateAuthUrl(player);
            
            if (authUrl != null) {
                // 显示未绑定消息
                player.sendMessage(Utils.colorize(plugin.getConfigManager().getMessage("not_bound")
                        .replace("%bind_url%", authUrl)));
            } else {
                player.sendMessage("§c生成绑定链接失败，请联系管理员。");
            }
        }
    }
    
    /**
     * 处理解绑命令
     * @param player 玩家
     * @param args 命令参数
     */
    private void handleUnbindCommand(Player player, String[] args) {
        StorageManager storageManager = plugin.getStorageManager();
        
        // 管理员解绑其他玩家
        if (args.length > 1 && player.hasPermission("ssoplugin.admin")) {
            String targetName = args[1];
            Player targetPlayer = plugin.getServer().getPlayer(targetName);
            
            if (targetPlayer != null) {
                // 目标玩家在线
                UUID targetUuid = targetPlayer.getUniqueId();
                
                if (storageManager.isPlayerBound(targetUuid)) {
                    if (storageManager.deleteBinding(targetUuid)) {
                        player.sendMessage("§a成功解除玩家 " + targetName + " 的SSO账号绑定。");
                        targetPlayer.sendMessage(Utils.colorize(plugin.getConfigManager().getMessage("unbind_success")));
                    } else {
                        player.sendMessage("§c解除玩家 " + targetName + " 的SSO账号绑定失败，请检查数据库。");
                    }
                } else {
                    player.sendMessage("§c玩家 " + targetName + " 尚未绑定SSO账号。");
                }
            } else {
                // 目标玩家不在线，尝试通过名称查找
                player.sendMessage("§c玩家 " + targetName + " 不在线，无法解绑。");
            }
            
            return;
        }
        
        // 玩家解绑自己
        UUID playerUuid = player.getUniqueId();
        
        if (storageManager.isPlayerBound(playerUuid)) {
            if (storageManager.deleteBinding(playerUuid)) {
                player.sendMessage(Utils.colorize(plugin.getConfigManager().getMessage("unbind_success")));
            } else {
                player.sendMessage("§c解除SSO账号绑定失败，请联系管理员。");
            }
        } else {
            player.sendMessage("§c您尚未绑定SSO账号。");
        }
    }
    
    /**
     * 处理重载命令
     * @param player 玩家
     */
    private void handleReloadCommand(Player player) {
        if (plugin.reload()) {
            player.sendMessage("§a插件配置已重新加载。");
        } else {
            player.sendMessage("§c插件配置重新加载失败，请检查控制台错误信息。");
        }
    }
    
    /**
     * 处理状态命令
     * @param player 玩家
     */
    private void handleStatusCommand(Player player) {
        player.sendMessage("§e===== SSO绑定插件状态 =====");
        player.sendMessage("§e插件版本: §f" + plugin.getDescription().getVersion());
        player.sendMessage("§e回调服务器: §f" + (plugin.getCallbackServer() != null ? "运行中" : "未运行"));
        player.sendMessage("§e回调端口: §f" + plugin.getConfigManager().getCallbackPort());
        player.sendMessage("§e回调路径: §f" + plugin.getConfigManager().getCallbackPath());
        player.sendMessage("§e外部URL: §f" + plugin.getConfigManager().getExternalUrl());
        player.sendMessage("§e数据库类型: §f" + plugin.getConfigManager().getDatabaseType());
        player.sendMessage("§eOAuth提供者: §f" + plugin.getConfigManager().getOAuthProvider());
    }
    
    /**
     * 处理列表命令
     * @param player 玩家
     * @param args 命令参数
     */
    private void handleListCommand(Player player, String[] args) {
        // 解析页码
        int page = 1;
        if (args.length > 1) {
            try {
                page = Integer.parseInt(args[1]);
                if (page < 1) {
                    page = 1;
                }
            } catch (NumberFormatException e) {
                player.sendMessage("§c无效的页码: " + args[1]);
                return;
            }
        }
        
        // 每页显示的数量
        int pageSize = 10;
        
        // 获取绑定列表
        StorageManager storageManager = plugin.getStorageManager();
        List<Map<String, Object>> bindings = storageManager.getAllBindings(page, pageSize);
        
        if (bindings.isEmpty()) {
            player.sendMessage("§c没有找到绑定记录。");
            return;
        }
        
        player.sendMessage("§e===== SSO绑定列表 (第 " + page + " 页) =====");
        
        for (Map<String, Object> binding : bindings) {
            String playerName = (String) binding.get("player_name");
            String ssoId = (String) binding.get("sso_id");
            String userData = (String) binding.get("user_data");
            
            // 提取用户名
            String username = "未知用户";
            if (userData != null && !userData.isEmpty()) {
                try {
                    JSONObject userDataJson = new JSONObject(userData);
                    username = Utils.extractField(userDataJson, plugin.getConfigManager().getUsernameField(), "未知用户");
                } catch (Exception e) {
                    // 忽略JSON解析错误
                }
            }
            
            player.sendMessage("§e" + playerName + " §7- §f" + username + " §7(ID: " + ssoId + ")");
        }
        
        player.sendMessage("§e使用 §f/ssobind list <页码> §e查看更多结果。");
    }
    
    /**
     * 处理信息命令
     * @param player 玩家
     * @param args 命令参数
     */
    private void handleInfoCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§c用法: /ssobind info <玩家名>");
            return;
        }
        
        String targetName = args[1];
        Player targetPlayer = plugin.getServer().getPlayer(targetName);
        
        if (targetPlayer == null) {
            player.sendMessage("§c玩家 " + targetName + " 不在线。");
            return;
        }
        
        UUID targetUuid = targetPlayer.getUniqueId();
        StorageManager storageManager = plugin.getStorageManager();
        
        if (!storageManager.isPlayerBound(targetUuid)) {
            player.sendMessage("§c玩家 " + targetName + " 尚未绑定SSO账号。");
            return;
        }
        
        Map<String, Object> binding = storageManager.getBinding(targetUuid);
        if (binding == null) {
            player.sendMessage("§c无法获取玩家 " + targetName + " 的绑定信息。");
            return;
        }
        
        String ssoId = (String) binding.get("sso_id");
        String userData = (String) binding.get("user_data");
        Timestamp createdAt = (Timestamp) binding.get("created_at");
        
        player.sendMessage("§e===== 玩家 " + targetName + " 的绑定信息 =====");
        player.sendMessage("§eSSO ID: §f" + ssoId);
        
        // 提取用户数据
        if (userData != null && !userData.isEmpty()) {
            try {
                JSONObject userDataJson = new JSONObject(userData);
                
                // 提取用户名和邮箱
                String username = Utils.extractField(userDataJson, plugin.getConfigManager().getUsernameField(), "未知用户");
                String email = Utils.extractField(userDataJson, plugin.getConfigManager().getEmailField(), "未知邮箱");
                
                player.sendMessage("§e用户名: §f" + username);
                player.sendMessage("§e邮箱: §f" + email);
                
                // 提取自定义字段
                List<Map<String, String>> customFields = plugin.getConfigManager().getCustomFields();
                for (Map<String, String> field : customFields) {
                    String name = field.get("name");
                    String path = field.get("path");
                    String value = Utils.extractField(userDataJson, path, "未知");
                    
                    player.sendMessage("§e" + name + ": §f" + value);
                }
            } catch (Exception e) {
                player.sendMessage("§c无法解析用户数据: " + e.getMessage());
            }
        }
        
        if (createdAt != null) {
            player.sendMessage("§e绑定时间: §f" + createdAt);
        }
    }
}
