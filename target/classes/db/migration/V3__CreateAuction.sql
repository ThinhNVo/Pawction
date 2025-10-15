# create table auction
# (
#     auction_id     bigint auto_increment
#         primary key,
#     pet_id         bigint                                        not null,
#     seller_user_id bigint                                        not null,
#     winner_user_id bigint                                        null,
#     start_price    decimal(19, 4) default 0.0000                 not null,
#     highest_bid    decimal(19, 4) default 0.0000                 not null,
#     status         enum ('LIVE', 'ENDED', 'CANCELED', 'SETTLED') not null,
#     end_time       datetime                                      not null,
#     created_at     datetime       default CURRENT_TIMESTAMP      not null,
#     updated_at     datetime       default CURRENT_TIMESTAMP      not null on update CURRENT_TIMESTAMP,
#     constraint auction_seller_id_fk
#         foreign key (seller_user_id) references user (user_id),
#     constraint auction_user_user_id_fk
#         foreign key (winner_user_id) references user (user_id)
# );

