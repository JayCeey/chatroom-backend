package com.jayce.auth.model;

import com.jayce.common.model.BaseModel;
import lombok.Data;

@Data
public class AuthAccount extends BaseModel {

    /**
     * 用户id
     */
    private Integer userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 是否是管理员
     */
    private Integer sysType;
}
