package com.chj.aigc.web;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class DbInfoController {

    @Value("${spring.datasource.url:}")
    private String datasourceUrl;

    @Value("${spring.datasource.username:}")
    private String datasourceUsername;

    @Value("${APP_DB_PASSWORD:}")
    private String datasourcePassword;

    @GetMapping("/db-info")
    public ApiResponse<Map<String, Object>> dbInfo() {
        return ApiResponse.success(Map.of(
                "url", datasourceUrl,
                "username", datasourceUsername,
                "passwordConfigured", !datasourcePassword.isBlank()
        ));
    }
}
