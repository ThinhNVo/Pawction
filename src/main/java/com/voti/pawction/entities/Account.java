package com.voti.pawction.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
public class Account {
    @Id
    private Long id;
    @Column(name="balance", nullable = false)
    private BigDecimal balance;
    @Column(name="created_at", nullable = false)
    private LocalDateTime createdAt;



}
