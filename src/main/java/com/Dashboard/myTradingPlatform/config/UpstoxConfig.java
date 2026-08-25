package com.Dashboard.myTradingPlatform.config;

import com.upstox.ApiClient;
import com.upstox.auth.OAuth;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UpstoxConfig {

    @Bean
    public ApiClient upstoxApiClient(
            @Value("${UPSTOX_ACCESS_TOKEN}") String accessToken) {

        ApiClient apiClient = new ApiClient();

        OAuth oauth = (OAuth) apiClient.getAuthentication("OAUTH2");
        oauth.setAccessToken(accessToken);

        return apiClient;
    }
}