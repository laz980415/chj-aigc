package com.chj.aigc.web;

import com.chj.aigc.audit.AdminAuditQueryService;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 平台审计查询接口。
 * 只承载查询职责，避免与现有超管写接口文件形成并发冲突。
 */
@RestController
@RequestMapping("/api/admin/audit-events")
public final class AdminAuditQueryController {
    private final AdminAuditQueryService auditQueryService;

    public AdminAuditQueryController(AdminAuditQueryService auditQueryService) {
        this.auditQueryService = auditQueryService;
    }

    @GetMapping
    public ApiResponse<AdminAuditQueryService.AuditEventPage> auditEvents(
            @RequestParam(required = false) String tenantId,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String startAt,
            @RequestParam(required = false) String endAt,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize
    ) {
        return ApiResponse.success(auditQueryService.query(new AdminAuditQueryService.AuditEventQuery(
                tenantId,
                eventType,
                parseInstant(startAt, "startAt"),
                parseInstant(endAt, "endAt"),
                page,
                pageSize
        )));
    }

    private Instant parseInstant(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(fieldName + " 必须是 ISO-8601 时间，例如 2026-03-18T00:00:00Z");
        }
    }
}
