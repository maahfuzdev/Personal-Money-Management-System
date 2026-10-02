package com.maahfuzdev.moneymanager.goal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SavingsGoalRepository extends JpaRepository<SavingsGoal, Long> {
    List<SavingsGoal> findAllByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<SavingsGoal> findByIdAndUserId(Long id, Long userId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select goal from SavingsGoal goal where goal.id = :id and goal.user.id = :userId")
    Optional<SavingsGoal> findOwnedForUpdate(@Param("id") Long id, @Param("userId") Long userId);
}
