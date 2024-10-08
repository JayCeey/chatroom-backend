package com.jayce.auth.feign;

import com.jayce.api.auth.bo.UserInfoInTokenBO;
import com.jayce.api.auth.feign.TokenFeignClient;
import com.jayce.auth.manager.TokenStore;
import com.jayce.common.response.ServerResponseEntity;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TokenFeignController implements TokenFeignClient {
    private static final Logger logger = LoggerFactory.getLogger(TokenFeignController.class);

    @Autowired
    private TokenStore tokenStore;

    @Override
    public ServerResponseEntity<UserInfoInTokenBO> checkToken(String accessToken) {
        ServerResponseEntity<UserInfoInTokenBO> userInfoByAccessTokenResponse = tokenStore
                .getUserInfoByAccessToken(accessToken, true);
        if (!userInfoByAccessTokenResponse.isSuccess()) {
            return ServerResponseEntity.transform(userInfoByAccessTokenResponse);
        }
        return ServerResponseEntity.success(userInfoByAccessTokenResponse.getData());
    }
}
