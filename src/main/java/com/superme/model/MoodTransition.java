package com.superme.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class MoodTransition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "mood_from", nullable = false)
    private String moodFrom;

    @Column(name = "mood_to", nullable = false)
    private String moodTo;

    @Column(name = "title")
    private String title;

    @Column(name = "first_line")
    private String firstLine;

    @Column(name = "second_line")
    private String secondLine;

    @Column(name = "parent_version")
    private Boolean parentVersion;
}

