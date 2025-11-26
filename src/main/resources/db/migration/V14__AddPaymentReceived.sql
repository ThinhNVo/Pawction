alter table auction
    add payment_received enum ('PAID', 'UNPAID') not null;

