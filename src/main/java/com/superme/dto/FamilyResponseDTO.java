// Java
package com.superme.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FamilyResponseDTO {
    private Long id;
    private String familyCode;
    private String familyName;
    private List<MemberDTO> members;

    @Data
    @NoArgsConstructor
    public static class MemberDTO {
        private Long id;
        private String name;
        private String relationship;
        private java.time.LocalDate dateOfBirth;

        // Constructor with LocalDate
        public MemberDTO(Long id, String name, String relationship, java.time.LocalDate dateOfBirth) {
            this.id = id;
            this.name = name;
            this.relationship = relationship;
            this.dateOfBirth = dateOfBirth;
        }
    }
}