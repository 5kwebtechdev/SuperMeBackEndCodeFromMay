package com.superme.dto;



import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddFamilyMember {

    private Long mainUserId;   // ✅ parent/main user
    private String name;
    private String gender;
    private String dateOfBirth; // ✅ send as String (YYYY-MM-DD)
    private String relationship; // ✅ ADD THIS (PARENT / CHILD)

}