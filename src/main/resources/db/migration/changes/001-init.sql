--liquibase formatted sql

--changeset core:001-init context:dev,prod runInTransaction:true failOnError:true

CREATE TABLE IF NOT EXISTS users (
                                     id              BIGSERIAL PRIMARY KEY,
                                     email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    full_name       VARCHAR(255),
    enabled         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
    );

-- Роли и связь многие-ко-многим
CREATE TABLE IF NOT EXISTS roles (
                                     id   SMALLSERIAL PRIMARY KEY,
                                     name VARCHAR(50) NOT NULL UNIQUE  -- 'ADMIN', 'USER'
    );

CREATE TABLE IF NOT EXISTS user_roles (
                                          user_id BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id SMALLINT    NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
    );

-- Карты
-- pan_enc: зашифрованный PAN (AES-GCM)
-- pan_fp: отпечаток PAN (HMAC-SHA256) для дедупликации
-- pan_last4: последние 4 цифры для маскировки
-- status: 'ACTIVE' | 'BLOCKED' | 'EXPIRED'
CREATE TABLE IF NOT EXISTS cards (
                                     id              BIGSERIAL PRIMARY KEY,
                                     user_id         BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    pan_enc         BYTEA        NOT NULL,
    pan_fp          BYTEA        NOT NULL,
    pan_last4       VARCHAR(4)      NOT NULL,
    exp_month       SMALLINT     NOT NULL CHECK (exp_month BETWEEN 1 AND 12),
    exp_year        SMALLINT     NOT NULL CHECK (exp_year BETWEEN 2000 AND 2100),
    status          VARCHAR(16)  NOT NULL CHECK (status IN ('ACTIVE','BLOCKED','EXPIRED')),
    balance         NUMERIC(19,2) NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_pan_last4_digits CHECK (pan_last4 ~ '^[0-9]{4}$')
    );

-- Индексы по картам
CREATE UNIQUE INDEX IF NOT EXISTS ux_cards_pan_fp     ON cards(pan_fp);
CREATE INDEX        IF NOT EXISTS idx_cards_user_id   ON cards(user_id);
CREATE INDEX        IF NOT EXISTS idx_cards_user_status ON cards(user_id, status);

-- Переводы между своими картами
-- Бизнес-правило "только между своими картами" лучше наверное сделать на уровне сервиса, но еще утром лучше подумать.
-- status: 'PENDING' | 'SUCCESS' | 'FAILED'
CREATE TABLE IF NOT EXISTS transfers (
                                         id              BIGSERIAL PRIMARY KEY,
                                         user_id         BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    from_card_id    BIGINT       NOT NULL REFERENCES cards(id),
    to_card_id      BIGINT       NOT NULL REFERENCES cards(id),
    amount          NUMERIC(19,2) NOT NULL CHECK (amount > 0),
    status          VARCHAR(16)  NOT NULL CHECK (status IN ('PENDING','SUCCESS','FAILED')),
    description     TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_transfer_cards_different CHECK (from_card_id <> to_card_id)
    );

CREATE INDEX IF NOT EXISTS idx_transfers_user_created ON transfers(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_transfers_from_card    ON transfers(from_card_id);
CREATE INDEX IF NOT EXISTS idx_transfers_to_card      ON transfers(to_card_id);

-- Сидим роли (без пользователей, их сделаем позже через сервис/скрипт)
INSERT INTO roles(name) VALUES ('ADMIN') ON CONFLICT (name) DO NOTHING;
INSERT INTO roles(name) VALUES ('USER')  ON CONFLICT (name) DO NOTHING;


--rollback DROP TABLE IF EXISTS transfers;
--rollback DROP INDEX IF EXISTS idx_transfers_to_card;
--rollback DROP INDEX IF EXISTS idx_transfers_from_card;
--rollback DROP INDEX IF EXISTS idx_transfers_user_created;
--rollback DROP TABLE IF EXISTS cards;
--rollback DROP INDEX IF EXISTS idx_cards_user_status;
--rollback DROP INDEX IF EXISTS idx_cards_user_id;
--rollback DROP TABLE IF EXISTS user_roles;
--rollback DROP TABLE IF EXISTS roles;
--rollback DROP TABLE IF EXISTS users;