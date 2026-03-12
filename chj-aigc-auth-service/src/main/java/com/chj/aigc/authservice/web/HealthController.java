package com.chj.aigc.authservice.web;

import java.time.Instant;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证服务健康检查接口。
 * 用于本地联调时确认认证服务已经启动并注册到 Nacos。
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {
    @GetMapping
    public Map<String, Object> health() {
        return Map.of(
                "status", "ok",
                "service", "chj-aigc-auth-service",
                "time", Instant.now().toString()
        );
    }
}
