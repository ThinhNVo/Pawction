create table user
(
    user_id       bigint auto_increment
        primary key,
    name          varchar(120) not null,
    email         varchar(255) not null,
    password_hash varchar(255) not null
);

