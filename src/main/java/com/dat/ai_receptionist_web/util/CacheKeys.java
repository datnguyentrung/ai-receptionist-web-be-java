package com.dat.ai_receptionist_web.util;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.stream.Collectors;

public final class CacheKeys {
    private CacheKeys() {
    }

    public static String pageable(Pageable pageable) {
        if (pageable == null || pageable.isUnpaged()) {
            return "unpaged";
        }

        return "page:%d:size:%d:sort:%s".formatted(
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
