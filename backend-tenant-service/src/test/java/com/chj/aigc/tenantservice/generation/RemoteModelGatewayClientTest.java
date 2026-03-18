package com.chj.aigc.tenantservice.generation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class RemoteModelGatewayClientTest {
    @Test
    void submitJobSendsJsonBodyToModelService() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        AtomicReference<String> contentType = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/model/jobs", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            contentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
            byte[] responseBody = """
                    {
                      "job_id": "job-123",
                      "status": "succeeded"
                    }
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, responseBody.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(responseBody);
            }
        });
        server.start();
        try {
            RemoteModelGatewayClient client = new RemoteModelGatewayClient(
                    "http://127.0.0.1:" + server.getAddress().getPort(),
                    new ObjectMapper()
            );

            client.submitJob(new ModelGatewayClient.SubmitPayload(
                    "tenant-demo",
                    "project-demo",
                    "user-tenant-owner",
                    "copy-standard",
                    "copywriting",
                    "生成春季活动文案",
                    "示例客户",
                    "示例品牌",
                    "品牌摘要",
                    List.of(new ModelGatewayClient.AssetPayload(
                            "asset-demo-1",
                            "主视觉海报",
                            "image",
                            "oss://tenant-demo/assets/poster.png",
                            List.of("hero", "spring"),
                            "活动 KV"
                    ))
            ));

            assertNotNull(requestBody.get());
            assertTrue(contentType.get().startsWith("application/json"));

            JsonNode payload = new ObjectMapper().readTree(requestBody.get());
            assertEquals("tenant-demo", payload.get("tenant_id").asText());
            assertEquals("project-demo", payload.get("project_id").asText());
            assertEquals("user-tenant-owner", payload.get("actor_id").asText());
            assertEquals("copy-standard", payload.get("model_alias").asText());
            assertEquals("copywriting", payload.get("capability").asText());
            assertEquals("生成春季活动文案", payload.get("user_prompt").asText());
            assertEquals("示例品牌", payload.get("brand_name").asText());
            assertEquals("品牌摘要", payload.get("brand_summary").asText());
            assertEquals("示例客户", payload.get("client_name").asText());
            assertEquals("asset-demo-1", payload.get("assets").get(0).get("id").asText());
        } finally {
            server.stop(0);
        }
    }
}
