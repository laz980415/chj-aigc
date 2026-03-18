package com.chj.aigc.tenantservice.web;

import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chj.aigc.tenantservice.TenantServiceApplication;
import com.chj.aigc.tenantservice.auth.AuthService;
import com.chj.aigc.tenantservice.generation.ModelAccessClient;
import com.chj.aigc.tenantservice.generation.ModelGatewayClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
        classes = TenantServiceApplication.class,
        properties = {
                "auth.session-validation.mode=local"
        }
)
@AutoConfigureMockMvc
class GenerationControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AuthService authService;
    @MockBean
    private ModelAccessClient modelAccessClient;
    @MockBean
    private ModelGatewayClient modelGatewayClient;

    @Test
    void tenantCanSubmitCopyGenerationJob() throws Exception {
        when(modelAccessClient.evaluate(eq("tenant-demo"), eq("project-demo"), anySet(), eq("copy-standard")))
                .thenReturn(new ModelAccessClient.ModelAccessDecision(true, "allowed", "rule-demo"));
        when(modelGatewayClient.submitJob(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new ModelGatewayClient.JobResult(
                        "job-copy-ui",
                        "succeeded",
                        "品牌安全文案输出",
                        "",
                        "",
                        "openai",
                        "gpt-4o",
                        "",
                        42,
                        96,
                        null,
                        null
                ));

        String token = authService.login("tenant_owner", "Tenant@123").token();

        mockMvc.perform(post("/api/tenant/generation/jobs")
                        .header("X-Auth-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "projectId": "project-demo",
                                  "modelAlias": "copy-standard",
                                  "capability": "copywriting",
                                  "userPrompt": "生成一条新品上市广告文案",
                                  "brandId": "brand-demo"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.job_id").value("job-copy-ui"))
                .andExpect(jsonPath("$.data.status").value("succeeded"))
                .andExpect(jsonPath("$.data.provider_id").value("openai"))
                .andExpect(jsonPath("$.data.charge_amount").exists());
    }

    @Test
    void pendingVideoJobCanBeRefreshedToSucceeded() throws Exception {
        when(modelAccessClient.evaluate(eq("tenant-demo"), eq("project-demo"), anySet(), eq("video-standard")))
                .thenReturn(new ModelAccessClient.ModelAccessDecision(true, "allowed", "rule-video"));
        when(modelGatewayClient.submitJob(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new ModelGatewayClient.JobResult(
                        "job-video-ui",
                        "pending",
                        "",
                        "",
                        "",
                        "openai",
                        "sora-2",
                        "provider-job-video-ui",
                        null,
                        null,
                        null,
                        10
                ));
        when(modelGatewayClient.fetchJob("job-video-ui"))
                .thenReturn(new ModelGatewayClient.JobResult(
                        "job-video-ui",
                        "succeeded",
                        "",
                        "oss://generated/sora-2/job-video-ui.mp4",
                        "",
                        "openai",
                        "sora-2",
                        "provider-job-video-ui",
                        null,
                        null,
                        null,
                        10
                ));

        String token = authService.login("tenant_owner", "Tenant@123").token();

        mockMvc.perform(post("/api/tenant/generation/jobs")
                        .header("X-Auth-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "projectId": "project-demo",
                                  "modelAlias": "video-standard",
                                  "capability": "video_generation",
                                  "userPrompt": "生成一个 10 秒短视频",
                                  "brandId": "brand-demo"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("pending"));

        mockMvc.perform(get("/api/tenant/generation/jobs/job-video-ui")
                        .header("X-Auth-Token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("succeeded"))
                .andExpect(jsonPath("$.data.output_uri").value("oss://generated/sora-2/job-video-ui.mp4"))
                .andExpect(jsonPath("$.data.charge_amount").exists());
    }
}
