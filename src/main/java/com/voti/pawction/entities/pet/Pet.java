package com.voti.pawction.entities.pet;

import com.voti.pawction.entities.pet.enums.*;
import jakarta.persistence.*;
import lombok.*;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
@Entity
@Table(name = "pet")
public class Pet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long petId;

    @Column(name = "pet_name", nullable = false)
    private String petName;

    @Column(name = "age_months", nullable = false)
    private int petAgeMonths;

    @Enumerated(EnumType.STRING)
    @Column(name = "sex", nullable = false)
    private Sex petSex;

    @Column(name = "weight_kg", nullable = false)
    private Double petWeight;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private Category petCategory;

    @Column(name = "dog_breed", nullable = true)
    public String dogBreed;

    @Enumerated(EnumType.STRING)
    @Column(name = "dog_size", nullable = true)
    public Size dogSize;

    @Column(name = "dog_temperature", nullable = true)
    public String dogTemperature;

    @Enumerated(EnumType.STRING)
    @Column(name = " dog_is_hypoallergenic", nullable = true)
    public Allergy dogIsHypoallergenic;

    @Column(name = "cat_breed", nullable = true)
    public String catBreed;

    @Column(name = "cat_coat_length", nullable = true)
    public Coat_Length catCoatLength;

    @Enumerated(EnumType.STRING)
    @Column(name = "cat_indoor_only", nullable = true)
    public Indoor CatIndoorOnly;

    @Column(name = "primary_photo_url", nullable = false)
    public String primaryPhotoUrl;

}
