package com.superme.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "field_options",
        uniqueConstraints = @UniqueConstraint(columnNames = {"field_id", "option_value"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FieldOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonBackReference
    @JoinColumn(name = "field_id", nullable = false)
    private CategoryField field;

    @Column(name = "option_value",nullable = false, length = 100)
    private String optionValue;

    @Column(name = "option_label",nullable = false, length = 100)
    private String optionLabel;

    @Column(name = "display_order")
    private Integer displayOrder = 0;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
