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
@Table(name = "cats")

public class Cat extends Pets{

    private boolean indoorOnly;

    @Enumerated(EnumType.STRING)
    @Column(name = "coatLength", nullable = false)
    private Cat.Category coatLength;

    public enum Category {
        LONG,
        SHORT,
    }

}
