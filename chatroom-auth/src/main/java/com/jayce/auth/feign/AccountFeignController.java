package com.jayce.auth.feign;

import cn.hutool.core.util.StrUtil;
import com.jayce.api.auth.bo.UserInfoInTokenBO;
import com.jayce.api.auth.constant.SysTypeEnum;
import com.jayce.api.auth.dto.AuthAccountDTO;
import com.jayce.api.auth.feign.AccountFeignClient;
import com.jayce.api.auth.vo.AuthAccountVO;
import com.jayce.api.auth.vo.TokenInfoVO;
import com.jayce.auth.manager.TokenStore;
import com.jayce.auth.mapper.AuthAccountMapper;
import com.jayce.auth.model.AuthAccount;
import com.jayce.common.exception.ChatroomException;
import com.jayce.common.response.ResponseEnum;
import com.jayce.common.response.ServerResponseEntity;
import com.jayce.common.security.AuthUserContext;
import com.jayce.common.security.bo.AuthAccountInVerifyBO;
import com.jayce.common.security.config.InputUserNameEnum;
import com.jayce.common.util.BeanUtil;
import com.jayce.common.util.PrincipalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
public class AccountFeignController implements AccountFeignClient {
    @Autowired
    private AuthAccountMapper authAccountMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TokenStore tokenStore;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ServerResponseEntity<Integer> save(AuthAccountDTO authAccountDTO) {

        ServerResponseEntity<AuthAccount> verify = verify(authAccountDTO);
        if (!verify.isSuccess()) {
            return ServerResponseEntity.transform(verify);
        }
        AuthAccount data = verify.getData();
        authAccountMapper.save(data);

        return ServerResponseEntity.success(data.getUserId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ServerResponseEntity<Void> update(AuthAccountDTO authAccountDTO) {
        ServerResponseEntity<AuthAccount> verify = verify(authAccountDTO);
        if (!verify.isSuccess()) {
            return ServerResponseEntity.transform(verify);
        }
        authAccountMapper.updateAccountInfo(verify.getData());
        return ServerResponseEntity.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ServerResponseEntity<Void> updateAuthAccountStatus(AuthAccountDTO authAccountDTO) {
        AuthAccount authAccount = BeanUtil.map(authAccountDTO, AuthAccount.class);
        authAccountMapper.updateAccountInfo(authAccount);
        return ServerResponseEntity.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ServerResponseEntity<Void> deleteByUserIdAndSysType(Integer userId) {
        UserInfoInTokenBO userInfoInTokenBO = AuthUserContext.get();
        authAccountMapper.deleteByUserIdAndSysType(userId, userInfoInTokenBO.getSysType());
        return ServerResponseEntity.success();
    }

    @Override
    public ServerResponseEntity<AuthAccountVO> getByUserIdAndSysType(Integer userId,Integer sysType) {
        UserInfoInTokenBO userInfoInTokenBO = AuthUserContext.get();
        AuthAccount authAccount = authAccountMapper.getByUserIdAndType(userId, userInfoInTokenBO.getSysType());
        return ServerResponseEntity.success(BeanUtil.map(authAccount, AuthAccountVO.class));
    }

    @Override
    public ServerResponseEntity<TokenInfoVO> storeTokenAndGetVo(UserInfoInTokenBO userInfoInTokenBO) {
        return ServerResponseEntity.success(tokenStore.storeAndGetVo(userInfoInTokenBO));
    }

    @Override
    public ServerResponseEntity<AuthAccountVO> getByUsernameAndSysType(String username, SysTypeEnum sysType) {
        return ServerResponseEntity.success(authAccountMapper.getByUsernameAndSysType(username, sysType.value()));
    }

    private ServerResponseEntity<AuthAccount> verify(AuthAccountDTO authAccountDTO) {

        // 用户名
        if (!PrincipalUtil.isUserName(authAccountDTO.getUsername())) {
            return ServerResponseEntity.showFailMsg("用户名格式不正确");
        }

        AuthAccountInVerifyBO userNameBo = authAccountMapper.getAuthAccountInVerifyByInputUserName(InputUserNameEnum.USERNAME.value(), authAccountDTO.getUsername(), authAccountDTO.getSysType());
        if (userNameBo != null && !Objects.equals(userNameBo.getUserId(), authAccountDTO.getUserId())) {
            return ServerResponseEntity.showFailMsg("用户名已存在，请更换用户名再次尝试");
        }

        AuthAccount authAccount = BeanUtil.map(authAccountDTO, AuthAccount.class);

        if (StrUtil.isNotBlank(authAccount.getPassword())) {
            authAccount.setPassword(passwordEncoder.encode(authAccount.getPassword()));
        }

        return ServerResponseEntity.success(authAccount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ServerResponseEntity<Void> updateUserInfoByUserIdAndSysType(UserInfoInTokenBO userInfoInTokenBO, Integer userId, Integer sysType) {
        AuthAccount byUserIdAndType = authAccountMapper.getByUserIdAndType(userId, sysType);
        userInfoInTokenBO.setUserId(byUserIdAndType.getUserId());
        tokenStore.updateUserInfoByUidAndAppId(byUserIdAndType.getUserId(), sysType.toString(), userInfoInTokenBO);
        AuthAccount authAccount = BeanUtil.map(userInfoInTokenBO, AuthAccount.class);
        int res = authAccountMapper.updateUserInfoByUserId(authAccount, userId, sysType);
        if (res != 1) {
            throw new ChatroomException("用户信息错误，更新失败");
        }
        return ServerResponseEntity.success();
    }

    @Override
    public ServerResponseEntity<AuthAccountVO> getMerchantInfoByTenantId(Long tenantId) {
        AuthAccountVO authAccountVO = authAccountMapper.getMerchantInfoByTenantId(tenantId);
        return ServerResponseEntity.success(authAccountVO);
    }

    @Override
    public ServerResponseEntity<Void> updateShopPassword(AuthAccountDTO authAccountDTO) {
        if (StrUtil.isNotBlank(authAccountDTO.getPassword())) {
            authAccountDTO.setPassword(passwordEncoder.encode(authAccountDTO.getPassword()));
        }
        authAccountMapper.updatePassword(authAccountDTO.getUserId(), authAccountDTO.getSysType(), authAccountDTO.getPassword());
        return ServerResponseEntity.success();
    }
}
