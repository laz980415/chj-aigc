package com.chj.aigc.tenantservice.asset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import org.springframework.http.HttpMethod;

class RemoteAssetGroundingClientTest {
    @Test
    void remoteClientMapsGroundingContextResponse() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://127.0.0.1:18084");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://127.0.0.1:18084/api/model/assets/grounding-context"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andRespond(withSuccess("""
                        {
                          "context_summary": "Retrieved 1 semantic asset chunks for brand grounding.",
                          "snippets": [
                            {
                              "asset_id": "asset-demo-1",
                              "asset_name": "演示主视觉",
                              "asset_kind": "image",
                              "source_uri": "E:/uploads/demo.png",
                              "content_text": "图片素材描述",
                              "summary": "主视觉摘要",
                              "page_no": null,
                              "frame_no": null
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        RemoteAssetGroundingClient client = new RemoteAssetGroundingClient(builder.build());
        AssetGroundingClient.GroundingContext context = client.buildContext(
                "tenant-demo",
                "brand-demo",
                "生成新品广告文案",
                List.of(new Asset(
                        "asset-demo-1",
                        "tenant-demo",
                        "project-demo",
                        "client-demo",
                        "brand-demo",
                        "演示主视觉",
                        AssetKind.IMAGE,
                        "E:/uploads/demo.png",
                        java.util.Set.of("hero"),
                        true
                )),
                5
        );

        assertEquals("Retrieved 1 semantic asset chunks for brand grounding.", context.contextSummary());
        assertEquals(1, context.snippets().size());
        assertEquals("asset-demo-1", context.snippets().get(0).assetId());
        assertTrue(context.snippets().get(0).contentText().contains("图片素材"));
        server.verify();
    }
}
