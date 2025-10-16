package com.voti.pawction.entities.wallet;

import com.voti.pawction.entities.pet.Pet;
import jakarta.persistence.*;
import lombok.*;
import com.voti.pawction.entities.User;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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

    //Many transactions
    @OneToMany(mappedBy = "account")
    private List<Transaction> transaction;

    //Many holds
    @OneToMany(mappedBy = "account")
    private List<DepositHold> depositHold;

    //One user to one account
    @OneToOne(mappedBy = "account")
    private User user;
}
