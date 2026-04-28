package com.superme.dto;

import com.superme.model.User;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class LoginResponse {

    private String token;
    private UserDTO user;

}
