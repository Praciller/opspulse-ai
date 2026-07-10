package com.opspulse.shared.web;

import java.util.List;

public record PagedResponse<T>(
        List<T> items,
        int page,
        int size,
        long total,
        int totalPages) {

    public PagedResponse {
        items = items == null ? List.of() : List.copyOf(items);
        if (page < 0 || size < 0 || total < 0 || totalPages < 0) {
            throw new IllegalArgumentException("Pagination values must not be negative");
        }
    }
}
