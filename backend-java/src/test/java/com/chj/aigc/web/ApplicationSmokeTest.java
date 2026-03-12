package com.chj.aigc.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    private <T> T readData(MvcResult result, TypeReference<T> typeReference) throws Exception {
        Map<String, Object> envelope = objectMapper.readValue(
                result.getResponse().getContentAsByteArray(),
                new TypeReference<>() {
                }
        );
        assertEquals(0, envelope.get("code"));
        return objectMapper.convertValue(envelope.get("data"), typeReference);
    }

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

        Map<String, Object> payload = readData(result, new TypeReference<>() {
        });
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

        Map<String, Object> payload = readData(result, new TypeReference<>() {
        });
        assertEquals("ok", payload.get("status"));
    }

    @Test
    void tenantEndpointsReturnSeedData() throws Exception {
        String token = loginAsAdmin();

        MvcResult walletResult = mockMvc.perform(get("/api/tenant/wallet")
                        .header("X-Auth-Token", token))
                .andExpect(status().isOk())
                .andReturn();
        Map<String, Object> wallet = readData(walletResult, new TypeReference<>() {
        });
        assertEquals("tenant-demo", wallet.get("tenantId"));

        MvcResult clientsResult = mockMvc.perform(get("/api/tenant/clients")
                        .header("X-Auth-Token", token))
                .andExpect(status().isOk())
                .andReturn();
        List<Map<String, Object>> clients = readData(clientsResult, new TypeReference<>() {
        });
        assertFalse(clients.isEmpty());
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
        Map<String, Object> wallet = readData(walletResult, new TypeReference<>() {
        });
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
        Map<String, Object> quotas = readData(quotaResult, new TypeReference<>() {
        });
        assertEquals("48800.0", String.valueOf(quotas.get("userTokenRemaining")));
    }

    @Test
    void loginAndMeEndpointWork() throws Exception {
        String token = loginAsAdmin();

        MvcResult meResult = mockMvc.perform(get("/api/auth/me")
                        .header("X-Auth-Token", token))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> me = readData(meResult, new TypeReference<>() {
        });
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
        List<Map<String, Object>> clients = readData(clientsResult, new TypeReference<>() {
        });
        assertFalse(clients.isEmpty());

        MvcResult brandsResult = mockMvc.perform(get("/api/tenant/brands/client-created-1")
                        .header("X-Auth-Token", token))
                .andExpect(status().isOk())
                .andReturn();
        List<Map<String, Object>> brands = readData(brandsResult, new TypeReference<>() {
        });
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
        List<Map<String, Object>> projects = readData(projectsResult, new TypeReference<>() {
        });
        assertFalse(projects.isEmpty());

        MvcResult membersResult = mockMvc.perform(get("/api/tenant/members")
                        .header("X-Auth-Token", token))
                .andExpect(status().isOk())
                .andReturn();
        List<Map<String, Object>> members = readData(membersResult, new TypeReference<>() {
        });
        assertFalse(members.isEmpty());
    }

    @Test
    void tenantOwnerCanCreateMemberAndAssignUserQuota() throws Exception {
        String token = loginAsTenantOwner();

        String createMemberBody = """
                {
                  "userId": "user-created-member-1",
                  "username": "tenant_member_new",
                  "password": "Member@123",
                  "displayName": "新租户成员",
                  "roleKey": "tenant_member"
                }
                """;

        mockMvc.perform(post("/api/tenant/members")
                        .header("X-Auth-Token", token)
                        .contentType("application/json")
                        .content(createMemberBody))
                .andExpect(status().isOk());

        String quotaBody = """
                {
                  "allocationId": "quota-user-created-1",
                  "scopeType": "user",
                  "scopeId": "user-created-member-1",
                  "dimension": "tokens",
                  "limit": "20000",
                  "used": "500"
                }
                """;

        mockMvc.perform(post("/api/tenant/quotas")
                        .header("X-Auth-Token", token)
                        .contentType("application/json")
                        .content(quotaBody))
                .andExpect(status().isOk());

        MvcResult quotaListResult = mockMvc.perform(get("/api/tenant/quota-allocations")
                        .header("X-Auth-Token", token))
                .andExpect(status().isOk())
                .andReturn();
        List<Map<String, Object>> quotaAllocations = readData(quotaListResult, new TypeReference<>() {
        });
        assertFalse(quotaAllocations.isEmpty());

        MvcResult membersResult = mockMvc.perform(get("/api/tenant/members")
                        .header("X-Auth-Token", token))
                .andExpect(status().isOk())
                .andReturn();
        List<Map<String, Object>> members = readData(membersResult, new TypeReference<>() {
        });
        assertFalse(members.isEmpty());
    }

    @Test
    void tenantOwnerCanDisableAndEnableTenantMember() throws Exception {
        String ownerToken = loginAsTenantOwner();

        String createMemberBody = """
                {
                  "userId": "user-status-member-1",
                  "username": "tenant_member_status_1",
                  "password": "Member@123",
                  "displayName": "状态测试成员",
                  "roleKey": "tenant_member"
                }
                """;

        mockMvc.perform(post("/api/tenant/members")
                        .header("X-Auth-Token", ownerToken)
                        .contentType("application/json")
                        .content(createMemberBody))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/tenant/members/user-status-member-1/status")
                        .header("X-Auth-Token", ownerToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "active": false
                                }
                                """))
                .andExpect(status().isOk());

        MvcResult membersResult = mockMvc.perform(get("/api/tenant/members")
                        .header("X-Auth-Token", ownerToken))
                .andExpect(status().isOk())
                .andReturn();
        List<Map<String, Object>> members = readData(membersResult, new TypeReference<>() {
        });
        assertTrue(members.stream()
                .filter(member -> "user-status-member-1".equals(member.get("id")))
                .anyMatch(member -> Boolean.FALSE.equals(member.get("active"))));

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("""
                                {
                                  "username": "tenant_member_status_1",
                                  "password": "Member@123"
                                }
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/tenant/members/user-status-member-1/status")
                        .header("X-Auth-Token", ownerToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "active": true
                                }
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void tenantOwnerCanChangeMemberRole() throws Exception {
        String ownerToken = loginAsTenantOwner();

        mockMvc.perform(post("/api/tenant/members")
                        .header("X-Auth-Token", ownerToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "userId": "user-role-member-1",
                                  "username": "tenant_member_role_1",
                                  "password": "Member@123",
                                  "displayName": "角色测试成员",
                                  "roleKey": "tenant_member"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/tenant/members/user-role-member-1/role")
                        .header("X-Auth-Token", ownerToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "roleKey": "tenant_owner"
                                }
                                """))
                .andExpect(status().isOk());

        MvcResult membersResult = mockMvc.perform(get("/api/tenant/members")
                        .header("X-Auth-Token", ownerToken))
                .andExpect(status().isOk())
                .andReturn();
        List<Map<String, Object>> members = readData(membersResult, new TypeReference<>() {
        });
        assertTrue(members.stream()
                .filter(member -> "user-role-member-1".equals(member.get("id")))
                .anyMatch(member -> "tenant_owner".equals(member.get("roleKey"))));
    }

    @Test
    void tenantOwnerCannotAccessAdminApi() throws Exception {
        String token = loginAsTenantOwner();

        mockMvc.perform(get("/api/admin/users")
                        .header("X-Auth-Token", token))
                .andExpect(status().isForbidden());
    }

    @Test
    void tenantMemberCannotWriteTenantWorkspace() throws Exception {
        String ownerToken = loginAsTenantOwner();

        String createMemberBody = """
                {
                  "userId": "user-readonly-member-1",
                  "username": "tenant_member_readonly_1",
                  "password": "Member@123",
                  "displayName": "只读成员",
                  "roleKey": "tenant_member"
                }
                """;

        mockMvc.perform(post("/api/tenant/members")
                        .header("X-Auth-Token", ownerToken)
                        .contentType("application/json")
                        .content(createMemberBody))
                .andExpect(status().isOk());

        String token = login("tenant_member_readonly_1", "Member@123");

        String createProjectBody = """
                {
                  "projectId": "project-member-blocked",
                  "name": "成员不可创建项目"
                }
                """;

        mockMvc.perform(post("/api/tenant/projects")
                        .header("X-Auth-Token", token)
                        .contentType("application/json")
                        .content(createProjectBody))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/tenant/members/user-demo/status")
                        .header("X-Auth-Token", token)
                        .contentType("application/json")
                        .content("""
                                {
                                  "active": false
                                }
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/tenant/members/user-demo/role")
                        .header("X-Auth-Token", token)
                        .contentType("application/json")
                        .content("""
                                {
                                  "roleKey": "tenant_owner"
                                }
                                """))
                .andExpect(status().isForbidden());
    }
}
