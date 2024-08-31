package com.jayce.auth.service;

import com.jayce.api.auth.bo.UserInfoInTokenBO;
import com.jayce.auth.model.AuthAccount;
import com.jayce.common.response.ServerResponseEntity;

public interface AuthAccountService {
    /**
     * 通过输入的用户名密码，校验并获取部分用户信息
     * @param inputUserName 输入的用户名（用户名）
     * @param password 密码
     * @return 用户在token中信息
     */
    ServerResponseEntity<UserInfoInTokenBO> getUserInfoInTokenByInputUserNameAndPassword(String inputUserName,
                                                                                         String password, Integer sysType);

    /**
     * 根据用户id 和系统类型获取平台唯一用户
     * @param userId 用户id
     * @param sysType 系统类型
     * @return 平台唯一用户
     */
    AuthAccount getByUserIdAndType(Integer userId, Integer sysType);

    /**
     * 更新密码 根据用户id 和系统类型
     * @param userId 用户id
     * @param sysType 系统类型
     * @param newPassWord 新密码
     */
    void updatePassword(Integer userId, Integer sysType, String newPassWord);

    /**
     * 根据getByUid获取平台唯一用户
     *
     * @param userId  userId
     * @return 平台唯一用户
     */
    AuthAccount getByUid(Integer userId);

    /**
     * 根据用户名获取用户信息
     * @param username 用户名
     * @param systemType 系统类型
     * @return uid
     */
    AuthAccount getAccountByInputUserName(String username, Integer systemType);
}
