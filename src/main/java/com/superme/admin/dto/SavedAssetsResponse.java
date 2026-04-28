package com.superme.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavedAssetsResponse {
    private FolderResponse currentFolder;
    private List<FolderResponse> subFolders;
    private List<AssetResponse> assets;
}
