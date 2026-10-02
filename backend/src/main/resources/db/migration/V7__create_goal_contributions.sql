CREATE TABLE savings_goal_contributions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    goal_id BIGINT NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    note VARCHAR(300) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_goal_contributions_goal FOREIGN KEY (goal_id) REFERENCES savings_goals (id) ON DELETE CASCADE,
    CONSTRAINT chk_goal_contributions_amount CHECK (amount > 0),
    INDEX idx_goal_contributions_goal_date (goal_id, created_at DESC)
) ENGINE=InnoDB
  DEFAULT CHARACTER SET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;
