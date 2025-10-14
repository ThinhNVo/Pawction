package com.voti.pawction.entities;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
@Entity
@Table(name = "deposit_holds")
public class DepositHold {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int holdId;

    @Column(name = "auction_id", nullable = false)
    private int auctionId;

    //This may need to be changed to better represent money value
    @Column(name = "amount", nullable = false)
    private int amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "deposit_status", nullable = false)
    private DepositStatus depositStatus;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum DepositStatus {
        HELD,
        APPLIED,
        FORFEITED,
        RELEASED
    }
}