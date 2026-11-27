package com.dat.ai_receptionist_web.util;

import com.dat.ai_receptionist_web.context.TenantContext;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.UUID;
import java.util.stream.Collectors;

public final class CacheKeys {
    private CacheKeys() {
    }

    public static String tenantPrefix() {
        UUID tenantId = TenantContext.getTenantIdOrNull();
        return tenantId != null ? "tenant:" + tenantId : "tenant:global";
    }

    public static String pageable(Pageable pageable) {
        if (pageable == null || pageable.isUnpaged()) {
            return "%s:unpaged".formatted(tenantPrefix());
        }

        return "%s:page:%d:size:%d:sort:%s".formatted(
                tenantPrefix(),
                pageable.getPageNumber(),
                pageable.getPageSize(),
                sort(pageable.getSort())
        );
    }

    private static String sort(Sort sort) {
        if (sort == null || sort.isUnsorted()) {
            return "unsorted";
        }

        return sort.stream()
                .map(order -> "%s,%s,%s,%s".formatted(
                        order.getProperty(),
                        order.getDirection(),
                        order.getNullHandling(),
                        order.isIgnoreCase()
                ))
                .collect(Collectors.joining("|"));
    }
}
