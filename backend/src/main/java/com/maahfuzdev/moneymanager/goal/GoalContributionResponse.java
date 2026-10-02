package com.maahfuzdev.moneymanager.goal;

import java.math.BigDecimal;
import java.time.Instant;

public record GoalContributionResponse(Long id, BigDecimal amount, String note, Instant createdAt) {
    static GoalContributionResponse from(GoalContribution contribution) {
        return new GoalContributionResponse(contribution.getId(), contribution.getAmount(), contribution.getNote(), contribution.getCreatedAt());
    }
}
