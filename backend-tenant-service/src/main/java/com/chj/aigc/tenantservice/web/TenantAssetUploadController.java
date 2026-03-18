package com.chj.aigc.tenantservice.web;

import com.chj.aigc.tenantservice.asset.Asset;
import com.chj.aigc.tenantservice.asset.AssetUploadService;
import com.chj.aigc.tenantservice.auth.AuthInterceptor;
import com.chj.aigc.tenantservice.auth.AuthSession;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * 独立素材上传控制器，避免与已有租户工作台热文件产生并发冲突。
 */
@RestController
@RequestMapping("/api/tenant/assets")
public final class TenantAssetUploadController {
    private final AssetUploadService assetUploadService;

    public TenantAssetUploadController(AssetUploadService assetUploadService) {
        this.assetUploadService = assetUploadService;
    }

    @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Asset> upload(
            @RequestParam(required = false) String assetId,
            @RequestParam String projectId,
            @RequestParam String brandId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String kind,
            @RequestParam(required = false) List<String> tags,
            @RequestPart("file") MultipartFile file,
            HttpServletRequest request
    ) {
        requireTenantOwner(request);
        return ApiResponse.success(assetUploadService.upload(
                resolveTenantId(request),
                new AssetUploadService.UploadAssetCommand(
                        assetId,
                        projectId,
                        brandId,
                        name,
                        kind,
                        tags,
                        file
                )
        ));
    }

    private void requireTenantOwner(HttpServletRequest request) {
        if (!"tenant_owner".equals(currentSession(request).roleKey())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "当前账号没有执行该租户操作的权限");
        }
    }

    private AuthSession currentSession(HttpServletRequest request) {
        return (AuthSession) request.getAttribute(AuthInterceptor.REQUEST_SESSION_KEY);
    }

    private String resolveTenantId(HttpServletRequest request) {
        AuthSession session = currentSession(request);
        return session != null && session.tenantId() != null ? session.tenantId() : "tenant-demo";
    }
}
