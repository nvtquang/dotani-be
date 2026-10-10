package com.dotani.member.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record MemberLookupRequest(
        @NotEmpty
        @Size(max = 200)
        List<@Size(max = 36) String> memberIds
) {
}
