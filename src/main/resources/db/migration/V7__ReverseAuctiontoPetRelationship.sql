ALTER TABLE pet
DROP FOREIGN KEY pet_auction_auction_id_fk;

ALTER TABLE auction
    MODIFY pet_id BIGINT NOT NULL,
    ADD CONSTRAINT uq_auction_pet UNIQUE (pet_id);

ALTER TABLE auction
    ADD CONSTRAINT fk_auction_pet
        FOREIGN KEY (pet_id) REFERENCES pet(pet_id)
            ON DELETE RESTRICT;