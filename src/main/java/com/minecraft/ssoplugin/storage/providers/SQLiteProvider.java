package com.minecraft.ssoplugin.storage.providers;

import com.minecraft.ssoplugin.SSOPlugin;
import com.minecraft.ssoplugin.storage.StorageProvider;
import org.json.JSONObject;

import java.io.File;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.logging.Level;

/**
 * SQLite存储提供者实现
 */
public class SQLiteProvider implements StorageProvider {
    
    private final SSOPlugin plugin;
    private final String dbFile;
    private Connection connection;
    
    /**
     * 构造函数
     * @param plugin 插件实例
     */
    public SQLiteProvider(SSOPlugin plugin) {
        this.plugin = plugin;
        this.dbFile = plugin.getConfigManager().getSqliteFile();
    }
    
    @Override
    public boolean initialize() {
        try {
            // 确保数据库目录存在
            File dataFolder = new File(dbFile).getParentFile();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }
            
            // 加载SQLite JDBC驱动
            Class.forName("org.sqlite.JDBC");
            
            // 创建连接
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile);
            
            // 创建表
            createTables();
            
            return true;
        } catch (ClassNotFoundException | SQLException e) {
            plugin.log(Level.SEVERE, "初始化SQLite数据库时出错: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    @Override
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.log(Level.WARNING, "关闭SQLite连接时出错: " + e.getMessage());
        }
    }
    
    /**
     * 创建数据库表
     * @throws SQLException 如果创建表时出错
     */
    private void createTables() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            // 创建玩家绑定表
            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS player_bindings (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "player_uuid VARCHAR(36) NOT NULL, " +
                            "player_name VARCHAR(16) NOT NULL, " +
                            "sso_id VARCHAR(255) NOT NULL, " +
                            "access_token VARCHAR(255), " +
                            "refresh_token VARCHAR(255), " +
                            "token_expires_at TIMESTAMP, " +
                            "user_data TEXT, " +
                            "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                            "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                            "UNIQUE(player_uuid), " +
                            "UNIQUE(sso_id)" +
                            ")"
            );
        }
    }
    
    @Override
    public boolean saveBinding(UUID playerUuid, String playerName, String ssoId, 
                              String accessToken, String refreshToken, long expiresIn, String userData) {
        String sql = "INSERT OR REPLACE INTO player_bindings " +
                "(player_uuid, player_name, sso_id, access_token, refresh_token, token_expires_at, user_data, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerUuid.toString());
            statement.setString(2, playerName);
            statement.setString(3, ssoId);
            statement.setString(4, accessToken);
            statement.setString(5, refreshToken);
            
            // 计算令牌过期时间
            Timestamp expiresAt = null;
            if (expiresIn > 0) {
                expiresAt = new Timestamp(System.currentTimeMillis() + (expiresIn * 1000));
            }
            statement.setTimestamp(6, expiresAt);
            
            statement.setString(7, userData);
            statement.setTimestamp(8, new Timestamp(System.currentTimeMillis()));
            
            int rowsAffected = statement.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            plugin.log(Level.SEVERE, "保存绑定信息时出错: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public Map<String, Object> getBinding(UUID playerUuid) {
        String sql = "SELECT * FROM player_bindings WHERE player_uuid = ?";
        
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerUuid.toString());
            
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSetToMap(resultSet);
                }
            }
        } catch (SQLException e) {
            plugin.log(Level.SEVERE, "获取绑定信息时出错: " + e.getMessage());
        }
        
        return null;
    }
    
    @Override
    public Map<String, Object> getBindingBySsoId(String ssoId) {
        String sql = "SELECT * FROM player_bindings WHERE sso_id = ?";
        
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, ssoId);
            
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSetToMap(resultSet);
                }
            }
        } catch (SQLException e) {
            plugin.log(Level.SEVERE, "通过SSO ID获取绑定信息时出错: " + e.getMessage());
        }
        
        return null;
    }
    
    @Override
    public boolean deleteBinding(UUID playerUuid) {
        String sql = "DELETE FROM player_bindings WHERE player_uuid = ?";
        
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerUuid.toString());
            
            int rowsAffected = statement.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            plugin.log(Level.SEVERE, "删除绑定信息时出错: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public List<Map<String, Object>> getAllBindings(int page, int pageSize) {
        List<Map<String, Object>> bindings = new ArrayList<>();
        
        // 计算偏移量
        int offset = (page - 1) * pageSize;
        
        String sql = "SELECT * FROM player_bindings ORDER BY created_at DESC LIMIT ? OFFSET ?";
        
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, pageSize);
            statement.setInt(2, offset);
            
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    bindings.add(resultSetToMap(resultSet));
                }
            }
        } catch (SQLException e) {
            plugin.log(Level.SEVERE, "获取所有绑定信息时出错: " + e.getMessage());
        }
        
        return bindings;
    }
    
    @Override
    public boolean isPlayerBound(UUID playerUuid) {
        String sql = "SELECT COUNT(*) FROM player_bindings WHERE player_uuid = ?";
        
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerUuid.toString());
            
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            plugin.log(Level.SEVERE, "检查玩家是否已绑定时出错: " + e.getMessage());
        }
        
        return false;
    }
    
    @Override
    public boolean isSSoIdBound(String ssoId) {
        String sql = "SELECT COUNT(*) FROM player_bindings WHERE sso_id = ?";
        
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, ssoId);
            
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            plugin.log(Level.SEVERE, "检查SSO ID是否已绑定时出错: " + e.getMessage());
        }
        
        return false;
    }
    
    @Override
    public boolean updateToken(UUID playerUuid, String accessToken, String refreshToken, long expiresIn) {
        String sql = "UPDATE player_bindings SET access_token = ?, refresh_token = ?, token_expires_at = ?, updated_at = ? " +
                "WHERE player_uuid = ?";
        
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, accessToken);
            statement.setString(2, refreshToken);
            
            // 计算令牌过期时间
            Timestamp expiresAt = null;
            if (expiresIn > 0) {
                expiresAt = new Timestamp(System.currentTimeMillis() + (expiresIn * 1000));
            }
            statement.setTimestamp(3, expiresAt);
            
            statement.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
            statement.setString(5, playerUuid.toString());
            
            int rowsAffected = statement.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            plugin.log(Level.SEVERE, "更新令牌时出错: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean updatePlayerName(UUID playerUuid, String playerName) {
        String sql = "UPDATE player_bindings SET player_name = ?, updated_at = ? WHERE player_uuid = ?";
        
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerName);
            statement.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
            statement.setString(3, playerUuid.toString());
            
            int rowsAffected = statement.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            plugin.log(Level.SEVERE, "更新玩家名称时出错: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 将ResultSet转换为Map
     * @param resultSet 结果集
     * @return 映射
     * @throws SQLException 如果转换时出错
     */
    private Map<String, Object> resultSetToMap(ResultSet resultSet) throws SQLException {
        Map<String, Object> map = new HashMap<>();
        
        ResultSetMetaData metaData = resultSet.getMetaData();
        int columnCount = metaData.getColumnCount();
        
        for (int i = 1; i <= columnCount; i++) {
            String columnName = metaData.getColumnName(i);
            Object value = resultSet.getObject(i);
            map.put(columnName, value);
        }
        
        return map;
    }
}
