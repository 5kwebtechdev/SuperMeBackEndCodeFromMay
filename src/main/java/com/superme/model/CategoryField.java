package com.superme.model;



import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.superme.enums.FieldType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "category_fields",
        uniqueConstraints = @UniqueConstraint(columnNames = {"category_id", "field_key"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryField {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonBackReference
    @JoinColumn(name = "category_id", nullable = false)
    private TutorCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "field_type",nullable = false)
    private FieldType fieldType;

    @Column(name = "field_key",nullable = false, length = 100)
    private String fieldKey;

    @Column(name = "field_label",nullable = false, length = 100)
    private String fieldLabel;

    @Column(name = "is_required")
    private Boolean isRequired = false;

    @Column(name = "is_filterable")
    private Boolean isFilterable = false;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(name = "validation_rule",columnDefinition = "JSON")
    private String validationRules;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "field", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<FieldOption> options;
}

