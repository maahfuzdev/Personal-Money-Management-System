package com.maahfuzdev.moneymanager.transaction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.time.LocalDate;

public interface TransactionRepository extends JpaRepository<MoneyTransaction, Long> {

    @Query("select min(t.category) from MoneyTransaction t where t.user.id = :userId " +
            "and (:type is null or t.type = :type) group by lower(t.category) order by min(t.category)")
    List<String> findCategorySuggestions(@Param("userId") Long userId, @Param("type") TransactionType type);

    @Query(value = "select t from MoneyTransaction t where t.user.id = :userId " +
            "and (:type is null or t.type = :type) " +
            "and (:startDate is null or t.transactionDate >= :startDate) " +
            "and (:endDate is null or t.transactionDate <= :endDate) " +
            "and (:search is null or lower(t.category) like lower(concat('%', :search, '%')) " +
            "or lower(coalesce(t.note, '')) like lower(concat('%', :search, '%'))) ",
            countQuery = "select count(t) from MoneyTransaction t where t.user.id = :userId " +
                    "and (:type is null or t.type = :type) " +
                    "and (:startDate is null or t.transactionDate >= :startDate) " +
                    "and (:endDate is null or t.transactionDate <= :endDate) " +
                    "and (:search is null or lower(t.category) like lower(concat('%', :search, '%')) " +
                    "or lower(coalesce(t.note, '')) like lower(concat('%', :search, '%'))) ")
    Page<MoneyTransaction> searchByUser(@Param("userId") Long userId,
                                        @Param("type") TransactionType type,
                                        @Param("search") String search,
                                        @Param("startDate") LocalDate startDate,
                                        @Param("endDate") LocalDate endDate,
                                        Pageable pageable);

    @Query(value = "select t from MoneyTransaction t where t.user.id = :userId " +
            "and (:accountId is null or t.account.id = :accountId) " +
            "and (:type is null or t.type = :type) " +
            "and (:startDate is null or t.transactionDate >= :startDate) " +
            "and (:endDate is null or t.transactionDate <= :endDate) " +
            "and (:search is null or lower(t.category) like lower(concat('%', :search, '%')) " +
            "or lower(coalesce(t.note, '')) like lower(concat('%', :search, '%'))) ",
            countQuery = "select count(t) from MoneyTransaction t where t.user.id = :userId " +
                    "and (:accountId is null or t.account.id = :accountId) " +
                    "and (:type is null or t.type = :type) " +
                    "and (:startDate is null or t.transactionDate >= :startDate) " +
                    "and (:endDate is null or t.transactionDate <= :endDate) " +
                    "and (:search is null or lower(t.category) like lower(concat('%', :search, '%')) " +
                    "or lower(coalesce(t.note, '')) like lower(concat('%', :search, '%'))) ")
    Page<MoneyTransaction> searchByUserAndAccount(@Param("userId") Long userId,
                                        @Param("accountId") Long accountId,
                                        @Param("type") TransactionType type,
                                        @Param("search") String search,
                                        @Param("startDate") LocalDate startDate,
                                        @Param("endDate") LocalDate endDate,
                                        Pageable pageable);

    Optional<MoneyTransaction> findByIdAndUserId(Long id, Long userId);
    List<MoneyTransaction> findAllByUserIdOrderByTransactionDateDescCreatedAtDesc(Long userId);

    @Query(value = "SELECT a.name, MIN(t.category), MIN(t.note), t.transaction_date, t.type, t.amount, COUNT(*) " +
            "FROM money_transactions t JOIN money_accounts a ON a.id = t.account_id " +
            "WHERE t.user_id = :userId GROUP BY t.account_id, a.name, LOWER(t.category), COALESCE(t.note, ''), " +
            "t.transaction_date, t.type, t.amount HAVING COUNT(*) > 1 ORDER BY t.transaction_date DESC LIMIT 50",
            nativeQuery = true)
    List<Object[]> findDuplicateGroups(@Param("userId") Long userId);

    @Query("select avg(t.amount) from MoneyTransaction t where t.user.id = :userId and t.type = :type")
    BigDecimal averageAmountByUserAndType(@Param("userId") Long userId, @Param("type") TransactionType type);

    @Query("select t from MoneyTransaction t where t.user.id = :userId and t.type = :type " +
            "and t.amount >= :threshold order by t.amount desc, t.transactionDate desc")
    List<MoneyTransaction> findLargeTransactions(@Param("userId") Long userId, @Param("type") TransactionType type,
                                                @Param("threshold") BigDecimal threshold, Pageable pageable);

    @Query("select count(t) > 0 from MoneyTransaction t where t.user.id = :userId " +
            "and t.transactionDate = :date and t.type = :type and t.amount = :amount " +
            "and lower(t.category) = lower(:category) " +
            "and ((:note is null and t.note is null) or t.note = :note)")
    boolean existsDuplicate(@Param("userId") Long userId, @Param("date") LocalDate date,
                            @Param("type") TransactionType type, @Param("amount") BigDecimal amount,
                            @Param("category") String category, @Param("note") String note);

    @Query("select count(t) > 0 from MoneyTransaction t where t.user.id = :userId " +
            "and t.account.id = :accountId and t.transactionDate = :date and t.type = :type and t.amount = :amount " +
            "and lower(t.category) = lower(:category) " +
            "and ((:note is null and t.note is null) or t.note = :note)")
    boolean existsDuplicateInAccount(@Param("userId") Long userId, @Param("accountId") Long accountId,
                            @Param("date") LocalDate date, @Param("type") TransactionType type,
                            @Param("amount") BigDecimal amount, @Param("category") String category,
                            @Param("note") String note);

    @Query("select coalesce(sum(t.amount), 0) from MoneyTransaction t where t.user.id = :userId and t.type = :type")
    BigDecimal sumAmountByUserAndType(@Param("userId") Long userId, @Param("type") TransactionType type);

    @Query("select coalesce(sum(case when t.type = :incomeType then t.amount else -t.amount end), 0) " +
            "from MoneyTransaction t where t.account.id = :accountId and t.user.id = :userId")
    BigDecimal netAmountByAccount(@Param("accountId") Long accountId, @Param("userId") Long userId,
                                  @Param("incomeType") TransactionType incomeType);

    @Query("select lower(t.category), sum(t.amount) from MoneyTransaction t " +
            "where t.user.id = :userId and t.type = :type and t.transactionDate >= :start and t.transactionDate < :end " +
            "group by lower(t.category)")
    List<Object[]> sumExpensesByCategory(@Param("userId") Long userId, @Param("type") TransactionType type,
                                        @Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("select min(t.category), sum(t.amount) from MoneyTransaction t " +
            "where t.user.id = :userId and t.type = :type and t.transactionDate >= :start and t.transactionDate < :end " +
            "group by lower(t.category)")
    List<Object[]> summarizeExpensesByCategory(@Param("userId") Long userId, @Param("type") TransactionType type,
                                              @Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query(value = "SELECT YEAR(transaction_date), MONTH(transaction_date), type, SUM(amount) " +
            "FROM money_transactions WHERE user_id = :userId AND transaction_date >= :start " +
            "AND transaction_date < :end GROUP BY YEAR(transaction_date), MONTH(transaction_date), type",
            nativeQuery = true)
    List<Object[]> summarizeMonthly(@Param("userId") Long userId, @Param("start") LocalDate start,
                                   @Param("end") LocalDate end);
}
