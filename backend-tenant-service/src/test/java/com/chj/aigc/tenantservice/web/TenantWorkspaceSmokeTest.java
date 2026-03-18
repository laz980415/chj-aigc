package com.chj.aigc.tenantservice.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chj.aigc.tenantservice.TenantServiceApplication;
import com.chj.aigc.tenantservice.auth.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 覆盖租户工作台关键链路：登录、项目、成员、额度。
 */
@SpringBootTest(
        classes = TenantServiceApplication.class,
        properties = {
                "auth.session-validation.mode=local"
        }
)
@AutoConfigureMockMvc
class TenantWorkspaceSmokeTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AuthService authService;

    @Test
    void tenantOwnerCanManageWorkspaceResources() throws Exception {
        String token = loginAndExtractToken("tenant_owner", "Tenant@123");

        mockMvc.perform(post("/api/tenant/projects")
                        .header("X-Auth-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "projectId": "tenant-service-project-ui",
                                  "name": "租户服务联调项目"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value("tenant-service-project-ui"));

        mockMvc.perform(post("/api/tenant/members")
                        .header("X-Auth-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "tenant-service-member-ui",
                                  "username": "tenant_member_ui",
                                  "password": "Member@123",
                                  "displayName": "租户联调成员",
                                  "roleKey": "tenant_member"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.username").value("tenant_member_ui"));

        mockMvc.perform(post("/api/tenant/quotas")
                        .header("X-Auth-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "allocationId": "tenant-service-quota-ui",
                                  "scopeType": "project",
                                  "scopeId": "tenant-service-project-ui",
                                  "dimension": "image_count",
                                  "limit": "66",
                                  "used": "3"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.projectImageRemaining").exists());

        mockMvc.perform(post("/api/tenant/wallet/payment-orders/wechat")
                        .header("X-Auth-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderId": "tenant-service-pay-ui",
                                  "tenantId": "tenant-demo",
                                  "amount": "88.00",
                                  "description": "租户服务模拟充值",
                                  "referenceId": "tenant-service-smoke"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value("tenant-service-pay-ui"));

        mockMvc.perform(post("/api/tenant/wallet/payment-orders/tenant-service-pay-ui/mock-paid")
                        .header("X-Auth-Token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("PAID"));

        mockMvc.perform(post("/api/tenant/clients")
                        .header("X-Auth-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clientId": "tenant-service-client-ui",
                                  "name": "租户服务广告主"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value("tenant-service-client-ui"));

        mockMvc.perform(post("/api/tenant/brands")
                        .header("X-Auth-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "brandId": "tenant-service-brand-ui",
                                  "clientId": "tenant-service-client-ui",
                                  "name": "租户服务品牌",
                                  "summary": "用于租户服务接口联调"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value("tenant-service-brand-ui"));

        mockMvc.perform(get("/api/tenant/projects")
                        .header("X-Auth-Token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id=='tenant-service-project-ui')]").exists());

        mockMvc.perform(get("/api/tenant/members")
                        .header("X-Auth-Token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id=='tenant-service-member-ui')]").exists());

        mockMvc.perform(get("/api/tenant/quota-allocations")
                        .header("X-Auth-Token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id=='tenant-service-quota-ui')]").exists());

        mockMvc.perform(get("/api/tenant/wallet")
                        .header("X-Auth-Token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.balance").exists());

        mockMvc.perform(get("/api/tenant/wallet/payment-orders")
                        .header("X-Auth-Token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id=='tenant-service-pay-ui')]").exists());

        mockMvc.perform(get("/api/tenant/clients")
                        .header("X-Auth-Token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id=='tenant-service-client-ui')]").exists());

        mockMvc.perform(get("/api/tenant/brands")
                        .header("X-Auth-Token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id=='tenant-service-brand-ui')]").exists());

        mockMvc.perform(get("/api/tenant/assets")
                        .header("X-Auth-Token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").exists());
    }

    @Test
    void tenantMemberCannotWriteWorkspaceResources() throws Exception {
        String ownerToken = loginAndExtractToken("tenant_owner", "Tenant@123");
        mockMvc.perform(post("/api/tenant/members")
                        .header("X-Auth-Token", ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "tenant-service-member-readonly",
                                  "username": "tenant_member_readonly",
                                  "password": "Member@123",
                                  "displayName": "只读成员",
                                  "roleKey": "tenant_member"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        String token = loginAndExtractToken("tenant_member_readonly", "Member@123");

        mockMvc.perform(post("/api/tenant/projects")
                        .header("X-Auth-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "projectId": "tenant-service-project-denied",
                                  "name": "无权限项目"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("当前账号没有执行该租户操作的权限"));
    }

    private String loginAndExtractToken(String username, String password) throws Exception {
        return authService.login(username, password).token();
    }
}
