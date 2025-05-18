package com.minecraft.ssoplugin.oauth.providers;

import com.minecraft.ssoplugin.SSOPlugin;
import com.minecraft.ssoplugin.oauth.OAuthProvider;
import com.minecraft.ssoplugin.oauth.OAuthTokenResponse;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * 通用OAuth提供者实现，适用于大多数标准OAuth2.0服务
 */
public class GenericOAuthProvider implements OAuthProvider {
    
    private final SSOPlugin plugin;
    private final String authUrl;
    private final String tokenUrl;
    private final String userInfoUrl;
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
    private final String scope;
    
    /**
     * 构造函数
     * @param plugin 插件实例
     */
    public GenericOAuthProvider(SSOPlugin plugin) {
        this.plugin = plugin;
        this.authUrl = plugin.getConfigManager().getAuthUrl();
        this.tokenUrl = plugin.getConfigManager().getTokenUrl();
        this.userInfoUrl = plugin.getConfigManager().getUserInfoUrl();
        this.clientId = plugin.getConfigManager().getClientId();
        this.clientSecret = plugin.getConfigManager().getClientSecret();
        this.redirectUri = plugin.getConfigManager().getRedirectUri();
        this.scope = plugin.getConfigManager().getScope();
    }
    
    @Override
    public String generateAuthUrl(String state) {
        try {
            StringBuilder urlBuilder = new StringBuilder(authUrl);
            urlBuilder.append("?response_type=code");
            urlBuilder.append("&client_id=").append(URLEncoder.encode(clientId, StandardCharsets.UTF_8.name()));
            urlBuilder.append("&redirect_uri=").append(URLEncoder.encode(redirectUri, StandardCharsets.UTF_8.name()));
            
            if (scope != null && !scope.isEmpty()) {
                urlBuilder.append("&scope=").append(URLEncoder.encode(scope, StandardCharsets.UTF_8.name()));
            }
            
            urlBuilder.append("&state=").append(URLEncoder.encode(state, StandardCharsets.UTF_8.name()));
            
            return urlBuilder.toString();
        } catch (UnsupportedEncodingException e) {
            plugin.log(Level.SEVERE, "生成授权URL时出错: " + e.getMessage());
            return null;
        }
    }
    
    @Override
    public OAuthTokenResponse getAccessToken(String code) {
        HttpClient httpClient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(tokenUrl);
        
        // 设置请求参数
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("grant_type", "authorization_code"));
        params.add(new BasicNameValuePair("code", code));
        params.add(new BasicNameValuePair("client_id", clientId));
        params.add(new BasicNameValuePair("client_secret", clientSecret));
        params.add(new BasicNameValuePair("redirect_uri", redirectUri));
        
        try {
            httpPost.setEntity(new UrlEncodedFormEntity(params));
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");
            
            // 发送请求
            HttpResponse response = httpClient.execute(httpPost);
            HttpEntity entity = response.getEntity();
            
            if (entity != null) {
                String responseString = EntityUtils.toString(entity);
                JSONObject jsonResponse = new JSONObject(responseString);
                
                // 解析响应
                if (jsonResponse.has("access_token")) {
                    String accessToken = jsonResponse.getString("access_token");
                    String refreshToken = jsonResponse.optString("refresh_token", null);
                    long expiresIn = jsonResponse.optLong("expires_in", 3600);
                    String tokenType = jsonResponse.optString("token_type", "Bearer");
                    
                    return new OAuthTokenResponse(accessToken, refreshToken, expiresIn, tokenType);
                } else if (jsonResponse.has("error")) {
                    String error = jsonResponse.getString("error");
                    String errorDescription = jsonResponse.optString("error_description", "Unknown error");
                    plugin.log(Level.WARNING, "获取访问令牌失败: " + error + " - " + errorDescription);
                }
            }
        } catch (IOException | JSONException e) {
            plugin.log(Level.SEVERE, "获取访问令牌时出错: " + e.getMessage());
        }
        
        return null;
    }
    
    @Override
    public JSONObject getUserInfo(String accessToken) {
        HttpClient httpClient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(userInfoUrl);
        
        // 设置请求头
        httpGet.setHeader("Authorization", "Bearer " + accessToken);
        httpGet.setHeader("Accept", "application/json");
        
        try {
            // 发送请求
            HttpResponse response = httpClient.execute(httpGet);
            HttpEntity entity = response.getEntity();
            
            if (entity != null) {
                String responseString = EntityUtils.toString(entity);
                return new JSONObject(responseString);
            }
        } catch (IOException | JSONException e) {
            plugin.log(Level.SEVERE, "获取用户信息时出错: " + e.getMessage());
        }
        
        return null;
    }
    
    @Override
    public OAuthTokenResponse refreshAccessToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isEmpty()) {
            return null;
        }
        
        HttpClient httpClient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(tokenUrl);
        
        // 设置请求参数
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("grant_type", "refresh_token"));
        params.add(new BasicNameValuePair("refresh_token", refreshToken));
        params.add(new BasicNameValuePair("client_id", clientId));
        params.add(new BasicNameValuePair("client_secret", clientSecret));
        
        try {
            httpPost.setEntity(new UrlEncodedFormEntity(params));
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");
            
            // 发送请求
            HttpResponse response = httpClient.execute(httpPost);
            HttpEntity entity = response.getEntity();
            
            if (entity != null) {
                String responseString = EntityUtils.toString(entity);
                JSONObject jsonResponse = new JSONObject(responseString);
                
                // 解析响应
                if (jsonResponse.has("access_token")) {
                    String accessToken = jsonResponse.getString("access_token");
                    String newRefreshToken = jsonResponse.optString("refresh_token", refreshToken);
                    long expiresIn = jsonResponse.optLong("expires_in", 3600);
                    String tokenType = jsonResponse.optString("token_type", "Bearer");
                    
                    return new OAuthTokenResponse(accessToken, newRefreshToken, expiresIn, tokenType);
                } else if (jsonResponse.has("error")) {
                    String error = jsonResponse.getString("error");
                    String errorDescription = jsonResponse.optString("error_description", "Unknown error");
                    plugin.log(Level.WARNING, "刷新访问令牌失败: " + error + " - " + errorDescription);
                }
            }
        } catch (IOException | JSONException e) {
            plugin.log(Level.SEVERE, "刷新访问令牌时出错: " + e.getMessage());
        }
        
        return null;
    }
}
