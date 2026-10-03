CREATE TABLE money_accounts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    name VARCHAR(60) NOT NULL,
    type VARCHAR(20) NOT NULL,
    opening_balance DECIMAL(19,2) NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_money_accounts_user FOREIGN KEY (user_id) REFERENCES app_users (id) ON DELETE CASCADE,
    CONSTRAINT uk_money_accounts_user_name UNIQUE (user_id, name),
    CONSTRAINT chk_money_accounts_type CHECK (type IN ('CASH', 'BANK', 'MOBILE_WALLET', 'CREDIT_CARD', 'OTHER')),
    CONSTRAINT chk_money_accounts_opening_balance CHECK (opening_balance >= 0),
    INDEX idx_money_accounts_user (user_id)
) ENGINE=InnoDB DEFAULT CHARACTER SET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO money_accounts (user_id, name, type, opening_balance)
SELECT id, 'Cash', 'CASH', 0 FROM app_users;

ALTER TABLE money_transactions ADD COLUMN account_id BIGINT NULL;
UPDATE money_transactions t
JOIN money_accounts a ON a.user_id = t.user_id AND a.name = 'Cash'
SET t.account_id = a.id;
ALTER TABLE money_transactions MODIFY account_id BIGINT NOT NULL;
ALTER TABLE money_transactions
    ADD CONSTRAINT fk_transactions_account FOREIGN KEY (account_id) REFERENCES money_accounts (id),
    ADD INDEX idx_transactions_account_date (account_id, transaction_date);

CREATE TABLE money_transfers (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    from_account_id BIGINT NOT NULL,
    to_account_id BIGINT NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    transfer_date DATE NOT NULL,
    note VARCHAR(500) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_money_transfers_user FOREIGN KEY (user_id) REFERENCES app_users (id) ON DELETE CASCADE,
    CONSTRAINT fk_money_transfers_from FOREIGN KEY (from_account_id) REFERENCES money_accounts (id),
    CONSTRAINT fk_money_transfers_to FOREIGN KEY (to_account_id) REFERENCES money_accounts (id),
    CONSTRAINT chk_money_transfers_accounts CHECK (from_account_id <> to_account_id),
    CONSTRAINT chk_money_transfers_amount CHECK (amount > 0),
    INDEX idx_money_transfers_user_date (user_id, transfer_date)
) ENGINE=InnoDB DEFAULT CHARACTER SET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
