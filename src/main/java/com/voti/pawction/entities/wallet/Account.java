package com.voti.pawction.entities.wallet;

import com.voti.pawction.entities.auction.Auction;
import com.voti.pawction.entities.wallet.enums.Status;
import com.voti.pawction.entities.wallet.enums.Transaction_Type;
import com.voti.pawction.exceptions.AccountExceptions.InvalidAmountException;
import jakarta.persistence.*;
import lombok.*;
import com.voti.pawction.entities.User;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
@Entity
@Table(name = "account")

public class Account {
    @Id
    @Column(name = "account_id")
    private Long accountId;

    @Column(name="balance", nullable = false)
    private BigDecimal balance=BigDecimal.ZERO;

    @Column(name="created_at", nullable = false)
    private LocalDateTime createdAt;

    //Account to Transaction Relation
    @Builder.Default
    @OneToMany(mappedBy = "account", cascade = {CascadeType.PERSIST,
            CascadeType.REMOVE}, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<Transaction> transactions = new ArrayList<>();

    //Account to DepositHold Relation
    @Builder.Default
    @OneToMany(mappedBy = "account", cascade ={CascadeType.PERSIST,
            CascadeType.REMOVE}, fetch = FetchType.LAZY, orphanRemoval = true)
    @ToString.Exclude
    private List<DepositHold> holds = new ArrayList<>();

    //Account to User Relation
    @MapsId
    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "account_id")
    @ToString.Exclude
    private User user;

    public Transaction deposit(BigDecimal amount) {
        balance = balance.add(amount);
        return addTransaction(Transaction_Type.DEPOSIT, amount);
    }

    public Transaction withdraw(BigDecimal amount) {
        balance = balance.subtract(amount);
        return  addTransaction(Transaction_Type.WITHDRAWAL, amount);
    }

    //Helper methods
    public DepositHold addHold(Auction auction, BigDecimal amount) {
        if (auction == null) throw new IllegalArgumentException("auction is required");
        if (amount == null || amount.signum() <= 0) throw new IllegalArgumentException("amount must be positive");

        DepositHold hold = new DepositHold();
        hold.setAmount(amount);
        hold.setDepositStatus(Status.HELD);
        hold.setCreatedAt(LocalDateTime.now());
        hold.setUpdatedAt(LocalDateTime.now());
        hold.setAuction(auction);
        hold.setAccount(this);
        holds.add(hold);

        return hold;
    }

    public Transaction addTransaction(Transaction_Type type, BigDecimal amount) {
        Objects.requireNonNull(amount, "amount");
        if (amount.signum() <= 0) throw new InvalidAmountException("Amount must be larger than 0");

        Transaction tx = new Transaction();
        tx.setTransactionType(type);
        tx.setAmount(amount.setScale(2, RoundingMode.HALF_UP));
        tx.setCreatedAt(LocalDateTime.now());
        tx.setAccount(this);
        transactions.add(tx);
        return tx;
    }
}
