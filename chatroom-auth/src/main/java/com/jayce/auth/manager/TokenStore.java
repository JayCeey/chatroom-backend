package com.jayce.auth.manager;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.jayce.api.auth.bo.UserInfoInTokenBO;
import com.jayce.api.auth.constant.SysTypeEnum;
import com.jayce.api.auth.vo.TokenInfoVO;
import com.jayce.common.cache.constant.CacheNames;
import com.jayce.common.exception.ChatroomException;
import com.jayce.common.response.ResponseEnum;
import com.jayce.common.response.ServerResponseEntity;
import com.jayce.common.security.bo.TokenInfoBO;
import com.jayce.common.util.BeanUtil;
import com.jayce.common.util.PrincipalUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Component
@RefreshScope
@Slf4j
public class TokenStore {
    private static final Logger logger = LoggerFactory.getLogger(TokenStore.class);

    private final RedisTemplate<Object, Object> redisTemplate;

    private final RedisSerializer<Object> redisSerializer;

    private final StringRedisTemplate stringRedisTemplate;


    private static final String SECRET_KEY = "this-is-a-big-big-secret-for-my-application";    // 签名时使用的secret

    public TokenStore(RedisTemplate<Object, Object> redisTemplate, RedisSerializer<Object> redisSerializer,
                      StringRedisTemplate stringRedisTemplate) {
        this.redisTemplate = redisTemplate;
        this.redisSerializer = redisSerializer;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 将用户的部分信息存储在token中，并返回token信息
     * @param userInfoInToken 用户在token中的信息
     * @return token信息
     */
    public TokenInfoBO storeAccessToken(UserInfoInTokenBO userInfoInToken) {
        TokenInfoBO tokenInfoBO = new TokenInfoBO();
        // String accessToken = IdUtil.simpleUUID();
        Map<String, Object> claims = BeanUtil.map(userInfoInToken, Map.class);
        String accessToken = getJwtToken(claims); // 这里改成JWT
        String refreshToken = IdUtil.simpleUUID();

        tokenInfoBO.setUserInfoInToken(userInfoInToken);
        tokenInfoBO.setExpiresIn(getExpiresIn(userInfoInToken.getSysType()));

        String uidToAccessKeyStr = getUidToAccessKey(getApprovalKey(userInfoInToken));
        String accessKeyStr = getAccessKey(accessToken);
        String refreshToAccessKeyStr = getRefreshToAccessKey(refreshToken);

        // 一个用户会登陆很多次，每次登陆的token都会存在 uid_to_access里面
        // 但是每次保存都会更新这个key的时间，而key里面的token有可能会过期，过期就要移除掉
        List<String> existsAccessTokens = new ArrayList<>();
        // 新的token数据
        existsAccessTokens.add(accessToken + StrUtil.COLON + refreshToken);

        Long size = redisTemplate.opsForSet().size(uidToAccessKeyStr);
        if (size != null && size != 0) {
            List<String> tokenInfoBoList = stringRedisTemplate.opsForSet().pop(uidToAccessKeyStr, size);
            if (tokenInfoBoList != null) {
                for (String accessTokenWithRefreshToken : tokenInfoBoList) {
                    String[] accessTokenWithRefreshTokenArr = accessTokenWithRefreshToken.split(StrUtil.COLON);
                    String accessTokenData = accessTokenWithRefreshTokenArr[0];
                    if (BooleanUtil.isTrue(stringRedisTemplate.hasKey(getAccessKey(accessTokenData)))) {
                        existsAccessTokens.add(accessTokenWithRefreshToken);
                    }
                }
            }
        }

        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {

            long expiresIn = tokenInfoBO.getExpiresIn();

            byte[] uidKey = uidToAccessKeyStr.getBytes(StandardCharsets.UTF_8);
            byte[] refreshKey = refreshToAccessKeyStr.getBytes(StandardCharsets.UTF_8);
            byte[] accessKey = accessKeyStr.getBytes(StandardCharsets.UTF_8);

            for (String existsAccessToken : existsAccessTokens) {
                connection.sAdd(uidKey, existsAccessToken.getBytes(StandardCharsets.UTF_8)); //set中的元素不会过期，因此需要手动去除set中过期的元素。在这个set中的accessKey都是合法的。主要是表明该用户当前有多少个accessKey存在
            }

            // 通过uid + sysType 保存access_token，当需要禁用用户的时候，可以根据uid + sysType 禁用用户
            connection.expire(uidKey, expiresIn);

            // 通过refresh_token获取用户的access_token从而刷新token，默认过期时间是10分钟
            connection.setEx(refreshKey, 600, accessToken.getBytes(StandardCharsets.UTF_8));

            // 通过access_token保存用户的租户id，用户id，uid
            connection.setEx(accessKey, expiresIn, Objects.requireNonNull(redisSerializer.serialize(userInfoInToken)));

            return null;
        });

        // 返回给前端是加密的token
//        tokenInfoBO.setAccessToken(encryptToken(accessToken,userInfoInToken.getSysType()));
        tokenInfoBO.setAccessToken(accessToken);
        tokenInfoBO.setRefreshToken(encryptToken(refreshToken,userInfoInToken.getSysType()));


        log.info("===============================================解码JWTTOKEN");
        log.info(decryptJwtToken(accessToken).toString());

        return tokenInfoBO;
    }

