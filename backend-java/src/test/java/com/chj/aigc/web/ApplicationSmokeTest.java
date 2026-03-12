package com.chj.aigc.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.chj.aigc.Application;
import com.chj.aigc.auth.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.core.type.TypeReference;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = Application.class)
@AutoConfigureMockMvc
class ApplicationSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Autowired
    private ObjectMapper objectMapper;

    private <T> T readData(MvcResult result, TypeReference<T> typeReference) throws Exception {
        Map<String, Object> envelope = objectMapper.readValue(
                result.getResponse().getContentAsByteArray(),
                new TypeReference<>() {
                }
        );
        assertEquals(0, envelope.get("code"));
        return objectMapper.convertValue(envelope.get("data"), typeReference);
    }

    private String loginAsAdmin() throws Exception {
        return authService.login("admin", "Admin@123").token();
    }

    @Test
    void healthEndpointReturnsOk() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> payload = readData(result, new TypeReference<>() {
        });
        assertEquals("ok", payload.get("status"));
    }

    @Test
    void dbInfoEndpointExposesConnectionShapeWithoutPassword() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/db-info"))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> payload = readData(result, new TypeReference<>() {
        });
        assertTrue(payload.containsKey("url"));
        assertTrue(payload.containsKey("username"));
        assertTrue(payload.containsKey("passwordConfigured"));
    }

    @Test
    void adminApisWork() throws Exception {
        String adminToken = loginAsAdmin();

        String createRuleBody = """
                {
                  "ruleId": "rule-created-1",
                  "actorId": "super-admin",
                  "platformModelAlias": "image-standard",
                  "scopeType": "project",
                  "scopeValue": "project-demo",
                  "effect": "allow",
                  "reason": "Enable image generation for demo project"
                }
                """;

        mockMvc.perform(post("/api/admin/model-access-rules")
                        .header("X-Auth-Token", adminToken)
                        .contentType("application/json")
                        .content(createRuleBody))
                .andExpect(status().isOk());
    }

    @Test
    void adminCanCreateAndListUsers() throws Exception {
        String token = loginAsAdmin();

        String createUserBody = """
                {
                  "userId": "user-created-1",
                  "username": "member_demo",
                  "password": "Member@123",
                  "displayName": "演示成员",
                  "roleKey": "tenant_member",
                  "tenantId": "tenant-demo"
                }
                """;

        mockMvc.perform(post("/api/admin/users")
                        .header("X-Auth-Token", token)
                        .contentType("application/json")
                        .content(createUserBody))
                .andExpect(status().isOk());

        MvcResult result = mockMvc.perform(get("/api/admin/users")
                        .header("X-Auth-Token", token))
                .andExpect(status().isOk())
                .andReturn();

        List<Map<String, Object>> users = readData(result, new TypeReference<>() {
        });
        assertFalse(users.isEmpty());
    }

    @Test
    void adminCanViewTenantOverview() throws Exception {
        String token = loginAsAdmin();

        MvcResult result = mockMvc.perform(get("/api/admin/tenants")
                        .header("X-Auth-Token", token))
                .andExpect(status().isOk())
                .andReturn();

        List<Map<String, Object>> tenants = readData(result, new TypeReference<>() {
        });
        assertFalse(tenants.isEmpty());
    }

    @Test
    void adminCanViewTenantDetail() throws Exception {
        String token = loginAsAdmin();

        MvcResult result = mockMvc.perform(get("/api/admin/tenants/tenant-demo")
                        .header("X-Auth-Token", token))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> detail = readData(result, new TypeReference<>() {
        });
        assertTrue(detail.containsKey("tenant"));
        assertTrue(detail.containsKey("members"));
        assertTrue(detail.containsKey("rules"));
        assertTrue(detail.containsKey("ledgerEntries"));
        assertTrue(detail.containsKey("paymentOrders"));
    }

    @Test
    void tenantOwnerCannotAccessAdminApi() throws Exception {
        String token = authService.login("tenant_owner", "Tenant@123").token();

        mockMvc.perform(get("/api/admin/users")
                        .header("X-Auth-Token", token))
                .andExpect(status().isForbidden());
    }

    @Test
    void platformServiceNoLongerServesTenantOrAuthEndpoints() throws Exception {
        String adminToken = loginAsAdmin();

        mockMvc.perform(get("/api/tenant/wallet")
                        .header("X-Auth-Token", adminToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("""
                                {
                                  "username": "admin",
                                  "password": "Admin@123"
                                }
                                """))
                .andExpect(status().isNotFound());
    }
}
