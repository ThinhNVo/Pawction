package com.voti.pawction.entities;

import jakarta.persistence.*;
import lombok.*;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "pets")
public class Pets {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long petId;

    @Column(name = "pet_name", nullable = false)
    private String petName;

    @Column(name = "pet_breed", nullable = false)
    private String petBreed;

    @Column(name = "pet_age_months", nullable = false)
    private int petAgeMonths;

    @Column(name = "pet_sex", nullable = false)
    private String petSex;

    @Column(name = "pet_weight", nullable = false)
    private Double petWeight;

    @Enumerated(EnumType.STRING)
    @Column(name = "pet_category", nullable = false)
    private Category petCategory;

    public enum Category {
        DOG,
        CAT,
    }
}
