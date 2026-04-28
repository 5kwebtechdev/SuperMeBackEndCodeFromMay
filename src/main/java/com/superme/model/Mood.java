package com.superme.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "mood")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Mood {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String value;

    @Column(name = "date", nullable = false)
    private LocalDate date;
    @Column(name = "time", nullable = false)
    private LocalTime time;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "calendar_event_id")
    private CalendarEvent calendarEvent;

    @Column(name = "created_date", nullable = false)
    private LocalDate createdDate;
    @Column(name = "created_time", nullable = false)
    private LocalTime createdTime;
    // Lombok will generate constructors, getters, setters, builder
}