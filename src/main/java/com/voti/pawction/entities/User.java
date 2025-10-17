package com.voti.pawction.entities;

import com.voti.pawction.entities.wallet.Account;
import com.voti.pawction.entities.auction.Bid;
import com.voti.pawction.entities.auction.Auction;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
@Entity
@Table(name="user")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "name", nullable = false)
    private String name;
    @Column(name = "email", nullable = false)
    private String email;
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;


    //One Account per User
    @OneToOne
    @JoinColumn(name = "account_id")
    private Account account;

    //Many auctions created
    @OneToMany(mappedBy = "user")
    private List<Auction> auctions;

    @ManyToMany
    @JoinTable(
            name = "user_bids",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "bid_id")
    )
    private List<Bid> bids;

}
