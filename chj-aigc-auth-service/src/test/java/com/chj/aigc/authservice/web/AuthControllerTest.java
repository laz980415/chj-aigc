package com.chj.aigc.authservice.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chj.aigc.authservice.AuthServiceApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * 认证服务登录、当前用户和内部会话校验接口冒烟测试。
 */
@SpringBootTest(classes = AuthServiceApplication.class)
@AutoConfigureMockMvc
class AuthControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void loginAndMeEndpointsWork() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "tenant_owner",
                                  "password": "Tenant@123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.username").value("tenant_owner"))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        int start = body.indexOf("\"token\":\"");
        int valueStart = start + 9;
        int valueEnd = body.indexOf('"', valueStart);
        String token = body.substring(valueStart, valueEnd);

        mockMvc.perform(get("/api/auth/me")
                        .header("X-Auth-Token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.username").value("tenant_owner"));

        mockMvc.perform(get("/api/auth/introspect")
                        .header("X-Auth-Token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.token").value(token))
                .andExpect(jsonPath("$.data.username").value("tenant_owner"));
    }
}
