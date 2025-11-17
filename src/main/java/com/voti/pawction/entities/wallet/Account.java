package com.voti.pawction.entities.wallet;

import com.voti.pawction.entities.auction.Auction;
import com.voti.pawction.entities.wallet.enums.Status;
import com.voti.pawction.entities.wallet.enums.Transaction_Type;
import jakarta.persistence.*;
import lombok.*;
import com.voti.pawction.entities.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long accountId;

    @Column(name="balance", nullable = false)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name="created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder.Default
    @OneToMany(mappedBy = "account", cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<Transaction> transactions = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "account", cascade ={CascadeType.PERSIST, CascadeType.REMOVE}, fetch = FetchType.LAZY, orphanRemoval = true)
    @ToString.Exclude
    private List<DepositHold> holds = new ArrayList<>();

    @MapsId
    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "account_id")
    @ToString.Exclude
    private User user;

    // ---- Money movements ----
    public Transaction deposit(BigDecimal amount) {
        balance = balance.add(amount);
        return addTransaction(Transaction_Type.DEPOSIT, amount);
    }

    public Transaction withdraw(BigDecimal amount) {
        if (!canAfford(amount)) {
            throw new IllegalArgumentException("Insufficient available funds");
        }
        balance = balance.subtract(amount);
        return addTransaction(Transaction_Type.WITHDRAWAL, amount);
    }

    public boolean canAfford(BigDecimal amount) {
        BigDecimal held = holds.stream()
                .filter(h -> h.getDepositStatus() == Status.HELD)
                .map(DepositHold::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return balance.subtract(held).compareTo(amount) >= 0;
    }

    public DepositHold addHold(Auction auction, BigDecimal amount) {
        if (!canAfford(amount)) throw new IllegalArgumentException("Insufficient funds to place hold");
        DepositHold hold = new DepositHold();
        hold.setAccount(this);
        auction.addDepositHold(hold);
        hold.setAmount(amount);
        hold.setDepositStatus(Status.HELD);
        hold.setCreatedAt(LocalDateTime.now());
        hold.setUpdatedAt(LocalDateTime.now());
        holds.add(hold);
        return hold;
    }

    public Transaction addTransaction(Transaction_Type type, BigDecimal amount) {
        Transaction tx = new Transaction();
        tx.setTransactionType(type);
        tx.setAmount(amount);
        tx.setCreatedAt(LocalDateTime.now());
        tx.setAccount(this);
        transactions.add(tx);
        return tx;
    }
}
