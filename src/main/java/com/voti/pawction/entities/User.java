package com.voti.pawction.entities;

import com.voti.pawction.entities.pet.Pet;
import com.voti.pawction.entities.wallet.Account;
import com.voti.pawction.entities.auction.Bid;
import com.voti.pawction.entities.auction.Auction;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
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
    private Long userId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    //User to Account relation
    @OneToOne(mappedBy = "user", cascade = CascadeType.REMOVE, fetch = FetchType.EAGER)
    private Account account;

    //User to Auction relation
    //@OneToMany(mappedBy = "selling_user", cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, orphanRemoval = true)
    @Builder.Default
    @OneToMany(mappedBy = "sellingUser", cascade = {CascadeType.PERSIST,
            CascadeType.REMOVE}, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Auction> auctions = new ArrayList<>();

    public void addAuction(Auction auction) {
        auctions.add(auction);
        auction.setSellingUser(this);
    }

    //User to Bid relation
    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = {CascadeType.PERSIST,
            CascadeType.REMOVE}, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<Bid> bids = new ArrayList<>();

    public void addBid(Auction auction, Bid bid) {
        bids.add(bid);
        bid.setUser(this);
        auction.addBid(bid);
    }


    // don't test
    public void setWinningBid(Auction auction, Bid bid) {
        auction.setWinningUser(this);
        auction.setHighestBid(bid.getAmount());
    }
}
