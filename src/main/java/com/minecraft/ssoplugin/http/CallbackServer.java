package com.minecraft.ssoplugin.http;

import com.minecraft.ssoplugin.SSOPlugin;
import com.minecraft.ssoplugin.oauth.OAuthManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.logging.Level;

/**
 * 回调服务器类，负责监听OAuth回调
 */
public class CallbackServer {
    
    private final SSOPlugin plugin;
    private HttpServer server;
    private final int port;
    private final String callbackPath;
    
    /**
     * 构造函数
     * @param plugin 插件实例
     */
    public CallbackServer(SSOPlugin plugin) {
        this.plugin = plugin;
        this.port = plugin.getConfigManager().getCallbackPort();
        this.callbackPath = plugin.getConfigManager().getCallbackPath();
    }
    
    /**
     * 启动回调服务器
     * @return 是否成功启动
     */
    public boolean start() {
        try {
            // 创建HTTP服务器
            server = HttpServer.create(new InetSocketAddress(port), 0);
            
            // 设置回调处理器
            server.createContext(callbackPath, new CallbackHandler(plugin));
            
            // 设置线程池
            server.setExecutor(Executors.newCachedThreadPool());
            
            // 启动服务器
            server.start();
            
            plugin.log(Level.INFO, "回调服务器已启动，监听端口: " + port);
            return true;
        } catch (IOException e) {
            plugin.log(Level.SEVERE, "无法启动回调服务器: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 停止回调服务器
     */
    public void stop() {
        if (server != null) {
            server.stop(0);
            plugin.log(Level.INFO, "回调服务器已停止");
        }
    }
    
    /**
     * 回调处理器类
     */
    private static class CallbackHandler implements HttpHandler {
        
        private final SSOPlugin plugin;
        
        /**
         * 构造函数
         * @param plugin 插件实例
         */
        public CallbackHandler(SSOPlugin plugin) {
            this.plugin = plugin;
        }
        
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // 获取请求方法
            String requestMethod = exchange.getRequestMethod();
            
            // 只处理GET请求
            if (!requestMethod.equalsIgnoreCase("GET")) {
                sendResponse(exchange, 405, "Method Not Allowed");
                return;
            }
            
            // 解析查询参数
            String query = exchange.getRequestURI().getQuery();
            Map<String, String> params = parseQueryParams(query);
            
            // 检查是否有错误
            if (params.containsKey("error")) {
                String error = params.get("error");
                String errorDescription = params.getOrDefault("error_description", "Unknown error");
                plugin.log(Level.WARNING, "OAuth回调错误: " + error + " - " + errorDescription);
                sendResponse(exchange, 400, "认证失败: " + errorDescription);
                return;
            }
            
            // 检查是否有授权码
            if (!params.containsKey("code")) {
                plugin.log(Level.WARNING, "OAuth回调缺少授权码");
                sendResponse(exchange, 400, "缺少授权码");
                return;
            }
            
            // 检查是否有状态参数
            if (!params.containsKey("state")) {
                plugin.log(Level.WARNING, "OAuth回调缺少状态参数");
                sendResponse(exchange, 400, "缺少状态参数");
                return;
            }
            
            // 获取授权码和状态
            String code = params.get("code");
            String state = params.get("state");
            
            // 处理OAuth回调
            try {
                OAuthManager oauthManager = plugin.getOAuthManager();
                boolean success = oauthManager.handleCallback(code, state);
                
                if (success) {
                    sendResponse(exchange, 200, "绑定成功！您现在可以关闭此页面并返回游戏。");
                } else {
                    sendResponse(exchange, 400, "绑定失败，请重试。");
                }
            } catch (Exception e) {
                plugin.log(Level.SEVERE, "处理OAuth回调时出错: " + e.getMessage());
                e.printStackTrace();
                sendResponse(exchange, 500, "服务器内部错误");
            }
        }
        
        /**
         * 解析查询参数
         * @param query 查询字符串
         * @return 参数映射
         */
        private Map<String, String> parseQueryParams(String query) {
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
        
        /**
         * 发送HTTP响应
         * @param exchange HTTP交换
         * @param statusCode 状态码
         * @param response 响应内容
         * @throws IOException 如果发送响应时出错
         */
        private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
            // 构建HTML响应
            String html = "<!DOCTYPE html>\n" +
                    "<html>\n" +
                    "<head>\n" +
                    "    <meta charset=\"UTF-8\">\n" +
                    "    <title>Minecraft SSO绑定</title>\n" +
                    "    <style>\n" +
                    "        body {\n" +
                    "            font-family: Arial, sans-serif;\n" +
                    "            background-color: #f0f0f0;\n" +
                    "            margin: 0;\n" +
                    "            padding: 0;\n" +
                    "            display: flex;\n" +
                    "            justify-content: center;\n" +
                    "            align-items: center;\n" +
                    "            height: 100vh;\n" +
                    "        }\n" +
                    "        .container {\n" +
                    "            background-color: white;\n" +
                    "            border-radius: 8px;\n" +
                    "            box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);\n" +
                    "            padding: 40px;\n" +
                    "            text-align: center;\n" +
                    "            max-width: 500px;\n" +
                    "        }\n" +
                    "        h1 {\n" +
                    "            color: #333;\n" +
                    "            margin-bottom: 20px;\n" +
                    "        }\n" +
                    "        p {\n" +
                    "            color: #666;\n" +
                    "            line-height: 1.6;\n" +
                    "        }\n" +
                    "        .success {\n" +
                    "            color: #4CAF50;\n" +
                    "        }\n" +
                    "        .error {\n" +
                    "            color: #F44336;\n" +
                    "        }\n" +
                    "    </style>\n" +
                    "</head>\n" +
                    "<body>\n" +
                    "    <div class=\"container\">\n" +
                    "        <h1>Minecraft SSO绑定</h1>\n" +
                    "        <p class=\"" + (statusCode == 200 ? "success" : "error") + "\">" + response + "</p>\n" +
                    "    </div>\n" +
                    "</body>\n" +
                    "</html>";
            
            // 设置响应头
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            byte[] responseBytes = html.getBytes(StandardCharsets.UTF_8);
            
            // 发送响应
            exchange.sendResponseHeaders(statusCode, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        }
    }
}
