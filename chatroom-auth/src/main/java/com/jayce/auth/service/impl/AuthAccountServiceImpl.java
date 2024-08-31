package com.jayce.auth.service.impl;

import cn.hutool.core.util.StrUtil;
import com.jayce.api.auth.bo.UserInfoInTokenBO;
import com.jayce.auth.constant.AuthAccountStatusEnum;
import com.jayce.auth.mapper.AuthAccountMapper;
import com.jayce.auth.model.AuthAccount;
import com.jayce.auth.service.AuthAccountService;
import com.jayce.common.response.ServerResponseEntity;
import com.jayce.common.security.bo.AuthAccountInVerifyBO;
import com.jayce.common.security.config.InputUserNameEnum;
import com.jayce.common.util.BeanUtil;
import com.jayce.common.util.PrincipalUtil;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class AuthAccountServiceImpl implements AuthAccountService {
    @Resource
    private AuthAccountMapper authAccountMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public static final String USER_NOT_FOUND_SECRET = "USER_NOT_FOUND_SECRET";

    private static String userNotFoundEncodedPassword;

    @Override
    public ServerResponseEntity<UserInfoInTokenBO> getUserInfoInTokenByInputUserNameAndPassword(String inputUserName,
                                                                                                String password, Integer sysType) {

        if (StrUtil.isBlank(inputUserName)) {
            return ServerResponseEntity.showFailMsg("用户名不能为空");
        }
        if (StrUtil.isBlank(password)) {
            return ServerResponseEntity.showFailMsg("密码不能为空");
        }

        InputUserNameEnum inputUserNameEnum = null;

        // 用户名
        if (PrincipalUtil.isUserName(inputUserName)) {
            inputUserNameEnum = InputUserNameEnum.USERNAME;
        }

        if (inputUserNameEnum == null) {
            return ServerResponseEntity.showFailMsg("请输入正确的用户名");
        }

        AuthAccountInVerifyBO authAccountInVerifyBO = authAccountMapper
                .getAuthAccountInVerifyByInputUserName(inputUserNameEnum.value(), inputUserName, sysType);

        if (authAccountInVerifyBO == null) {
            prepareTimingAttackProtection();
            // 再次进行运算，防止计时攻击
            // 计时攻击（Timing
            // attack），通过设备运算的用时来推断出所使用的运算操作，或者通过对比运算的时间推定数据位于哪个存储设备，或者利用通信的时间差进行数据窃取。
            mitigateAgainstTimingAttack(password);
            return ServerResponseEntity.showFailMsg("用户名或密码不正确");
        }

//        if (Objects.equals(authAccountInVerifyBO.getStatus(), AuthAccountStatusEnum.DISABLE.value())) {
//            return ServerResponseEntity.showFailMsg("用户已禁用，请联系客服");
//        }

        if (!Objects.equals(password, authAccountInVerifyBO.getPassword())) {
            return ServerResponseEntity.showFailMsg("用户名或密码不正确");
        }

        return ServerResponseEntity.success(BeanUtil.map(authAccountInVerifyBO, UserInfoInTokenBO.class));
    }

    @Override
    public AuthAccount getByUserIdAndType(Integer userId, Integer sysType) {
        return authAccountMapper.getByUserIdAndType(userId, sysType);
    }

    @Override
    public void updatePassword(Integer userId, Integer sysType, String newPassWord) {
        authAccountMapper.updatePassword(userId, sysType, passwordEncoder.encode(newPassWord));
    }

    @Override
    public AuthAccount getByUid(Integer userId) {
        return authAccountMapper.getByUid(userId);
    }

    @Override
    public AuthAccount getAccountByInputUserName(String mobile, Integer systemType) {
        return authAccountMapper.getAccountByInputUserName(mobile,systemType);
    }

    /**
     * 防止计时攻击
     */
    private void prepareTimingAttackProtection() {
        if (userNotFoundEncodedPassword == null) {
            userNotFoundEncodedPassword = this.passwordEncoder.encode(USER_NOT_FOUND_SECRET);
        }
    }

    /**
     * 防止计时攻击
     */
    private void mitigateAgainstTimingAttack(String presentedPassword) {
        if (presentedPassword != null) {
            this.passwordEncoder.matches(presentedPassword, userNotFoundEncodedPassword);
        }
    }
}
