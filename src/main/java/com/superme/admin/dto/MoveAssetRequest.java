package com.superme.admin.dto;


import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MoveAssetRequest {
    @NotNull(message = "Target folder ID is required")
    private Long targetFolderId;
}