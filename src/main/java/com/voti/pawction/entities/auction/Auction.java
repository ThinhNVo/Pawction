package com.voti.pawction.entities.auction;

import com.voti.pawction.entities.auction.enums.Auction_Status;
import com.voti.pawction.entities.wallet.Account;
import com.voti.pawction.entities.wallet.DepositHold;
import com.voti.pawction.entities.User;
import com.voti.pawction.entities.pet.Pet;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
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

    @Column(name = "pet_id", nullable = false)
    private int petId;

    @Column(name = "seller_user_id", nullable = false)
    private int sellerUserId;

    @Column(name = "winner_user_id")
    private int winnerUserId;

    @Column(name = "start_price", nullable = false)
    private Double startPrice;

    @Column(name = "highest_bid", nullable = false)
    private Double highestBid;

    @Column(name = "status", nullable = false)
    private Auction_Status status;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    //One Auction has many deposite holds
    @OneToMany(mappedBy = "auction")
    private List<DepositHold> depositHold;

    //One pet to one auction
    @OneToOne(mappedBy = "auction")
    private Pet pet;

    @OneToMany(mappedBy = "auction")
    private List<Bid> bid;

    //May auctions made by one user
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}


