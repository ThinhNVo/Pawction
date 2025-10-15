create table pet
(
    pet_id                   bigint auto_increment
        primary key,
    pet_name                 varchar(120)                             not null,
    age_months               int                                      not null,
    sex                      enum ('M', 'F')                          not null,
    weight_kg                decimal(8, 3)                            not null,
    category                 enum ('Dog', 'Cat')                      not null,
    dog_breed                varchar(120)                             null,
    dog_size                 enum ('TOY', 'SMALL', 'MEDIUM', 'LARGE') null,
    dog_temperature          varchar(80)                              null,
    ` dog_is_hypoallergenic` enum ('YES', 'NO', 'UNKNOWN')            null,
    cat_breed                varchar(120)                             null,
    cat_coat_length          enum ('SHORT', 'MEDIUM', 'LONG')         null,
    cat_indoor_only          enum ('YES', 'NO')                       null,
    primary_photo_url        text                                     null,
    constraint pet_auction_auction_id_fk
        foreign key (pet_id) references auction (auction_id)
);

create table bid
(
    bid_id     bigint auto_increment
        primary key,
    auction_id bigint                                   not null,
    user_id    bigint                                   not null,
    amount     decimal(19, 4) default 0.0000            not null,
    status     enum ('OUTBID', 'WINNING', 'WON')        not null,
    bid_time   datetime       default CURRENT_TIMESTAMP not null,
    constraint bid_auction_auction_id_fk
        foreign key (auction_id) references auction (auction_id),
    constraint bid_user_user_id_fk
        foreign key (user_id) references user (user_id)
# );
#
# create table deposit_hold
# (
#     hold_id    bigint auto_increment
#         primary key,
#     account_id bigint                                            not null,
#     auction_id bigint                                            not null,
#     status     enum ('HELD', 'APPLIED', 'RELEASED', 'FORFEITED') not null,
#     amount     decimal(19, 4) default 0.0000                     not null,
#     created_at datetime       default CURRENT_TIMESTAMP          not null,
#     updated_at datetime       default CURRENT_TIMESTAMP          not null,
#     constraint uq_deposit_hold_pk
#         unique (account_id, auction_id),
#     constraint deposit_hold_account_account_id_fk
#         foreign key (account_id) references account (account_id),
#     constraint deposit_hold_auction_auction_id_fk
#         foreign key (auction_id) references auction (auction_id)
# );


