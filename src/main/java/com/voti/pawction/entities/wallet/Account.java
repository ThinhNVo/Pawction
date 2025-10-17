package com.voti.pawction.entities.wallet;

import com.voti.pawction.entities.auction.Auction;
import com.voti.pawction.entities.pet.Pet;
import com.voti.pawction.entities.wallet.enums.Status;
import com.voti.pawction.entities.wallet.enums.Transaction_Type;
import jakarta.persistence.*;
import lombok.*;
import com.voti.pawction.entities.User;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.voti.pawction.entities.wallet.Transaction;
import com.voti.pawction.entities.wallet.DepositHold;

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
    private int id;

    @Column(name="balance", nullable = false)
    private BigDecimal balance;

    @Column(name="created_at", nullable = false)
    private LocalDateTime createdAt;

    //Account to Transaction Relation
    @OneToMany(mappedBy = "account", cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, orphanRemoval = true)
    private List<Transaction> transactions = new ArrayList<>();

    //Account to DepositHold Relation
    @OneToMany(mappedBy = "account")
    private List<DepositHold> holds = new ArrayList<>();
    public DepositHold addHold(Auction auction, Double amount) {
        DepositHold hold = new DepositHold();
        hold.setAuction(auction);
        hold.setAmount(amount);
        hold.setDepositStatus(Status.HELD);
        holds.add(hold);
        hold.setAccount(this);
        return hold;
    }

    //Account to User Relation
    @MapsId
    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "account_id")
    @ToString.Exclude
    private User user;


    //Transaction helper method Test Please
    public Transaction addTransaction(Transaction_Type type, double amount) {
        Transaction tx = new Transaction();
        tx.setTransactionType(type);
        tx.setAmount(amount);
        tx.setCreatedAt(LocalDateTime.now());
        tx.setAccount(this);
        transactions.add(tx);
        return tx;
    }
}
