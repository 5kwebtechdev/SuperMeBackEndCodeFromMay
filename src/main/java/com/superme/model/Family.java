package com.superme.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDate;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonBackReference;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Family {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String familyCode; // Unique family code (used for QR/invite)

    @Column(nullable = false)
    private String familyName;

    @OneToMany(mappedBy = "family", cascade = CascadeType.ALL)
    @JsonManagedReference
    private List<User> members;

    // Relationship-based family management (children, partner, etc.)
    @OneToMany(mappedBy = "family", cascade = CascadeType.ALL)
    @JsonManagedReference("family-member")
    private List<FamilyMember> familyMembers;

    @Transient
    private String joinToken;

    @Column(nullable = true)
    private Long createdBy; // User ID of the creator

    @Column(nullable = false)
    private LocalDate createdAt;

    // Lombok will generate constructors, getters, setters, builder

    // Utility method to auto-generate family name based on main user's name
    public static String generateFamilyName(String mainUserName) {
        if (mainUserName == null || mainUserName.isBlank()) {
            return "Family";
        }
        return mainUserName.trim() + "'s Family";
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDate.now();
    }
}