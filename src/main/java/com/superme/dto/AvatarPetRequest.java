package com.superme.dto;

import lombok.Data;

@Data
public class AvatarPetRequest {
    private String name;
    private String iconPath;
    private String newName;     // for update
    private String newIconPath; // for update
}