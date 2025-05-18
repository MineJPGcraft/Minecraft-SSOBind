package com.minecraft.ssoplugin.config;

import com.minecraft.ssoplugin.SSOPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * 配置管理器类，负责管理插件配置
 */
public class ConfigManager {
    
    private final SSOPlugin plugin;
    private FileConfiguration config;
    
    // 基本设置
    private int callbackPort;
    private String callbackPath;
    private String externalUrl;
    private String redirectUri;
    
    // OAuth2设置
    private String oauthProvider;
    private String authUrl;
    private String tokenUrl;
    private String userInfoUrl;
    private String clientId;
    private String clientSecret;
    private String scope;
    
    // 用户数据字段
    private String idField;
    private String usernameField;
    private String emailField;
    private List<Map<String, String>> customFields;
    
    // 数据库设置
    private String databaseType;
    private String sqliteFile;
    private String mysqlHost;
    private int mysqlPort;
    private String mysqlDatabase;
    private String mysqlUsername;
    private String mysqlPassword;
    private int mysqlMaxPoolSize;
    private int mysqlMinIdle;
    private int mysqlIdleTimeout;
    
    // 消息设置
    private Map<String, String> messages;
    
    /**
     * 构造函数
     * @param plugin 插件实例
     */
    public ConfigManager(SSOPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * 加载配置
     * @return 是否加载成功
     */
    public boolean load() {
        try {
            // 获取配置
            config = plugin.getConfig();
            
            // 加载基本设置
            loadBasicSettings();
            
            // 加载OAuth2设置
            loadOAuth2Settings();
            
            // 加载用户数据字段
            loadUserDataFields();
            
            // 加载数据库设置
            loadDatabaseSettings();
            
            // 加载消息设置
            loadMessages();
            
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "加载配置时出错: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 加载基本设置
     */
    private void loadBasicSettings() {
        ConfigurationSection settings = config.getConfigurationSection("settings");
        
        if (settings != null) {
            callbackPort = settings.getInt("callback_port", 8080);
            callbackPath = settings.getString("callback_path", "/oauth/callback");
            externalUrl = settings.getString("external_url", "http://localhost:" + callbackPort);
            redirectUri = externalUrl + callbackPath;
        } else {
            // 使用默认值
            callbackPort = 8080;
            callbackPath = "/oauth/callback";
            externalUrl = "http://localhost:" + callbackPort;
            redirectUri = externalUrl + callbackPath;
        }
    }
    
    /**
     * 加载OAuth2设置
     */
    private void loadOAuth2Settings() {
        ConfigurationSection oauth = config.getConfigurationSection("oauth");
        
        if (oauth != null) {
            oauthProvider = oauth.getString("provider", "generic");
            authUrl = oauth.getString("auth_url");
            tokenUrl = oauth.getString("token_url");
            userInfoUrl = oauth.getString("userinfo_url");
            clientId = oauth.getString("client_id");
            clientSecret = oauth.getString("client_secret");
            scope = oauth.getString("scope");
        } else {
            // 使用默认值
            oauthProvider = "generic";
            authUrl = "";
            tokenUrl = "";
            userInfoUrl = "";
            clientId = "";
            clientSecret = "";
            scope = "";
        }
    }
    
    /**
     * 加载用户数据字段
     */
    private void loadUserDataFields() {
        ConfigurationSection fields = config.getConfigurationSection("user_fields");
        
        if (fields != null) {
            idField = fields.getString("id_field", "id");
            usernameField = fields.getString("username_field", "name");
            emailField = fields.getString("email_field", "email");
            
            // 加载自定义字段
            customFields = new ArrayList<>();
            ConfigurationSection customFieldsSection = fields.getConfigurationSection("custom_fields");
            
            if (customFieldsSection != null) {
                for (String key : customFieldsSection.getKeys(false)) {
                    ConfigurationSection fieldSection = customFieldsSection.getConfigurationSection(key);
                    
                    if (fieldSection != null) {
                        Map<String, String> field = new HashMap<>();
                        field.put("name", fieldSection.getString("name", key));
                        field.put("path", fieldSection.getString("path", ""));
                        
                        customFields.add(field);
                    }
                }
            }
        } else {
            // 使用默认值
            idField = "id";
            usernameField = "name";
            emailField = "email";
            customFields = new ArrayList<>();
        }
    }
    
    /**
     * 加载数据库设置
     */
    private void loadDatabaseSettings() {
        ConfigurationSection database = config.getConfigurationSection("database");
        
        if (database != null) {
            databaseType = database.getString("type", "sqlite");
            
            // SQLite设置
            ConfigurationSection sqlite = database.getConfigurationSection("sqlite");
            if (sqlite != null) {
                sqliteFile = sqlite.getString("file", plugin.getDataFolder().getAbsolutePath() + "/database.db");
            } else {
                sqliteFile = plugin.getDataFolder().getAbsolutePath() + "/database.db";
            }
            
            // MySQL设置
            ConfigurationSection mysql = database.getConfigurationSection("mysql");
            if (mysql != null) {
                mysqlHost = mysql.getString("host", "localhost");
                mysqlPort = mysql.getInt("port", 3306);
                mysqlDatabase = mysql.getString("database", "minecraft_sso");
                mysqlUsername = mysql.getString("username", "root");
                mysqlPassword = mysql.getString("password", "");
                mysqlMaxPoolSize = mysql.getInt("max_pool_size", 10);
                mysqlMinIdle = mysql.getInt("min_idle", 5);
                mysqlIdleTimeout = mysql.getInt("idle_timeout", 30000);
            } else {
                mysqlHost = "localhost";
                mysqlPort = 3306;
                mysqlDatabase = "minecraft_sso";
                mysqlUsername = "root";
                mysqlPassword = "";
                mysqlMaxPoolSize = 10;
                mysqlMinIdle = 5;
                mysqlIdleTimeout = 30000;
            }
        } else {
            // 使用默认值
            databaseType = "sqlite";
            sqliteFile = plugin.getDataFolder().getAbsolutePath() + "/database.db";
            mysqlHost = "localhost";
            mysqlPort = 3306;
            mysqlDatabase = "minecraft_sso";
            mysqlUsername = "root";
            mysqlPassword = "";
            mysqlMaxPoolSize = 10;
            mysqlMinIdle = 5;
            mysqlIdleTimeout = 30000;
        }
    }
    
    /**
     * 加载消息设置
     */
    private void loadMessages() {
        messages = new HashMap<>();
        ConfigurationSection messagesSection = config.getConfigurationSection("messages");
        
        if (messagesSection != null) {
            for (String key : messagesSection.getKeys(false)) {
                messages.put(key, messagesSection.getString(key, ""));
            }
        }
        
        // 设置默认消息
        if (!messages.containsKey("no_permission")) {
            messages.put("no_permission", "&c您没有权限执行此命令！");
        }
        
        if (!messages.containsKey("not_bound")) {
            messages.put("not_bound", "&e您尚未绑定SSO账号，请点击以下链接进行绑定：\n&b%bind_url%");
        }
        
        if (!messages.containsKey("already_bound")) {
            messages.put("already_bound", "&a您已绑定SSO账号：\n&e用户名：&f%username%\n&e邮箱：&f%email%\n\n&7使用 &f/ssobind unbind &7解除绑定");
        }
        
        if (!messages.containsKey("bind_success")) {
            messages.put("bind_success", "&a绑定成功！您已成功绑定SSO账号：&f%username%");
        }
        
        if (!messages.containsKey("bind_fail")) {
            messages.put("bind_fail", "&c绑定失败：%reason%");
        }
        
        if (!messages.containsKey("unbind_success")) {
            messages.put("unbind_success", "&a解绑成功！您已成功解除SSO账号绑定。");
        }
    }
    
    /**
     * 获取回调端口
     * @return 回调端口
     */
    public int getCallbackPort() {
        return callbackPort;
    }
    
    /**
     * 获取回调路径
     * @return 回调路径
     */
    public String getCallbackPath() {
        return callbackPath;
    }
    
    /**
     * 获取外部URL
     * @return 外部URL
     */
    public String getExternalUrl() {
        return externalUrl;
    }
    
    /**
     * 获取重定向URI
     * @return 重定向URI
     */
    public String getRedirectUri() {
        return redirectUri;
    }
    
    /**
     * 获取OAuth提供者
     * @return OAuth提供者
     */
    public String getOAuthProvider() {
        return oauthProvider;
    }
    
    /**
     * 获取授权URL
     * @return 授权URL
     */
    public String getAuthUrl() {
        return authUrl;
    }
    
    /**
     * 获取令牌URL
     * @return 令牌URL
     */
    public String getTokenUrl() {
        return tokenUrl;
    }
    
    /**
     * 获取用户信息URL
     * @return 用户信息URL
     */
    public String getUserInfoUrl() {
        return userInfoUrl;
    }
    
    /**
     * 获取客户端ID
     * @return 客户端ID
     */
    public String getClientId() {
        return clientId;
    }
    
    /**
     * 获取客户端密钥
     * @return 客户端密钥
     */
    public String getClientSecret() {
        return clientSecret;
    }
    
    /**
     * 获取作用域
     * @return 作用域
     */
    public String getScope() {
        return scope;
    }
    
    /**
     * 获取ID字段
     * @return ID字段
     */
    public String getIdField() {
        return idField;
    }
    
    /**
     * 获取用户名字段
     * @return 用户名字段
     */
    public String getUsernameField() {
        return usernameField;
    }
    
    /**
     * 获取邮箱字段
     * @return 邮箱字段
     */
    public String getEmailField() {
        return emailField;
    }
    
    /**
     * 获取自定义字段
     * @return 自定义字段列表
     */
    public List<Map<String, String>> getCustomFields() {
        return customFields;
    }
    
    /**
     * 获取数据库类型
     * @return 数据库类型
     */
    public String getDatabaseType() {
        return databaseType;
    }
    
    /**
     * 获取SQLite文件路径
     * @return SQLite文件路径
     */
    public String getSqliteFile() {
        return sqliteFile;
    }
    
    /**
     * 获取MySQL主机
     * @return MySQL主机
     */
    public String getMysqlHost() {
        return mysqlHost;
    }
    
    /**
     * 获取MySQL端口
     * @return MySQL端口
     */
    public int getMysqlPort() {
        return mysqlPort;
    }
    
    /**
     * 获取MySQL数据库名
     * @return MySQL数据库名
     */
    public String getMysqlDatabase() {
        return mysqlDatabase;
    }
    
    /**
     * 获取MySQL用户名
     * @return MySQL用户名
     */
    public String getMysqlUsername() {
        return mysqlUsername;
    }
    
    /**
     * 获取MySQL密码
     * @return MySQL密码
     */
    public String getMysqlPassword() {
        return mysqlPassword;
    }
    
    /**
     * 获取MySQL最大连接池大小
     * @return MySQL最大连接池大小
     */
    public int getMysqlMaxPoolSize() {
        return mysqlMaxPoolSize;
    }
    
    /**
     * 获取MySQL最小空闲连接数
     * @return MySQL最小空闲连接数
     */
    public int getMysqlMinIdle() {
        return mysqlMinIdle;
    }
    
    /**
     * 获取MySQL空闲超时时间
     * @return MySQL空闲超时时间
     */
    public int getMysqlIdleTimeout() {
        return mysqlIdleTimeout;
    }
    
    /**
     * 获取消息
     * @param key 消息键
     * @return 消息内容
     */
    public String getMessage(String key) {
        return messages.getOrDefault(key, "");
    }
}
