package com.chj.aigc.tenantservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 租户工作台微服务启动入口。
 * 当前先提供租户服务注册与基础健康检查，后续逐步承接项目、成员、素材和额度管理接口。
 */
@SpringBootApplication
@EnableDiscoveryClient
public class TenantServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(TenantServiceApplication.class, args);
    }
}
