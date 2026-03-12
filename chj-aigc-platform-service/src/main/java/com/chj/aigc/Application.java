package com.chj.aigc;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 平台管理服务启动入口。
 * 当前承载超管后台与部分租户后台能力，并注册到 Nacos 作为微服务体系中的平台服务。
 */
@SpringBootApplication
@MapperScan("com.chj.aigc.persistence.mapper")
@EnableDiscoveryClient
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
