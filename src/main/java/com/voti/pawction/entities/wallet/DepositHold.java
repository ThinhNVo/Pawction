package com.voti.pawction.entities.wallet;

import com.voti.pawction.entities.wallet.enums.Status;
import jakarta.persistence.*;
import com.voti.pawction.entities.wallet.Account;
import com.voti.pawction.entities.auction.Auction;
import lombok.*;

import java.time.LocalDateTime;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
@Entity
@Table(name = "deposit_hold")
public class DepositHold {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int holdId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status depositStatus;

    @Column(name = "amount", nullable = false)
    private double amount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    //DepositHold to Account Relationship
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    //DepositHold to Auction Relationship
    @Setter
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "auction_id", nullable = false)
    private Auction auction;

}