package com.jayce.auth.constant;

public enum AuthAccountStatusEnum {

    /**
     * 启用
     */
    ENABLE(1),

    /**
     * 禁用
     */
    DISABLE(0),

    /**
     * 删除
     */
    DELETE(-1)
    ;

    private final Integer value;

    public Integer value() {
        return value;
    }

    AuthAccountStatusEnum(Integer value) {
        this.value = value;
    }

}
