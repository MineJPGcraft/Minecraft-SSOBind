package com.minecraft.ssoplugin.oauth;

import org.json.JSONObject;

/**
 * OAuth提供者接口，定义OAuth认证流程的方法
 */
public interface OAuthProvider {
    
    /**
     * 生成授权URL
     * @param state 状态参数
     * @return 授权URL
     */
    String generateAuthUrl(String state);
    
    /**
     * 获取访问令牌
     * @param code 授权码
     * @return 令牌响应
     */
    OAuthTokenResponse getAccessToken(String code);
    
    /**
     * 获取用户信息
     * @param accessToken 访问令牌
     * @return 用户信息JSON对象
     */
    JSONObject getUserInfo(String accessToken);
    
    /**
     * 刷新访问令牌
     * @param refreshToken 刷新令牌
     * @return 新的令牌响应
     */
    OAuthTokenResponse refreshAccessToken(String refreshToken);
}
