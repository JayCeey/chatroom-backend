package com.jayce.biz.service.impl;

import cn.hutool.core.util.StrUtil;
import com.jayce.api.biz.vo.BizMessagePicVO;
import com.jayce.api.biz.vo.BizUserAvatarVO;
import com.jayce.biz.bo.FileBO;
import com.jayce.biz.config.MinioTemplate;
import com.jayce.biz.config.OssConfig;
import com.jayce.biz.service.BizService;
import com.jayce.common.response.ResponseEnum;
import com.jayce.common.response.ServerResponseEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

@Service
@Slf4j
public class BizServiceImpl implements BizService {

    public static final String NORM_DAY_PATTERN = "yyyy/MM/dd";

    @Autowired
    private MinioTemplate minioTemplate;

    @Override
    public ServerResponseEntity<Void> uploadPic(FileBO fileBO) {
        try{
            minioTemplate.uploadMinio(fileBO.getData(), fileBO.getPath(), fileBO.getContentType());
            return ServerResponseEntity.success();
        }catch (IOException e) {
            log.error("上传文件失败: ", e);
            return ServerResponseEntity.showFailMsg(e.toString());
        }
    }

    @Override
    public ServerResponseEntity<Void> deleteFile(String filePath){
        // 获取文件的实际路径--数据库中保存的文件路径为： / + 实际的文件路径
        if (StrUtil.isNotBlank(filePath)) {
            filePath = filePath.substring(1);
        }
        try {
            minioTemplate.removeObject(filePath);
            return ServerResponseEntity.success();
        } catch (Exception e) {
            e.printStackTrace();
            return ServerResponseEntity.showFailMsg("删除文件失败");
        }
    }

    @Override
    public ServerResponseEntity<BizUserAvatarVO> getUserAvatar(String path) {
        try {
            ServerResponseEntity<byte[]> serverResponseEntity = minioTemplate.getObjectResponse(path);
            if(!serverResponseEntity.isSuccess()) return ServerResponseEntity.showFailMsg(serverResponseEntity.getMsg());
            byte[] data = serverResponseEntity.getData();
            BizUserAvatarVO bizUserAvatarVO = new BizUserAvatarVO();
            bizUserAvatarVO.setData(data);
            return ServerResponseEntity.success(bizUserAvatarVO);
        } catch (Exception e) {
            log.error("获取文件失败: ", e);
            return ServerResponseEntity.fail(ResponseEnum.EXCEPTION);
        }
    }

    @Override
    public ServerResponseEntity<BizMessagePicVO> getMessagePic(String path) {
        try {
            ServerResponseEntity<byte[]> serverResponseEntity = minioTemplate.getObjectResponse(path);
            if(!serverResponseEntity.isSuccess()) return ServerResponseEntity.showFailMsg(serverResponseEntity.getMsg());
            byte[] data = serverResponseEntity.getData();
            BizMessagePicVO bizMessagePicVO = new BizMessagePicVO();
            bizMessagePicVO.setData(data);
            return ServerResponseEntity.success(bizMessagePicVO);
        } catch (Exception e) {
            log.error("获取文件失败: ", e);
            return ServerResponseEntity.fail(ResponseEnum.EXCEPTION);
        }
    }
}
