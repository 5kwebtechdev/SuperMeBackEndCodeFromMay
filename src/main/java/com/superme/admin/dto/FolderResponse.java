package com.superme.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FolderResponse {
    private Long id;
    private String name;
    private Long parentId;
    private String parentName;
    private Long createdById;
    private String createdBy;
    private String createdAt;
    private String updatedAt;
    private int assetCount;
    private int subFolderCount;
}