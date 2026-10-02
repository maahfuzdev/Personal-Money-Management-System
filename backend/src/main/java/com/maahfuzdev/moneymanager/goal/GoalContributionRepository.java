package com.maahfuzdev.moneymanager.goal;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GoalContributionRepository extends JpaRepository<GoalContribution, Long> {
    List<GoalContribution> findAllByGoalIdOrderByCreatedAtDesc(Long goalId);
}