    private static Key generateKey(){
        String encode = Base64.encode(SECRET_KEY);
        byte[] keyBytes = Decoders.BASE64.decode(encode);
        Key key = Keys.hmacShaKeyFor(keyBytes);
        return key;
    }

    private static String getJwtToken(Map<String, Object> claims){
        Date now = new Date();
        String token = Jwts.builder()               // 创建jwt builder
                .setClaims(claims)
                .setExpiration(new Date(now.getTime() + 604800000L))
                .signWith(generateKey())
                .compact();
        return token;
    }

    private static ServerResponseEntity<UserInfoInTokenBO> decryptJwtToken(String jwtToken){
        Claims claims = null;
        try {
            claims = Jwts.parserBuilder().setSigningKey(generateKey()).build().parseClaimsJws(jwtToken).getBody();
        } catch (Exception e) {
            log.error(e.getMessage());
            log.info("JWT格式验证失败:{}", jwtToken);
        }
        return claims != null? ServerResponseEntity.success(BeanUtil.map(claims, UserInfoInTokenBO.class)):
                ServerResponseEntity.fail(ResponseEnum.DATA_ERROR);
    }

    private int getExpiresIn(int sysType) {
        // 3600秒
        int expiresIn = 3600;

        // 普通用户token过期时间 10秒
        if (Objects.equals(sysType, SysTypeEnum.ORDINARY.value())) {
//            expiresIn = expiresIn * 24 * 30;
            expiresIn = 30;
        }
        // 系统管理员的token过期时间 5分钟
        if (Objects.equals(sysType, SysTypeEnum.ADMIN.value())) {
//            expiresIn = expiresIn * 24 * 30;
            expiresIn = 30;
        }
        return expiresIn;
    }

    /**
     * 根据accessToken 获取用户信息
     * @param accessToken accessToken
     * @param needDecrypt 是否需要解密
     * @return 用户信息
     */
    public ServerResponseEntity<UserInfoInTokenBO> getUserInfoByAccessToken(String accessToken, boolean needDecrypt) {
        if (StrUtil.isBlank(accessToken)) {
            return ServerResponseEntity.showFailMsg("accessToken is blank");
        }
//        String realAccessToken;
//        if (needDecrypt) {
//            ServerResponseEntity<String> decryptTokenEntity = decryptToken(accessToken);
//            if (!decryptTokenEntity.isSuccess()) {
//                return ServerResponseEntity.transform(decryptTokenEntity);
//            }
//            realAccessToken = decryptTokenEntity.getData();
//        }
//        else {
//            realAccessToken = accessToken;
//        }
//
//        UserInfoInTokenBO userInfoInTokenBO = (UserInfoInTokenBO) redisTemplate.opsForValue()
//                .get(getAccessKey(realAccessToken));

        UserInfoInTokenBO userInfoInTokenBO = (UserInfoInTokenBO) redisTemplate.opsForValue()
                .get(getAccessKey(accessToken));

        if (userInfoInTokenBO == null) {
            return ServerResponseEntity.showFailMsg("accessToken 已过期");
        }
        return ServerResponseEntity.success(userInfoInTokenBO);
    }

