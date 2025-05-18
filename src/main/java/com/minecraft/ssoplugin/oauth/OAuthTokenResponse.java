package com.minecraft.ssoplugin.oauth;

/**
 * OAuth令牌响应类，用于存储OAuth令牌信息
 */
public class OAuthTokenResponse {
    
    private final String accessToken;
    private final String refreshToken;
    private final long expiresIn;
    private final String tokenType;
    
    /**
     * 构造函数
     * @param accessToken 访问令牌
     * @param refreshToken 刷新令牌
     * @param expiresIn 过期时间（秒）
     * @param tokenType 令牌类型
     */
    public OAuthTokenResponse(String accessToken, String refreshToken, long expiresIn, String tokenType) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
        this.tokenType = tokenType;
    }
    
    /**
     * 获取访问令牌
     * @return 访问令牌
     */
    public String getAccessToken() {
        return accessToken;
    }
    
    /**
     * 获取刷新令牌
     * @return 刷新令牌
     */
    public String getRefreshToken() {
        return refreshToken;
    }
    
    /**
     * 获取过期时间（秒）
     * @return 过期时间
     */
    public long getExpiresIn() {
        return expiresIn;
    }
    
    /**
     * 获取令牌类型
     * @return 令牌类型
     */
    public String getTokenType() {
        return tokenType;
    }
}
