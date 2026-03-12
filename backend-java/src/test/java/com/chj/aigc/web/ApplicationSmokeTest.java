package com.chj.aigc.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.chj.aigc.Application;
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
    private ObjectMapper objectMapper;

    private String login(String username, String password) throws Exception {
        String loginBody = """
                {
                  "username": "%s",
                  "password": "%s"
                }
                """.formatted(username, password);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> payload = objectMapper.readValue(
                result.getResponse().getContentAsByteArray(),
                new TypeReference<>() {
                }
        );
        return String.valueOf(payload.get("token"));
    }

    private String loginAsAdmin() throws Exception {
        return login("admin", "Admin@123");
    }

    private String loginAsTenantOwner() throws Exception {
        return login("tenant_owner", "Tenant@123");
    }

    @Test
    void healthEndpointReturnsOk() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> payload = objectMapper.readValue(
                result.getResponse().getContentAsByteArray(),
                new TypeReference<>() {
                }
        );
        assertEquals("ok", payload.get("status"));
    }

    @Test
    void tenantEndpointsReturnSeedData() throws Exception {
        String token = loginAsAdmin();

        MvcResult walletResult = mockMvc.perform(get("/api/tenant/wallet")
                        .header("X-Auth-Token", token))
                .andExpect(status().isOk())
                .andReturn();
        Map<String, Object> wallet = objectMapper.readValue(
                walletResult.getResponse().getContentAsByteArray(),
                new TypeReference<>() {
                }
        );
        assertEquals("tenant-demo", wallet.get("tenantId"));

        MvcResult clientsResult = mockMvc.perform(get("/api/tenant/clients")
                        .header("X-Auth-Token", token))
                .andExpect(status().isOk())
                .andReturn();
        List<Map<String, Object>> clients = objectMapper.readValue(
                clientsResult.getResponse().getContentAsByteArray(),
                new TypeReference<>() {
                }
        );
        assertFalse(clients.isEmpty());
    }

    @Test
    void dbInfoEndpointExposesConnectionShapeWithoutPassword() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/db-info"))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> payload = objectMapper.readValue(
                result.getResponse().getContentAsByteArray(),
                new TypeReference<>() {
                }
        );
        assertEquals("", payload.get("url"));
        assertEquals("", payload.get("username"));
        assertEquals(false, payload.get("passwordConfigured"));
    }

    @Test
    void adminAndTenantMutationEndpointsWork() throws Exception {
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

        String rechargeBody = """
                {
                  "entryId": "recharge-test-1",
                  "amount": "250.00",
                  "description": "top up",
                  "referenceId": "manual-topup"
                }
                """;

        MvcResult walletResult = mockMvc.perform(post("/api/tenant/wallet/recharge")
                        .header("X-Auth-Token", adminToken)
                        .contentType("application/json")
                        .content(rechargeBody))
                .andExpect(status().isOk())
                .andReturn();
        Map<String, Object> wallet = objectMapper.readValue(
                walletResult.getResponse().getContentAsByteArray(),
                new TypeReference<>() {
                }
        );
        assertEquals("1250.0000", wallet.get("balance"));

        String tenantToken = loginAsTenantOwner();

        String quotaBody = """
                {
                  "allocationId": "quota-created-1",
                  "scopeType": "project",
                  "scopeId": "project-demo",
                  "dimension": "video_seconds",
                  "limit": "300",
                  "used": "15"
                }
                """;

        MvcResult quotaResult = mockMvc.perform(post("/api/tenant/quotas")
                        .header("X-Auth-Token", tenantToken)
                        .contentType("application/json")
                        .content(quotaBody))
                .andExpect(status().isOk())
                .andReturn();
        Map<String, Object> quotas = objectMapper.readValue(
                quotaResult.getResponse().getContentAsByteArray(),
                new TypeReference<>() {
                }
        );
        assertEquals("48800", String.valueOf(quotas.get("userTokenRemaining")));
    }

    @Test
    void loginAndMeEndpointWork() throws Exception {
        String token = loginAsAdmin();

        MvcResult meResult = mockMvc.perform(get("/api/auth/me")
                        .header("X-Auth-Token", token))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> me = objectMapper.readValue(
                meResult.getResponse().getContentAsByteArray(),
                new TypeReference<>() {
                }
        );
        assertEquals("admin", me.get("username"));
        assertEquals("platform_super_admin", me.get("roleKey"));
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

        List<Map<String, Object>> users = objectMapper.readValue(
                result.getResponse().getContentAsByteArray(),
                new TypeReference<>() {
                }
        );
        assertFalse(users.isEmpty());
    }

    @Test
    void tenantCanCreateClientAndBrand() throws Exception {
        String token = loginAsTenantOwner();

        String createClientBody = """
                {
                  "clientId": "client-created-1",
                  "name": "新广告主"
                }
                """;

        mockMvc.perform(post("/api/tenant/clients")
                        .header("X-Auth-Token", token)
                        .contentType("application/json")
                        .content(createClientBody))
                .andExpect(status().isOk());

        String createBrandBody = """
                {
                  "brandId": "brand-created-1",
                  "clientId": "client-created-1",
                  "name": "新品牌",
                  "summary": "面向年轻用户的新品品牌"
                }
                """;

        mockMvc.perform(post("/api/tenant/brands")
                        .header("X-Auth-Token", token)
                        .contentType("application/json")
                        .content(createBrandBody))
                .andExpect(status().isOk());

        MvcResult clientsResult = mockMvc.perform(get("/api/tenant/clients")
                        .header("X-Auth-Token", token))
                .andExpect(status().isOk())
                .andReturn();
        List<Map<String, Object>> clients = objectMapper.readValue(
                clientsResult.getResponse().getContentAsByteArray(),
                new TypeReference<>() {
                }
        );
        assertFalse(clients.isEmpty());

        MvcResult brandsResult = mockMvc.perform(get("/api/tenant/brands/client-created-1")
                        .header("X-Auth-Token", token))
                .andExpect(status().isOk())
                .andReturn();
        List<Map<String, Object>> brands = objectMapper.readValue(
                brandsResult.getResponse().getContentAsByteArray(),
                new TypeReference<>() {
                }
        );
        assertFalse(brands.isEmpty());
    }

    @Test
    void tenantCanCreateProjectAndSeeMembers() throws Exception {
        String token = loginAsTenantOwner();

        String createProjectBody = """
                {
                  "projectId": "project-created-1",
                  "name": "春季投放项目"
                }
                """;

        mockMvc.perform(post("/api/tenant/projects")
                        .header("X-Auth-Token", token)
                        .contentType("application/json")
                        .content(createProjectBody))
                .andExpect(status().isOk());

        MvcResult projectsResult = mockMvc.perform(get("/api/tenant/projects")
                        .header("X-Auth-Token", token))
                .andExpect(status().isOk())
                .andReturn();
        List<Map<String, Object>> projects = objectMapper.readValue(
                projectsResult.getResponse().getContentAsByteArray(),
                new TypeReference<>() {
                }
        );
        assertFalse(projects.isEmpty());

        MvcResult membersResult = mockMvc.perform(get("/api/tenant/members")
                        .header("X-Auth-Token", token))
                .andExpect(status().isOk())
                .andReturn();
        List<Map<String, Object>> members = objectMapper.readValue(
                membersResult.getResponse().getContentAsByteArray(),
                new TypeReference<>() {
                }
        );
        assertFalse(members.isEmpty());
    }

    @Test
    void tenantOwnerCannotAccessAdminApi() throws Exception {
        String token = loginAsTenantOwner();

        mockMvc.perform(get("/api/admin/users")
                        .header("X-Auth-Token", token))
                .andExpect(status().isForbidden());
    }
}
