package com.jayce.auth.controller;

import com.jayce.api.auth.bo.UserInfoInTokenBO;
import com.jayce.api.auth.vo.TokenInfoVO;
import com.jayce.auth.dto.AuthenticationDTO;
import com.jayce.auth.manager.TokenStore;
import com.jayce.auth.service.AuthAccountService;
import com.jayce.common.response.ServerResponseEntity;
import com.jayce.common.security.AuthUserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "登录")
@Slf4j
public class LoginController {

    @Autowired
    private AuthAccountService authAccountService;

    @Autowired
    private TokenStore tokenStore;

    @GetMapping("/test")
    public ServerResponseEntity<Void> test(){
        return ServerResponseEntity.success();
    }

    @PostMapping("/ua/login")
    @Operation(summary = "账号密码" , description = "通过账号登录，还要携带用户的类型，也就是用户所在的系统")
    public ServerResponseEntity<TokenInfoVO> login(@Valid @RequestBody AuthenticationDTO authenticationDTO,
                                               HttpServletResponse response) {

        ServerResponseEntity<UserInfoInTokenBO> userInfoInTokenResponse = authAccountService
                .getUserInfoInTokenByInputUserNameAndPassword(authenticationDTO.getPrincipal(),
                        authenticationDTO.getCredentials(), authenticationDTO.getSysType());

        if (!userInfoInTokenResponse.isSuccess()) {
            return ServerResponseEntity.transform(userInfoInTokenResponse);
        }

        UserInfoInTokenBO data = userInfoInTokenResponse.getData();

        TokenInfoVO tokenInfoVO = tokenStore.storeAndGetVo(data);
        log.info("登录成功，设置cookie: {}", tokenInfoVO);
        Cookie cookie = new Cookie("refreshToken", tokenInfoVO.getRefreshToken());
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setAttribute("SameSite", "strict");
        response.addCookie(cookie);

        // 保存token，返回token数据给前端，这里是最重要的
        return ServerResponseEntity.success(tokenInfoVO);
    }

    @PostMapping("/logout")
    @Operation(summary = "退出登陆" , description = "点击退出登陆，清除token，清除菜单缓存")
    public ServerResponseEntity<TokenInfoVO> logout() {
        UserInfoInTokenBO userInfoInToken = AuthUserContext.get();
        // 删除该用户在该系统的token
        tokenStore.deleteAllToken(userInfoInToken.getSysType().toString(), userInfoInToken.getUserId());
        return ServerResponseEntity.success();
    }
}
