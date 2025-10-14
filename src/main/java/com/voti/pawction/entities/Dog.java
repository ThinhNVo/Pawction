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
@PrimaryKeyJoinColumn(name = "pet_id")
@Table(name = "dogs")

public class Dog extends Pets{


    private String temperament;

    private boolean isHypoallergenic;

    @Enumerated(EnumType.STRING)
    @Column(name = "size", nullable = false)
    private Dog.Category Size;

    public enum Category {
        BIG,
        SMALL,
    }

}
