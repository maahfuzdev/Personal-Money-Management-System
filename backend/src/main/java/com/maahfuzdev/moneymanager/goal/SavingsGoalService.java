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
    private final AppUserRepository userRepository;

    public SavingsGoalService(SavingsGoalRepository goalRepository, AppUserRepository userRepository) {
        this.goalRepository = goalRepository;
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
