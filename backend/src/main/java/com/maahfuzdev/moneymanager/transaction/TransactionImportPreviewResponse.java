package com.maahfuzdev.moneymanager.transaction;

import java.util.List;

public record TransactionImportPreviewResponse(int validCount, int duplicateCount, int invalidCount,
                                                List<TransactionImportPreview> rows) {}
