package com.jayce.biz.constant;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import com.jayce.biz.config.OssConfig;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;

public class FilePath {

    public static final String AVATAR_PATH = "/user/avatar/";

    public static final String MESSAGE_PATH = "/message/";

    public static final String NORM_DAY_PATTERN = "yyyy/MM/dd";

    public static String getMessagePath(Long chatId, Long messageId, String contentType){
        contentType = "png";
        return MESSAGE_PATH + DateUtil.format(new Date(), NORM_DAY_PATTERN)+ "/" + chatId.toString() + "/" + messageId.toString() + "." + contentType;
    }

    public static String getAvatarPath(Integer userId, String contentType){
        return AVATAR_PATH + userId + "/" + IdUtil.simpleUUID() + ".png";
    }
}
