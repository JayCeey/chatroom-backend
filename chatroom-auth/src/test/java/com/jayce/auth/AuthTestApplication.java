package com.jayce.auth;

import com.jayce.api.auth.bo.UserInfoInTokenBO;
import com.jayce.api.auth.feign.TokenFeignClient;
import com.jayce.auth.feign.TokenFeignController;
import com.jayce.auth.mapper.AuthAccountMapper;
import com.jayce.common.response.ServerResponseEntity;
import com.jayce.common.security.bo.AuthAccountInVerifyBO;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@MapperScan("com.jayce.auth.mapper")
@Slf4j
public class AuthTestApplication {
    @Autowired
    private AuthAccountMapper authAccountMapper;

    @Autowired
    private TokenFeignController tokenFeignController;

    @Autowired
    private TokenFeignClient tokenFeignClient;

    @Test
    public void test(){
        AuthAccountInVerifyBO authAccountInVerifyBO = authAccountMapper.getAuthAccountInVerifyByInputUserName(1, "hello", 1);
        System.out.println(authAccountInVerifyBO);
    }

    @Test
    public void testFeign(){
        ServerResponseEntity<UserInfoInTokenBO> test = tokenFeignClient.checkToken("MjY1NzJiZGVmYjI4NDgxY2FlYTRlNTZlZDQ3Mjc3NzYxNzI0ODMxOTU4MzkzMQ==");
        log.info("=================================以下是查询结果");
        log.info(test.toString());
    }
}
