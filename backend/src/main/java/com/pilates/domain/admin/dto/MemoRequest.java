package com.pilates.domain.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MemoRequest(
        @NotBlank(message = "메모 내용을 입력해주세요.")
        @Size(max = 2000, message = "메모는 2000자 이하로 입력해주세요.")
        String content
) {}
