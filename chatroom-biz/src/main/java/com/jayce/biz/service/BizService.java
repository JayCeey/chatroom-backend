package com.jayce.biz.service;

import com.jayce.api.biz.vo.BizMessagePicVO;
import com.jayce.api.biz.vo.BizUserAvatarVO;
import com.jayce.biz.bo.FileBO;
import com.jayce.common.response.ServerResponseEntity;

public interface BizService {
    ServerResponseEntity<Void> uploadPic(FileBO fileBO);

    ServerResponseEntity<Void> deleteFile(String filePath);

    ServerResponseEntity<BizUserAvatarVO> getUserAvatar(String path);

    ServerResponseEntity<BizMessagePicVO> getMessagePic(String path);
}
