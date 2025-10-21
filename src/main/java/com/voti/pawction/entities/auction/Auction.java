package com.voti.pawction.entities.auction;

import com.voti.pawction.entities.auction.enums.Auction_Status;
import com.voti.pawction.entities.wallet.Account;
import com.voti.pawction.entities.wallet.DepositHold;
import com.voti.pawction.entities.User;
import com.voti.pawction.entities.pet.Pet;

import jakarta.persistence.*;
import lombok.*;
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
@Table(name="auction")
public class Auction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int auctionId;

    @Column(name = "start_price", nullable = false)
    private Double startPrice;

    @Column(name = "highest_bid", nullable = false)
    private Double highestBid;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Auction_Status status;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    //Auction to Pet Relation
    @OneToOne(mappedBy = "auction", cascade = CascadeType.REMOVE)
    private Pet pet;

    //Auction to DepositHold Relation
    @OneToMany(mappedBy = "auction")
    private List<DepositHold> depositHolds = new ArrayList<>();
    public void addDepositHold(DepositHold depositHold) {
        depositHolds.add(depositHold);
        depositHold.setAuction(this);
    }

    //Auction to Bid Relation
    @OneToMany(mappedBy = "auction", cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, orphanRemoval = true)
    private List<Bid> bids = new ArrayList<>();
    public void addBid(Bid bid) {
        bids.add(bid);
        bid.setAuction(this);
    }

    //Auction to Winning User relation
    @ManyToOne
    @JoinColumn(name = "winner_user_id")
    @ToString.Exclude
    private User winningUser;

    //Auction to Selling User relation
    @ManyToOne
    @JoinColumn(name = "seller_user_id")
    @ToString.Exclude
    private User sellingUser;
}


