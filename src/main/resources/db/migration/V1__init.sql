create table baskets (
    id         varchar(36)  primary key,
    user_id    varchar(255) not null,
    name       varchar(255) not null,
    created_at timestamp    not null default now(),
    unique (user_id, name)
);

create table basket_items (
    id           varchar(36)  primary key,
    basket_id    varchar(36)  not null references baskets (id) on delete cascade,
    product_ref  varchar(512) not null,
    product_name varchar(512) not null,
    quantity     integer      not null
);

-- Phase 3 tables (created now, used later)
create table substitutions (
    id                varchar(36)  primary key,
    user_id           varchar(255) not null,
    preferred_product varchar(512) not null,
    fallback_product  varchar(512) not null,
    priority          integer      not null
);

create table purchase_insights (
    id                    varchar(36)  primary key,
    user_id               varchar(255) not null,
    product_ref           varchar(512) not null,
    avg_purchase_gap_days integer,
    last_purchase_date    timestamp,
    purchase_count        integer,
    confidence_score      double precision
);