    /**
     * 刷新token，并返回新的token
     * @param refreshToken
     * @return
     */
    public ServerResponseEntity<TokenInfoBO> refreshToken(String refreshToken) {
        if (StrUtil.isBlank(refreshToken)) {
            return ServerResponseEntity.showFailMsg("refreshToken is blank");
        }
        ServerResponseEntity<String> decryptTokenEntity = decryptToken(refreshToken);
        if (!decryptTokenEntity.isSuccess()) {
            return ServerResponseEntity.transform(decryptTokenEntity);
        }
        String realRefreshToken = decryptTokenEntity.getData();
        log.info("这是解密后的refresh token: {}", realRefreshToken);
        String accessToken = stringRedisTemplate.opsForValue().get(getRefreshToAccessKey(realRefreshToken));

        if (StrUtil.isBlank(accessToken)) {
            return ServerResponseEntity.showFailMsg("refreshToken 已过期");
        }

        ServerResponseEntity<UserInfoInTokenBO> userInfoByAccessTokenEntity = decryptJwtToken(accessToken);

        if(!userInfoByAccessTokenEntity.isSuccess()){
            return ServerResponseEntity.showFailMsg(userInfoByAccessTokenEntity.getMsg());
        }

//        ServerResponseEntity<UserInfoInTokenBO> userInfoByAccessTokenEntity = getUserInfoByAccessToken(accessToken,
//                false);
//
//        if (!userInfoByAccessTokenEntity.isSuccess()) {
//            return ServerResponseEntity.showFailMsg("refreshToken 已过期");
//        }

        UserInfoInTokenBO userInfoInTokenBO = userInfoByAccessTokenEntity.getData();

        // 删除旧的refresh_token
        stringRedisTemplate.delete(getRefreshToAccessKey(realRefreshToken));
        // 删除旧的access_token
        stringRedisTemplate.delete(getAccessKey(accessToken));
        // 保存一份新的token
        TokenInfoBO tokenInfoBO = storeAccessToken(userInfoInTokenBO);

        return ServerResponseEntity.success(tokenInfoBO);
    }

    /**
     * 删除全部的token
     */
    public void deleteAllToken(String appId, Integer userId) {
        String uidKey = getUidToAccessKey(getApprovalKey(appId, userId));
        Long size = redisTemplate.opsForSet().size(uidKey);
        if (size == null || size == 0) {
            return;
        }
        List<String> tokenInfoBoList = stringRedisTemplate.opsForSet().pop(uidKey, size);

        if (CollUtil.isEmpty(tokenInfoBoList)) {
            return;
        }

        for (String accessTokenWithRefreshToken : tokenInfoBoList) {
            String[] accessTokenWithRefreshTokenArr = accessTokenWithRefreshToken.split(StrUtil.COLON);
            String accessToken = accessTokenWithRefreshTokenArr[0];
            String refreshToken = accessTokenWithRefreshTokenArr[1];
            redisTemplate.delete(getRefreshToAccessKey(refreshToken));
            redisTemplate.delete(getAccessKey(accessToken));
        }
        redisTemplate.delete(uidKey);

    }

    private static String getApprovalKey(UserInfoInTokenBO userInfoInToken) {
        return getApprovalKey(userInfoInToken.getSysType().toString(), userInfoInToken.getUserId());
    }

