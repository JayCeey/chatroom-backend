package com.jayce.biz.feign;

import com.jayce.api.biz.dto.BizMessagePicDTO;
import com.jayce.api.biz.dto.BizUserAvatarDTO;
import com.jayce.api.biz.feign.BizFeignClient;
import com.jayce.api.biz.vo.BizMessagePicVO;
import com.jayce.api.biz.vo.BizUserAvatarVO;
import com.jayce.biz.bo.FileBO;
import com.jayce.biz.service.BizService;
import com.jayce.common.response.ServerResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.jayce.biz.constant.FilePath.getAvatarPath;
import static com.jayce.biz.constant.FilePath.getMessagePath;

@RestController
public class BizFeignController implements BizFeignClient {
    @Autowired
    private BizService bizService;

    @Override
    public ServerResponseEntity<BizUserAvatarVO> uploadUserAvatar(BizUserAvatarDTO bizUserAvatarDTO) {
        String path = getAvatarPath(bizUserAvatarDTO.getUserId(), bizUserAvatarDTO.getContentType());
        FileBO fileBO = new FileBO();
        fileBO.setData(bizUserAvatarDTO.getData());
        fileBO.setPath(path);
        fileBO.setContentType("image/png"); // 先只接受png格式
        bizService.uploadPic(fileBO);
        BizUserAvatarVO bizUserAvatarVO = new BizUserAvatarVO();
        bizUserAvatarVO.setPath(path);
        return ServerResponseEntity.success(bizUserAvatarVO);
    }

    @Override
    public ServerResponseEntity<BizMessagePicVO> uploadMessagePic(BizMessagePicDTO bizMessagePicDTO) {
        String path = getMessagePath(bizMessagePicDTO.getChatId(), bizMessagePicDTO.getMsgId(), bizMessagePicDTO.getContentType());
        FileBO fileBO = new FileBO();
        fileBO.setData(bizMessagePicDTO.getData());
        fileBO.setPath(path);
        fileBO.setContentType("image/png");
        bizService.uploadPic(fileBO);
        BizMessagePicVO bizMessagePicVO = new BizMessagePicVO();
        bizMessagePicVO.setPath(path);
        return ServerResponseEntity.success(bizMessagePicVO);
    }

    @Override
    public ServerResponseEntity<BizUserAvatarVO> getUserAvatar(BizUserAvatarDTO bizUserAvatarDTO) {
        return bizService.getUserAvatar(bizUserAvatarDTO.getPath());
    }

    @Override
    public ServerResponseEntity<BizMessagePicVO> getMessagePic(BizMessagePicDTO bizMessagePicDTO) {
        return bizService.getMessagePic(bizMessagePicDTO.getPath());
    }
}
