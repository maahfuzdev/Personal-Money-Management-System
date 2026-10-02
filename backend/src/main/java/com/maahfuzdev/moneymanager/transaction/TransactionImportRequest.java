package com.maahfuzdev.moneymanager.transaction;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record TransactionImportRequest(@NotEmpty @Size(max = 1000) List<@Valid TransactionRequest> transactions) {}
