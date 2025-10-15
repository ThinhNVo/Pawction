package com.voti.pawction.entities.wallet;

import com.voti.pawction.entities.wallet.enums.Transaction_Type;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

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

    @Column(name = "account_id", nullable = false)
    private int accountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tx_type", nullable = false)
    private Transaction_Type transactionType;

    @Column(nullable = false)
    private Double amount;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime createdAt;





}