    private static String getApprovalKey(String appId, Integer userId) {
        return userId == null?  appId : appId + StrUtil.COLON + userId;
    }

    private String encryptToken(String token,Integer sysType) {
        return Base64.encode(token + System.currentTimeMillis() + sysType);
    }

    private ServerResponseEntity<String> decryptToken(String data) {
        String decryptStr;
        String decryptToken;
        try {
            decryptStr = Base64.decodeStr(data);
            decryptToken = decryptStr.substring(0,32);
            // 创建token的时间，token使用时效性，防止攻击者通过一堆的尝试找到aes的密码，虽然aes是目前几乎最好的加密算法
            long createTokenTime = Long.parseLong(decryptStr.substring(32,45));
            // 系统类型
            int sysType = Integer.parseInt(decryptStr.substring(45));
            // token的过期时间
            int expiresIn = getExpiresIn(sysType);
            long second = 1000L;
            if (System.currentTimeMillis() - createTokenTime > expiresIn * second) {
                return ServerResponseEntity.showFailMsg("refresh token已过期");
            }
        }
        catch (Exception e) {
            logger.error(e.getMessage());
            return ServerResponseEntity.showFailMsg("token 格式有误");
        }

        // 防止解密后的token是脚本，从而对redis进行攻击，uuid只能是数字和小写字母
        if (!PrincipalUtil.isSimpleChar(decryptToken)) {
            return ServerResponseEntity.showFailMsg("token 格式有误");
        }
        return ServerResponseEntity.success(decryptToken);
    }

    public String getAccessKey(String accessToken) {
        return CacheNames.ACCESS + accessToken;
    }

    public String getUidToAccessKey(String approvalKey) {
        return CacheNames.UID_TO_ACCESS + approvalKey;
    }

    public String getRefreshToAccessKey(String refreshToken) {
        return CacheNames.REFRESH_TO_ACCESS + refreshToken;
    }

    public TokenInfoVO storeAndGetVo(UserInfoInTokenBO userInfoInToken) {
        TokenInfoBO tokenInfoBO = storeAccessToken(userInfoInToken);

        TokenInfoVO tokenInfoVO = new TokenInfoVO();
        tokenInfoVO.setAccessToken(tokenInfoBO.getAccessToken());
        tokenInfoVO.setRefreshToken(tokenInfoBO.getRefreshToken());
        tokenInfoVO.setExpiresIn(tokenInfoBO.getExpiresIn());
        return tokenInfoVO;
    }

    public void updateUserInfoByUidAndAppId(Integer userId, String appId, UserInfoInTokenBO userInfoInTokenBO) {
        if (userInfoInTokenBO == null) {
            return;
        }
        String uidKey = getUidToAccessKey(getApprovalKey(appId, userId));
        Set<String> tokenInfoBoList = stringRedisTemplate.opsForSet().members(uidKey);
        if (tokenInfoBoList == null || tokenInfoBoList.size() == 0) {
            throw new ChatroomException(ResponseEnum.UNAUTHORIZED);
        }
        for (String accessTokenWithRefreshToken : tokenInfoBoList) {
            String[] accessTokenWithRefreshTokenArr = accessTokenWithRefreshToken.split(StrUtil.COLON);
            String accessKey = this.getAccessKey(accessTokenWithRefreshTokenArr[0]);
            UserInfoInTokenBO oldUserInfoInTokenBO = (UserInfoInTokenBO) redisTemplate.opsForValue().get(accessKey);
            if (oldUserInfoInTokenBO == null) {
                continue;
            }
            BeanUtils.copyProperties(userInfoInTokenBO, oldUserInfoInTokenBO);
            redisTemplate.opsForValue().set(accessKey, Objects.requireNonNull(userInfoInTokenBO),getExpiresIn(userInfoInTokenBO.getSysType()), TimeUnit.SECONDS);
        }
    }
}
