package com.jayce.auth.controller;

import com.jayce.api.auth.vo.TokenInfoVO;
import com.jayce.auth.dto.RefreshTokenDTO;
import com.jayce.auth.manager.TokenStore;
import com.jayce.common.response.ServerResponseEntity;
import com.jayce.common.security.bo.TokenInfoBO;
import com.jayce.common.util.BeanUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@Tag(name = "token")
public class TokenController {

    @Autowired
    private TokenStore tokenStore;

    // 如果accessToken过期，前端再发送请求到该接口，然后从cookie中拿出refreshToken并且返回新的accessToken
    @PostMapping("/ua/token/refresh")
    public ServerResponseEntity<TokenInfoVO> refreshToken(@CookieValue(value = "refreshToken") String refreshToken, HttpServletResponse response) {
        log.info("获取cookie：{}", refreshToken);
        ServerResponseEntity<TokenInfoBO> tokenInfoServerResponseEntity = tokenStore
                .refreshToken(refreshToken);
        if (!tokenInfoServerResponseEntity.isSuccess()) {
            return ServerResponseEntity.transform(tokenInfoServerResponseEntity);
        }

        TokenInfoBO tokenInfoBO = tokenInfoServerResponseEntity.getData();

        Cookie cookie = new Cookie("refreshToken", tokenInfoBO.getRefreshToken());

        cookie.setHttpOnly(true);

        cookie.setSecure(true);

        cookie.setAttribute("SameSite", "strict");

        response.addCookie(cookie);
        return ServerResponseEntity
                .success(BeanUtil.map(tokenInfoBO, TokenInfoVO.class));
    }

}
