package com.jayce.biz.bo;

import lombok.Data;

@Data
public class FileBO {
    private byte[] data;

    private String path;

    private String contentType;
}
