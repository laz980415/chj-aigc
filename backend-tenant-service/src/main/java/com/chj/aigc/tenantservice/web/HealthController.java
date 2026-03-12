package com.chj.aigc.tenantservice.web;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 租户微服务健康检查接口。
 * 供 Nacos 注册后联调和网关探活使用。
 */
@RestController
public class HealthController {
    @GetMapping("/api/health")
    public ApiResponse<Map<String, Object>> health() {
        return ApiResponse.success(Map.of(
                "status", "ok",
                "service", "tenant-service"
        ));
    }
}
