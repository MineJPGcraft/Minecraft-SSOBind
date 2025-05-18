# Minecraft SSO绑定插件

## 简介

Minecraft SSO绑定插件是一个用于Spigot服务器的插件，允许玩家将他们的游戏账号与SSO（单点登录）系统绑定。该插件支持各种OAuth2模式的SSO系统，提供了灵活的配置选项，可以适配不同的SSO服务提供商。

## 主要功能

- 支持OAuth2模式的SSO账号绑定
- 监听指定端口接收SSO回调
- 玩家可通过`/ssobind`或`/sb`指令查看绑定状态或获取绑定地址
- 支持SQLite和MySQL/MariaDB数据存储
- 可配置自定义用户数据字段，适配不同SSO系统
- 管理员可查看和管理玩家绑定信息

## 安装要求

- Minecraft服务器：Spigot 1.13.2或更高版本
- Java：Java 8或更高版本
- 数据库：SQLite（默认）或MySQL/MariaDB（可选）

## 安装步骤

1. 下载插件JAR文件
2. 将JAR文件放入服务器的`plugins`目录
3. 启动或重启服务器
4. 编辑生成的配置文件`plugins/MinecraftSSOPlugin/config.yml`
5. 重新加载插件或重启服务器

## 配置说明

### 基本配置

```yaml
# 基本设置
settings:
  # 回调服务器监听的端口
  callback_port: 8080
  # 回调路径
  callback_path: "/oauth/callback"
  # 外部访问URL（必须包含协议、域名/IP和端口）
  external_url: "http://your-server-ip:8080"
```

### OAuth2配置

```yaml
# OAuth2设置
oauth:
  # OAuth提供者类型（目前仅支持generic）
  provider: "generic"
  # 授权URL
  auth_url: "https://your-sso-server.com/oauth/authorize"
  # 令牌URL
  token_url: "https://your-sso-server.com/oauth/token"
  # 用户信息URL
  userinfo_url: "https://your-sso-server.com/api/userinfo"
  # 客户端ID
  client_id: "your-client-id"
  # 客户端密钥
  client_secret: "your-client-secret"
  # 授权作用域（多个作用域用空格分隔）
  scope: "profile email"
```

### 用户数据字段配置

```yaml
# 用户数据字段配置
user_fields:
  # 用户ID字段（必须）
  id_field: "id"
  # 用户名字段
  username_field: "name"
  # 邮箱字段
  email_field: "email"
  # 自定义字段
  custom_fields:
    # 字段1
    field1:
      name: "昵称"
      path: "profile.nickname"
    # 字段2
    field2:
      name: "手机号"
      path: "phone_number"
```

### 数据库配置

```yaml
# 数据库设置
database:
  # 数据库类型（sqlite或mysql）
  type: "sqlite"
  # SQLite设置
  sqlite:
    # 数据库文件路径
    file: "plugins/MinecraftSSOPlugin/database.db"
  # MySQL设置
  mysql:
    # 主机
    host: "localhost"
    # 端口
    port: 3306
    # 数据库名
    database: "minecraft_sso"
    # 用户名
    username: "root"
    # 密码
    password: "your-password"
    # 连接池设置
    max_pool_size: 10
    min_idle: 5
    idle_timeout: 30000
```

### 消息配置

```yaml
# 消息设置
messages:
  # 无权限消息
  no_permission: "&c您没有权限执行此命令！"
  # 未绑定消息
  not_bound: "&e您尚未绑定SSO账号，请点击以下链接进行绑定：\n&b%bind_url%"
  # 已绑定消息
  already_bound: "&a您已绑定SSO账号：\n&e用户名：&f%username%\n&e邮箱：&f%email%\n\n&7使用 &f/ssobind unbind &7解除绑定"
```

## 指令

### 玩家指令

- `/ssobind` 或 `/sb` - 查看绑定状态或获取绑定地址
- `/ssobind unbind` - 解除当前绑定

### 管理员指令

- `/ssobind reload` - 重新加载插件配置
- `/ssobind status` - 查看插件状态
- `/ssobind list [页码]` - 查看所有绑定玩家列表
- `/ssobind info <玩家名>` - 查看指定玩家的绑定信息
- `/ssobind unbind <玩家名>` - 解除指定玩家的绑定

## 权限

- `ssoplugin.bind` - 允许玩家绑定SSO账号
- `ssoplugin.unbind` - 允许玩家解除自己的绑定
- `ssoplugin.admin` - 允许管理员使用管理指令

## 常见问题

### 回调服务器无法启动

确保配置的端口未被其他程序占用，并且服务器防火墙已开放该端口。

### 无法连接到MySQL数据库

检查MySQL服务器是否正常运行，以及配置的主机、端口、用户名和密码是否正确。

### 绑定失败

检查OAuth2配置是否正确，特别是客户端ID、客户端密钥和回调URL。

## 开发者信息

如需二次开发，请参考源码中的注释和文档。插件使用了以下主要依赖：

- Spigot API
- HikariCP（MySQL连接池）
- SQLite JDBC
- MySQL Connector/J
- JSON库

## 许可证

本插件采用MIT许可证。详情请参阅LICENSE文件。
