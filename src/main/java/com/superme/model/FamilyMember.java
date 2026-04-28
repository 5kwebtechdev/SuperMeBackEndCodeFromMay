
package com.superme.model;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.superme.enums.Relationship;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FamilyMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JsonBackReference("family-member")
    private Family family;

    @ManyToOne(optional = false)
    private User user;

    @Column(nullable = false)
    private LocalDate dateOfBirth; // LocalDate instead of epoch millis

    // Lombok will generate constructors, getters, setters, builder
    public Long getMemberId() {
        return id;
    }

    // Utility methods for relationship checks (now use User's relationship)
    public boolean isChild() {
        return user != null && user.getRelationship() == Relationship.CHILD;
    }

    public boolean isParent() {
        return user != null && user.getRelationship() == Relationship.PARENT;
    }

    public boolean isSelf() {
        return user != null && user.getRelationship() == Relationship.SELF;
    }

    // For compatibility, if you need to get relationship as String
    public String getRelationship() {
        return user != null && user.getRelationship() != null
                ? user.getRelationship().name().toLowerCase()
                : null;
    }
}
