package com.superme.service;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AvatarPetService {

    private static final String S3_BASE_URL = "https://your-bucket.s3.amazonaws.com/";

    private final List<Map<String, String>> avatars = new ArrayList<>();
    private final List<Map<String, String>> pets = new ArrayList<>();

    public AvatarPetService() {
        // Initialize with default avatars
        avatars.add(icon("avatar1", "avatars/avatar1.png"));
        avatars.add(icon("avatar2", "avatars/avatar2.png"));
        avatars.add(icon("avatar3", "avatars/avatar3.png"));
        avatars.add(icon("avatar4", "avatars/avatar4.png"));

        // Initialize with default pets
        pets.add(icon("pet1", "pets/pet1.png"));
        pets.add(icon("pet2", "pets/pet2.png"));
        pets.add(icon("pet3", "pets/pet3.png"));
        pets.add(icon("pet4", "pets/pet4.png"));
    }

    public List<Map<String, String>> getAvatars() {
        return avatars;
    }

    public List<Map<String, String>> getPets() {
        return pets;
    }

    public boolean addAvatar(String name, String iconPath) {
        if (avatars.stream().anyMatch(a -> a.get("name").equals(name))) {
            return false;
        }
        avatars.add(icon(name, iconPath));
        return true;
    }

    public boolean updateAvatar(String name, String newName, String newIconPath) {
        for (Map<String, String> avatar : avatars) {
            if (avatar.get("name").equals(name)) {
                avatar.put("name", newName != null ? newName : name);
                avatar.put("imageUrl", S3_BASE_URL + (newIconPath != null ? newIconPath : avatar.get("imageUrl").replace(S3_BASE_URL, "")));
                return true;
            }
        }
        return false;
    }

    public boolean deleteAvatar(String name) {
        return avatars.removeIf(a -> a.get("name").equals(name));
    }

    public boolean addPet(String name, String iconPath) {
        if (pets.stream().anyMatch(p -> p.get("name").equals(name))) {
            return false;
        }
        pets.add(icon(name, iconPath));
        return true;
    }

    public boolean updatePet(String name, String newName, String newIconPath) {
        for (Map<String, String> pet : pets) {
            if (pet.get("name").equals(name)) {
                pet.put("name", newName != null ? newName : name);
                pet.put("imageUrl", S3_BASE_URL + (newIconPath != null ? newIconPath : pet.get("imageUrl").replace(S3_BASE_URL, "")));
                return true;
            }
        }
        return false;
    }

    public boolean deletePet(String name) {
        return pets.removeIf(p -> p.get("name").equals(name));
    }

    private Map<String, String> icon(String name, String path) {
        return new HashMap<>(Map.of(
                "name", name,
                "imageUrl", S3_BASE_URL + path
        ));
    }
}
