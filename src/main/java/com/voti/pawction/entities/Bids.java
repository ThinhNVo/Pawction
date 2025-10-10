package com.voti.pawction.entities;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class Bids {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    private Long bidId;

    @Column(name="bid_Time", nullable = false)
    private LocalDateTime bidTime;






}
