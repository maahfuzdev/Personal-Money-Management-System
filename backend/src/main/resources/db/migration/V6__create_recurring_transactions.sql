CREATE TABLE recurring_transactions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    type VARCHAR(10) NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    category VARCHAR(60) NOT NULL,
    note VARCHAR(500) NULL,
    frequency VARCHAR(10) NOT NULL,
    start_date DATE NOT NULL,
    next_run_date DATE NOT NULL,
    end_date DATE NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_recurring_transactions_user FOREIGN KEY (user_id) REFERENCES app_users (id) ON DELETE CASCADE,
    CONSTRAINT chk_recurring_transactions_type CHECK (type IN ('INCOME', 'EXPENSE')),
    CONSTRAINT chk_recurring_transactions_frequency CHECK (frequency IN ('WEEKLY', 'MONTHLY', 'YEARLY')),
    CONSTRAINT chk_recurring_transactions_amount CHECK (amount > 0),
    INDEX idx_recurring_due (active, next_run_date),
    INDEX idx_recurring_user (user_id, active, next_run_date)
) ENGINE=InnoDB
  DEFAULT CHARACTER SET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;
