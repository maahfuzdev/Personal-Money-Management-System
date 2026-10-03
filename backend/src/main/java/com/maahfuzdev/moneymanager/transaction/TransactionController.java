package com.maahfuzdev.moneymanager.transaction;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    public TransactionPageResponse list(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size,
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) @Size(max = 100) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return transactionService.list(jwt.getSubject(), accountId, type, search, startDate, endDate, page, size);
    }

    @GetMapping("/categories")
    public List<String> categories(@AuthenticationPrincipal Jwt jwt,
                                  @RequestParam(required = false) TransactionType type) {
        return transactionService.categorySuggestions(jwt.getSubject(), type);
    }

    @GetMapping(value = "/export.csv", produces = "text/csv")
    public ResponseEntity<byte[]> export(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) @Size(max = 100) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        byte[] csv = transactionService.exportCsv(jwt.getSubject(), accountId, type, search, startDate, endDate);
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        org.springframework.http.ContentDisposition.attachment()
                                .filename("transactions.csv", StandardCharsets.UTF_8).build().toString())
                .body(csv);
    }

    @PostMapping(value = "/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TransactionImportPreviewResponse previewImport(@AuthenticationPrincipal Jwt jwt,
            @RequestPart("file") MultipartFile file) {
        if (file.isEmpty() || file.getSize() > 1_048_576) throw new InvalidTransactionCsvException("Choose a CSV file smaller than 1 MB.");
        if (file.getOriginalFilename() == null || !file.getOriginalFilename().toLowerCase().endsWith(".csv"))
            throw new InvalidTransactionCsvException("Choose a .csv file.");
        try { return transactionService.previewCsv(jwt.getSubject(), file.getBytes()); }
        catch (java.io.IOException ex) { throw new InvalidTransactionCsvException("Could not read the CSV file."); }
    }

    @PostMapping("/import")
    public TransactionImportResult importTransactions(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody TransactionImportRequest request) {
        return transactionService.importTransactions(jwt.getSubject(), request.transactions());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse create(@AuthenticationPrincipal Jwt jwt,
                                      @Valid @RequestBody TransactionRequest request) {
        return transactionService.create(jwt.getSubject(), request);
    }

    @PutMapping("/{id}")
    public TransactionResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                      @Valid @RequestBody TransactionRequest request) {
        return transactionService.update(jwt.getSubject(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        transactionService.delete(jwt.getSubject(), id);
    }

    @GetMapping("/summary")
    public TransactionSummary summary(@AuthenticationPrincipal Jwt jwt) {
        return transactionService.summary(jwt.getSubject());
    }
}
