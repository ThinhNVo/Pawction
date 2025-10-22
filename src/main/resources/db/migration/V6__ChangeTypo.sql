alter table pet
    change ` dog_is_hypoallergenic` dog_is_hypoallergenic enum ('YES', 'NO', 'UNKNOWN') null;


alter table pet
    change dog_temperature dog_temperament varchar(80) null;

