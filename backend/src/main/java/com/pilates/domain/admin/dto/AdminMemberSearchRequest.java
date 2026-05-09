package com.pilates.domain.admin.dto;

public record AdminMemberSearchRequest(
        String search,
        String status,
        int page,
        int size,
        String sort
) {
    public AdminMemberSearchRequest {
        if (page < 0) page = 0;
        if (size <= 0 || size > 100) size = 20;
        if (sort == null || sort.isBlank()) sort = "createdAt,desc";
    }
}
