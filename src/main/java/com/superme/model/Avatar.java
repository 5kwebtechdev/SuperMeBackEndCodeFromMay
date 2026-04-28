package com.superme.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
public class Avatar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 12)
    private String avatarName;

    @Column
    private String gender;

    private String url;

    private String avatarImageName;

    @Column(nullable = false)
    private boolean renamedByUser = false;
}
