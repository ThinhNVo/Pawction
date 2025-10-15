# create table transaction
# (
#     tx_id       BIGINT auto_increment
#         primary key,
#     account_id  BIGINT                                       not null,
#     tx_type     ENUM ('DEPOSIT', 'WITHDRAWAL', 'SETTLEMENT') not null,
#     amount      decimal(19, 4)                               not null,
#     occurred_at DATETIME default CURRENT_TIMESTAMP           not null,
#     constraint transaction_account_account_id_fk
#         foreign key (account_id) references account (account_id)
# );

