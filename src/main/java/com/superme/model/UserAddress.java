package com.superme.model;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_addresses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // link to your authenticated user; adjust type if needed
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String receiverName;

    @Column(nullable = false)
    private String mobile;

    @Column(nullable = false)
    private String line1;

    private String line2;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String state;

    @Column(nullable = false)
    private String postalCode;

    private Double latitude;
    private Double longitude;

    @Column(name = "is_default")
    private Boolean isDefault;
}
