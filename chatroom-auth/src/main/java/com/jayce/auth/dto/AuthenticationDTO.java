package com.jayce.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AuthenticationDTO {
    @NotBlank(message = "principal不能为空")
    @Schema(description = "用户名" , requiredMode = Schema.RequiredMode.REQUIRED)
    protected String principal;

    /**
     * 密码
     */
    @NotBlank(message = "credentials不能为空")
    @Schema(description = "一般用作密码" , requiredMode = Schema.RequiredMode.REQUIRED)
    protected String credentials;

    @NotNull(message = "sysType不能为空")
    @Schema(description = "系统类型 0.普通用户系统 1.管理员" , requiredMode = Schema.RequiredMode.REQUIRED)
    protected Integer sysType;
}
