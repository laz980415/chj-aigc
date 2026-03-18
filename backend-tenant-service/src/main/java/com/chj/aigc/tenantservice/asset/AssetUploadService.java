package com.chj.aigc.tenantservice.asset;

import com.chj.aigc.tenantservice.tenant.TenantProjectStore;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 负责租户素材上传的本地文件落盘与元数据登记。
 * 原始文件当前落本地磁盘，后续如切到 COS 只替换这一层存储实现即可。
 */
@Service
public final class AssetUploadService {
    private static final DateTimeFormatter DATE_SEGMENT = DateTimeFormatter.BASIC_ISO_DATE;

    private final AssetCatalogStore assetCatalogStore;
    private final TenantProjectStore tenantProjectStore;
    private final AssetIngestionPipeline ingestionPipeline;
    private final Path uploadRootDir;

    public AssetUploadService(
            AssetCatalogStore assetCatalogStore,
            TenantProjectStore tenantProjectStore,
            AssetIngestionPipeline ingestionPipeline,
            @Value("${tenant.asset-upload.root-dir:${user.dir}/uploads/tenant-assets}") String uploadRootDir
    ) {
        this.assetCatalogStore = assetCatalogStore;
        this.tenantProjectStore = tenantProjectStore;
        this.ingestionPipeline = ingestionPipeline;
        this.uploadRootDir = Path.of(uploadRootDir).toAbsolutePath().normalize();
    }

    public Asset upload(String tenantId, UploadAssetCommand command) {
        if (command.file() == null || command.file().isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }

        Brand brand = assetCatalogStore.listBrands(tenantId).stream()
                .filter(Brand::active)
                .filter(item -> item.id().equals(command.brandId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("品牌不存在，无法上传素材"));
        boolean projectExists = tenantProjectStore.listProjects(tenantId).stream()
                .filter(project -> project.active())
                .map(project -> project.id())
                .anyMatch(command.projectId()::equals);
        if (!projectExists) {
            throw new IllegalArgumentException("项目不存在，无法上传素材");
        }

        String assetId = command.assetId() == null || command.assetId().isBlank()
                ? "asset-" + UUID.randomUUID().toString().replace("-", "")
                : command.assetId();
        String originalFilename = sanitizedFilename(command.file().getOriginalFilename());
        String extension = extensionOf(originalFilename);
        String storedFilename = extension.isBlank() ? assetId : assetId + "." + extension;
        String displayName = command.name() == null || command.name().isBlank()
                ? baseNameOf(originalFilename, assetId)
                : command.name();
        AssetKind kind = resolveKind(command.kind(), command.file(), originalFilename);
        Set<String> tags = normalizedTags(command.tags());

        Path storedFile = storeFile(tenantId, brand.id(), storedFilename, command.file());
        Asset asset = new Asset(
                assetId,
                tenantId,
                command.projectId(),
                brand.clientId(),
                brand.id(),
                displayName,
                kind,
                storedFile.toString(),
                tags,
                true
        );
        assetCatalogStore.saveAsset(asset);
        ingestionPipeline.ingest(new AssetIngestionPipeline.AssetIngestionCommand(asset, storedFile));
        return asset;
    }

    private Path storeFile(String tenantId, String brandId, String storedFilename, MultipartFile file) {
        try {
            Path targetDir = uploadRootDir
                    .resolve(tenantId)
                    .resolve(brandId)
                    .resolve(LocalDate.now().format(DATE_SEGMENT));
            Files.createDirectories(targetDir);
            Path targetFile = targetDir.resolve(storedFilename).normalize();
            if (!targetFile.startsWith(targetDir)) {
                throw new IllegalArgumentException("上传文件名不合法");
            }
            file.transferTo(targetFile);
            return targetFile;
        } catch (IOException exception) {
            throw new IllegalStateException("素材文件写入失败", exception);
        }
    }

    private AssetKind resolveKind(String kind, MultipartFile file, String filename) {
        if (kind != null && !kind.isBlank()) {
            return AssetKind.valueOf(kind.trim().toUpperCase(Locale.ROOT));
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (contentType.startsWith("image/")) {
            return AssetKind.IMAGE;
        }
        if (contentType.startsWith("video/")) {
            return AssetKind.VIDEO;
        }
        if (contentType.startsWith("text/")
                || contentType.contains("pdf")
                || contentType.contains("word")
                || contentType.contains("document")) {
            return AssetKind.DOCUMENT;
        }

        String extension = extensionOf(filename);
        return switch (extension) {
            case "png", "jpg", "jpeg", "gif", "webp", "bmp" -> AssetKind.IMAGE;
            case "mp4", "mov", "avi", "mkv", "webm" -> AssetKind.VIDEO;
            case "pdf", "doc", "docx", "txt", "md" -> AssetKind.DOCUMENT;
            default -> throw new IllegalArgumentException("无法识别素材类型，请显式传入 kind");
        };
    }

    private Set<String> normalizedTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return Set.of();
        }
        return tags.stream()
                .flatMap(tag -> Arrays.stream(tag.split(",")))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .collect(Collectors.toSet());
    }

    private String sanitizedFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "upload.bin";
        }
        String filename = Path.of(originalFilename).getFileName().toString();
        return filename.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private String extensionOf(String filename) {
        int index = filename.lastIndexOf('.');
        if (index < 0 || index == filename.length() - 1) {
            return "";
        }
        return filename.substring(index + 1).toLowerCase(Locale.ROOT);
    }

    private String baseNameOf(String filename, String fallback) {
        int index = filename.lastIndexOf('.');
        if (index <= 0) {
            return filename == null || filename.isBlank() ? fallback : filename;
        }
        return filename.substring(0, index);
    }

    public record UploadAssetCommand(
            String assetId,
            String projectId,
            String brandId,
            String name,
            String kind,
            List<String> tags,
            MultipartFile file
    ) {
    }
}
