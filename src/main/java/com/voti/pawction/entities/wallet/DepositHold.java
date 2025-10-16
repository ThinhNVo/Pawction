package com.voti.pawction.entities.wallet;

import com.voti.pawction.entities.wallet.enums.Status;
import jakarta.persistence.*;
import lombok.*;
import com.voti.pawction.entities.wallet.Account;
import com.voti.pawction.entities.auction.Auction;
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

    @Column(name = "account_id", nullable = false)
    public int accountId;

    @Column(name = "auction_id", nullable = false)
    private int auctionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status depositStatus;

    @Column(name = "amount", nullable = false)
    private double amount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    //Many DepHolds to one account
    @ManyToOne
    @JoinColumn(name = "account_id")
    private Account account;

    //Many DepHolds to one auction
    @ManyToOne
    @JoinColumn(name = "auction_id")
    private Auction auction;


}