CREATE TABLE money_account_adjustments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    previous_balance DECIMAL(19,2) NOT NULL,
    actual_balance DECIMAL(19,2) NOT NULL,
    adjustment_amount DECIMAL(19,2) NOT NULL,
    adjustment_date DATE NOT NULL,
    note VARCHAR(300) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_account_adjustments_user FOREIGN KEY (user_id) REFERENCES app_users (id) ON DELETE CASCADE,
    CONSTRAINT fk_account_adjustments_account FOREIGN KEY (account_id) REFERENCES money_accounts (id),
    INDEX idx_account_adjustments_user_date (user_id, adjustment_date, created_at)
) ENGINE=InnoDB DEFAULT CHARACTER SET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
