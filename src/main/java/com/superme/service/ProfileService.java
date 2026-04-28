package com.superme.service;

import com.superme.dto.ProfileRequestDto;
import com.superme.exception.BusinessException;
import com.superme.exception.ResourceNotFoundException;
import com.superme.model.Profile;
import com.superme.model.User;
import com.superme.repository.ProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ProfileService {

    @Autowired
    private ProfileRepository profileRepository;

    /**
     * Create a child profile.
     */
    public Profile createProfile(ProfileRequestDto dto, User user) {

        Profile profile = new Profile(); // ✅ create object

        profile.setFullName(dto.getFullName());
        profile.setPetName(dto.getPetName());
        profile.setDob(dto.getDob());
        profile.setGender(dto.getGender());
        profile.setAvatar(dto.getAvatar());

        profile.setUser(user); // ✅ attach user

        return profileRepository.save(profile);
    }
    /**
     * Get the child profile for a user.
     */
    public Optional<Profile> getProfile(User user) {
        return profileRepository.findByUser(user);
    }

    /**
     * Update the child profile.
     */
    public Profile updateProfile(ProfileRequestDto profileRequestDto, User user) {
        Optional<Profile> existingProfileOptional = profileRepository.findByUser(user);

        if (existingProfileOptional.isPresent()) {
            Profile existingProfile = existingProfileOptional.get();
            existingProfile.setFullName(profileRequestDto.getFullName());
            existingProfile.setPetName(profileRequestDto.getPetName());
            existingProfile.setDob(profileRequestDto.getDob());
            existingProfile.setGender(profileRequestDto.getGender());
            existingProfile.setAvatar(profileRequestDto.getAvatar());
            return profileRepository.save(existingProfile);
        } else {
            throw new ResourceNotFoundException("Profile not found for the user.");
        }
    }
}