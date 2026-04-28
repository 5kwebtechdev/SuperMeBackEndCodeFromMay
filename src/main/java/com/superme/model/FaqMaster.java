package com.superme.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "faq_master")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FaqMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String section;

    private String screen;

    @Column(columnDefinition = "TEXT")
    private String question;

    @Column(columnDefinition = "TEXT")
    private String answer;
}

