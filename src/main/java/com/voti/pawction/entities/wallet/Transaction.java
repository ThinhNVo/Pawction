package com.voti.pawction.entities.wallet;

import com.voti.pawction.entities.wallet.enums.Transaction_Type;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import com.voti.pawction.entities.wallet.Account;


@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
@Entity
@Table(name = "transaction")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int transactionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tx_type", nullable = false)
    private Transaction_Type transactionType;

    @Column(nullable = false)
    private Double amount;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime createdAt;

    //Transaction to Account Relation
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    @ToString.Exclude
    private Account account;


}