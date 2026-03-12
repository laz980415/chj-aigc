package com.chj.aigc.gateway.web;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 网关健康检查接口。
 */
@RestController
public class GatewayHealthController {
    @GetMapping("/gateway/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "ok",
                "service", "gateway-service"
        );
    }
}
