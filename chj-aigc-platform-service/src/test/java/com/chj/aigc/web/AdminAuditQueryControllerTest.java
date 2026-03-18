package com.chj.aigc.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chj.aigc.access.InMemoryModelAccessAdminStore;
import com.chj.aigc.access.ModelAccessAuditEvent;
import com.chj.aigc.audit.AdminAuditQueryService;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AdminAuditQueryControllerTest {
    private InMemoryModelAccessAdminStore store;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        store = new InMemoryModelAccessAdminStore();
        AdminAuditQueryService auditQueryService = new AdminAuditQueryService(store);
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminAuditQueryController(auditQueryService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void auditEventsEndpointSupportsPagination() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String tenantId = "tenant-audit-page-" + suffix;
        store.saveAuditEvent(new ModelAccessAuditEvent(
                "audit-page-1-" + suffix,
                "super-admin",
                "RULE_CREATED",
                "copy-standard",
                "TENANT",
                tenantId,
                "page-1",
                Instant.parse("2026-03-18T01:00:00Z")
        ));
        store.saveAuditEvent(new ModelAccessAuditEvent(
                "audit-page-2-" + suffix,
                "super-admin",
                "RULE_DISABLED",
                "image-standard",
                "TENANT",
                tenantId,
                "page-2",
                Instant.parse("2026-03-18T02:00:00Z")
        ));
        store.saveAuditEvent(new ModelAccessAuditEvent(
                "audit-page-3-" + suffix,
                "super-admin",
                "RULE_CREATED",
                "video-standard",
                "TENANT",
                tenantId,
                "page-3",
                Instant.parse("2026-03-18T03:00:00Z")
        ));

        mockMvc.perform(get("/api/admin/audit-events")
                        .param("tenantId", tenantId)
                        .param("page", "2")
                        .param("pageSize", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.page").value(2))
                .andExpect(jsonPath("$.data.pageSize").value(2))
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.totalPages").value(2))
                .andExpect(jsonPath("$.data.hasNext").value(false))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value("audit-page-1-" + suffix));
    }

    @Test
    void auditEventsEndpointSupportsTenantTimeRangeAndEventTypeFilters() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String tenantId = "tenant-audit-filter-" + suffix;
        store.saveAuditEvent(new ModelAccessAuditEvent(
                "audit-filter-1-" + suffix,
                "super-admin",
                "RULE_CREATED",
                "copy-standard",
                "TENANT",
                tenantId,
                "before-range",
                Instant.parse("2026-03-18T00:30:00Z")
        ));
        store.saveAuditEvent(new ModelAccessAuditEvent(
                "audit-filter-2-" + suffix,
                "super-admin",
                "RULE_DISABLED",
                "copy-standard",
                "TENANT",
                tenantId,
                "in-range",
                Instant.parse("2026-03-18T01:30:00Z")
        ));
        store.saveAuditEvent(new ModelAccessAuditEvent(
                "audit-filter-3-" + suffix,
                "super-admin",
                "RULE_DISABLED",
                "copy-standard",
                "TENANT",
                "tenant-other-" + suffix,
                "other-tenant",
                Instant.parse("2026-03-18T01:45:00Z")
        ));

        mockMvc.perform(get("/api/admin/audit-events")
                        .param("tenantId", tenantId)
                        .param("eventType", "RULE_DISABLED")
                        .param("startAt", "2026-03-18T01:00:00Z")
                        .param("endAt", "2026-03-18T02:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value("audit-filter-2-" + suffix))
                .andExpect(jsonPath("$.data.items[0].tenantId").value(tenantId))
                .andExpect(jsonPath("$.data.items[0].eventType").value("RULE_DISABLED"));
    }

    @Test
    void auditEventsEndpointRejectsInvalidTimeRange() throws Exception {
        mockMvc.perform(get("/api/admin/audit-events")
                        .param("startAt", "2026-03-18T03:00:00Z")
                        .param("endAt", "2026-03-18T01:00:00Z"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("startAt 不能晚于 endAt"));
    }
}
