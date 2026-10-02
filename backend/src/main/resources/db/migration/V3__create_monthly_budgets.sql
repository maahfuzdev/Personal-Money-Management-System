CREATE TABLE monthly_budgets (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    category VARCHAR(60) NOT NULL,
    monthly_limit DECIMAL(19,2) NOT NULL,
    month_start DATE NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_budgets_user FOREIGN KEY (user_id) REFERENCES app_users (id) ON DELETE CASCADE,
    CONSTRAINT uk_budgets_user_category_month UNIQUE (user_id, category, month_start),
    CONSTRAINT chk_budgets_limit CHECK (monthly_limit > 0)
) ENGINE=InnoDB
  DEFAULT CHARACTER SET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_transactions_budget_lookup
    ON money_transactions (user_id, type, transaction_date, category);
