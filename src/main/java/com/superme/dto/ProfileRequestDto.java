package com.superme.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class ProfileRequestDto {
    Integer id;
    private String fullName;
    private String petName;
    private LocalDate dob;
    private String gender;
    private String avatar;
}