-- Reset Schema (WARNING: Deletes all data)
drop table if exists transactions;
drop table if exists menu_items;

-- Enable UUID extension
create extension if not exists "uuid-ossp";

-- Menu Items Table
create table menu_items (
  id uuid primary key default uuid_generate_v4(),
  name text not null,
  category text not null,
  price double precision not null,
  image_url text,
  tax_rate double precision default 0.1,
  is_available boolean default true
);

-- Transactions Table
create table transactions (
  id uuid primary key default uuid_generate_v4(),
  timestamp bigint not null,
  total_amount double precision not null,
  tax_amount double precision not null,
  items_json text not null,
  status text not null,
  payment_method text not null,
  square_transaction_id text,
  is_synced boolean default true
);

-- RLS Policies (Example: Allow public read for menu, authenticated insert for transactions)
alter table menu_items enable row level security;
alter table transactions enable row level security;

create policy "Public Menu Read" on menu_items for select using (true);
create policy "Authenticated Transaction Insert" on transactions for insert with check (auth.role() = 'anon');
