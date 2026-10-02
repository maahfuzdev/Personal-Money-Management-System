package com.maahfuzdev.moneymanager.goal;

import com.maahfuzdev.moneymanager.user.AppUser;
import com.maahfuzdev.moneymanager.user.AppUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class SavingsGoalService {

    private final SavingsGoalRepository goalRepository;
    private final GoalContributionRepository contributionRepository;
    private final AppUserRepository userRepository;

    public SavingsGoalService(SavingsGoalRepository goalRepository, GoalContributionRepository contributionRepository,
                              AppUserRepository userRepository) {
        this.goalRepository = goalRepository;
        this.contributionRepository = contributionRepository;
        this.userRepository = userRepository;
    }

    public List<SavingsGoalResponse> list(String email) {
        AppUser owner = user(email);
        return goalRepository.findAllByUserIdOrderByCreatedAtDesc(owner.getId()).stream()
                .map(SavingsGoalResponse::from).toList();
    }

    @Transactional
    public SavingsGoalResponse create(String email, SavingsGoalRequest request) {
        validateAmounts(request);
        AppUser owner = user(email);
        SavingsGoal goal = goalRepository.save(new SavingsGoal(owner, request.name().trim(), request.targetAmount(),
                request.currentAmount(), request.targetDate(), cleanNote(request.note())));
        return SavingsGoalResponse.from(goal);
    }

    @Transactional
    public SavingsGoalResponse update(String email, Long id, SavingsGoalRequest request) {
        validateAmounts(request);
        SavingsGoal goal = ownedGoal(user(email).getId(), id);
        goal.update(request.name().trim(), request.targetAmount(), request.currentAmount(), request.targetDate(),
                cleanNote(request.note()));
        return SavingsGoalResponse.from(goal);
    }

    @Transactional
    public void delete(String email, Long id) {
        goalRepository.delete(ownedGoal(user(email).getId(), id));
    }

    public List<GoalContributionResponse> contributions(String email, Long id) {
        SavingsGoal goal = ownedGoal(user(email).getId(), id);
        return contributionRepository.findAllByGoalIdOrderByCreatedAtDesc(goal.getId()).stream()
                .map(GoalContributionResponse::from).toList();
    }

    @Transactional
    public SavingsGoalResponse contribute(String email, Long id, GoalContributionRequest request) {
        SavingsGoal goal = goalRepository.findOwnedForUpdate(id, user(email).getId())
                .orElseThrow(SavingsGoalNotFoundException::new);
        goal.addContribution(request.amount());
        String note = request.note() == null || request.note().isBlank() ? null : request.note().trim();
        contributionRepository.save(new GoalContribution(goal, request.amount(), note));
        return SavingsGoalResponse.from(goal);
    }

    private void validateAmounts(SavingsGoalRequest request) {
        if (request.currentAmount().compareTo(request.targetAmount()) > 0) {
            throw new SavingsGoalAmountException();
        }
    }

    private SavingsGoal ownedGoal(Long userId, Long id) {
        return goalRepository.findByIdAndUserId(id, userId).orElseThrow(SavingsGoalNotFoundException::new);
    }

    private AppUser user(String email) {
        return userRepository.findByEmail(email).orElseThrow(SavingsGoalNotFoundException::new);
    }

    private String cleanNote(String note) {
        return note == null || note.isBlank() ? null : note.trim();
    }
}
