create table account
(
    account_id bigint auto_increment
        primary key,
    balance    decimal(19, 4) default 0.0000            not null invisible,
    created_at datetime       default CURRENT_TIMESTAMP not null,
    constraint account_user_user_id_fk
        foreign key (account_id) references user (user_id)
);

