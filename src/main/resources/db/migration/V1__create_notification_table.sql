create table ntf_tx (
    id bigint primary key,
    tp varchar(32) not null,
    tgt varchar(255) not null,
    pld clob,
    st varchar(1) not null,
    fc integer not null,
    dc integer not null,
    bl varchar(1) not null,
    lm varchar(255),
    crt timestamp not null,
    upd timestamp not null
);

comment on table ntf_tx is 'Notification transaction log';
comment on column ntf_tx.id is 'Primary key for the notification transaction';
comment on column ntf_tx.tp is 'Notification type';
comment on column ntf_tx.tgt is 'Target destination for the notification';
comment on column ntf_tx.pld is 'Notification payload content';
comment on column ntf_tx.st is 'Current notification state (N=NEW, S=SENT, E=ERR, U=UNKNOWN, R=REJECTED)';
comment on column ntf_tx.fc is 'Failure counter';
comment on column ntf_tx.dc is 'Dispatch counter';
comment on column ntf_tx.bl is 'Whether the notification is blacklisted or blocked (Y/N)';
comment on column ntf_tx.lm is 'Last message or error detail';
comment on column ntf_tx.crt is 'Created timestamp';
comment on column ntf_tx.upd is 'Last updated timestamp';

create index ix_ntf_tx_tgt on ntf_tx (tgt);
create index ix_ntf_tx_st on ntf_tx (st);
