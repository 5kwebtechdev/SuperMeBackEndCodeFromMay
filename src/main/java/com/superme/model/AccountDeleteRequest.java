package com.superme.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "account_delete_request")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class AccountDeleteRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String email;

    @Column(length = 500)
    private String reason;

    @Column(nullable = false)
    private String status; // PENDING, COMPLETED, REJECTED

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime processedAt;
}