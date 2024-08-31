package com.jayce.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RefreshTokenDTO {
    /**
     * refreshToken
     */
    @NotBlank(message = "refreshToken不能为空")
    @Schema(description = "refreshToken" , requiredMode = Schema.RequiredMode.REQUIRED)
    private String refreshToken;
}
