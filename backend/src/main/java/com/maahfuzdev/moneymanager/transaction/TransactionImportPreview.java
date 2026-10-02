package com.maahfuzdev.moneymanager.transaction;

import java.util.List;

public record TransactionImportPreview(int rowNumber, String date, String type, String category,
                                       String note, String amount, boolean duplicate,
                                       List<String> errors, TransactionRequest transaction) {}
