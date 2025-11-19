alter table pet
    add user_id bigint not null after pet_id;

alter table pet
    add constraint pet_user_user_id_fk
        foreign key (user_id) references user (user_id);

