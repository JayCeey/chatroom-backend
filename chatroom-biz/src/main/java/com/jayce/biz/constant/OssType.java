package com.jayce.biz.constant;

public enum OssType {

    /**
     * 阿里云oss
     */
    ALI(0),

    /**
     * minio
     */
    MINIO(1),
    ;

    private final Integer value;

    public Integer value() {
        return value;
    }

    OssType(Integer value) {
        this.value = value;
    }

}
