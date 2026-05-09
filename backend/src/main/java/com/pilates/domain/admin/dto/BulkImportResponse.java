package com.pilates.domain.admin.dto;

import java.util.List;

public record BulkImportResponse(
        int successCount,
        int failureCount,
        List<FailureDetail> failures
) {
    public record FailureDetail(
            int row,
            String reason
    ) {}
}
