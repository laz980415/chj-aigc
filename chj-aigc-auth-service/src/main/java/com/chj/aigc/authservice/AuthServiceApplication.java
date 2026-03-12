package com.chj.aigc.authservice;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 独立身份认证服务启动入口。
 * 当前先提供注册发现和健康检查骨架，后续承接 auth_users 和 auth_sessions 的唯一写入职责。
 */
@SpringBootApplication
@MapperScan("com.chj.aigc.authservice.persistence.mapper")
public class AuthServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
