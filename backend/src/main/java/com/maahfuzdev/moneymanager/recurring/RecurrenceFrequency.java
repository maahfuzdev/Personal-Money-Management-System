package com.maahfuzdev.moneymanager.recurring;

import java.time.LocalDate;
import java.time.YearMonth;

public enum RecurrenceFrequency {
    WEEKLY,
    MONTHLY,
    YEARLY;

    public LocalDate nextDate(LocalDate currentDate, LocalDate anchorDate) {
        return switch (this) {
            case WEEKLY -> currentDate.plusWeeks(1);
            case MONTHLY -> {
                YearMonth nextMonth = YearMonth.from(currentDate).plusMonths(1);
                yield nextMonth.atDay(Math.min(anchorDate.getDayOfMonth(), nextMonth.lengthOfMonth()));
            }
            case YEARLY -> {
                YearMonth nextYearMonth = YearMonth.of(currentDate.getYear() + 1,
                        anchorDate.getMonthValue());
                yield nextYearMonth.atDay(Math.min(anchorDate.getDayOfMonth(), nextYearMonth.lengthOfMonth()));
            }
        };
    }
}
