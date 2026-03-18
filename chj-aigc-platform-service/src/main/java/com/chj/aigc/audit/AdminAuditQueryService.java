package com.chj.aigc.audit;

import com.chj.aigc.access.ModelAccessAdminStore;
import com.chj.aigc.access.ModelAccessAuditEvent;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 提供平台审计事件的只读过滤与分页。
 * 当前 tenant 维度只能从 TENANT 范围规则事件中推断，后续如有专门字段可下推到存储层。
 */
@Service
public final class AdminAuditQueryService {
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final ModelAccessAdminStore store;

    public AdminAuditQueryService(ModelAccessAdminStore store) {
        this.store = store;
    }

    public AuditEventPage query(AuditEventQuery query) {
        int page = query.page() == null ? DEFAULT_PAGE : query.page();
        int pageSize = query.pageSize() == null ? DEFAULT_PAGE_SIZE : query.pageSize();
        if (page < 1) {
            throw new IllegalArgumentException("page 必须大于等于 1");
        }
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("pageSize 必须介于 1 和 100 之间");
        }
        if (query.startAt() != null && query.endAt() != null && query.startAt().isAfter(query.endAt())) {
            throw new IllegalArgumentException("startAt 不能晚于 endAt");
        }

        List<AuditEventItem> filtered = store.listAuditEvents().stream()
                .map(this::toItem)
                .filter(item -> matchesTenant(item, query.tenantId()))
                .filter(item -> matchesEventType(item, query.eventType()))
                .filter(item -> matchesStartAt(item, query.startAt()))
                .filter(item -> matchesEndAt(item, query.endAt()))
                .sorted(Comparator.comparing(AuditEventItem::createdAt).reversed())
                .toList();

        int total = filtered.size();
        int fromIndex = Math.min((page - 1) * pageSize, total);
        int toIndex = Math.min(fromIndex + pageSize, total);
        int totalPages = total == 0 ? 0 : (total + pageSize - 1) / pageSize;
        return new AuditEventPage(
                page,
                pageSize,
                total,
                totalPages,
                page < totalPages,
                filtered.subList(fromIndex, toIndex)
        );
    }

    private AuditEventItem toItem(ModelAccessAuditEvent event) {
        String tenantId = isTenantScoped(event) ? event.targetScopeValue() : null;
        return new AuditEventItem(
                event.id(),
                event.actorId(),
                event.action(),
                event.targetModelAlias(),
                event.targetScopeType(),
                event.targetScopeValue(),
                tenantId,
                event.detail(),
                event.createdAt()
        );
    }

    private boolean matchesTenant(AuditEventItem item, String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            return true;
        }
        return tenantId.equals(item.tenantId());
    }

    private boolean matchesEventType(AuditEventItem item, String eventType) {
        if (eventType == null || eventType.isBlank()) {
            return true;
        }
        return item.eventType().equalsIgnoreCase(eventType);
    }

    private boolean matchesStartAt(AuditEventItem item, Instant startAt) {
        return startAt == null || !item.createdAt().isBefore(startAt);
    }

    private boolean matchesEndAt(AuditEventItem item, Instant endAt) {
        return endAt == null || !item.createdAt().isAfter(endAt);
    }

    private boolean isTenantScoped(ModelAccessAuditEvent event) {
        return "TENANT".equalsIgnoreCase(event.targetScopeType());
    }

    public record AuditEventQuery(
            String tenantId,
            String eventType,
            Instant startAt,
            Instant endAt,
            Integer page,
            Integer pageSize
    ) {
    }

    public record AuditEventPage(
            int page,
            int pageSize,
            int total,
            int totalPages,
            boolean hasNext,
            List<AuditEventItem> items
    ) {
    }

    public record AuditEventItem(
            String id,
            String actorId,
            String eventType,
            String targetModelAlias,
            String targetScopeType,
            String targetScopeValue,
            String tenantId,
            String detail,
            Instant createdAt
    ) {
    }
}
