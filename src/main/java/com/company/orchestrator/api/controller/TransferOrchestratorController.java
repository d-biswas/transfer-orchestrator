package com.company.orchestrator.api.controller;

import com.company.orchestrator.api.constants.ApiConstants;
import com.company.orchestrator.api.request.TransferInitiateDto;
import com.company.orchestrator.api.response.PagedResponseContent;
import com.company.orchestrator.api.response.ResponseContent;
import com.company.orchestrator.api.response.TransferDto;
import com.company.orchestrator.api.response.TransferResponseDto;
import com.company.orchestrator.api.validator.TransferInitiateRequestValidator;
import com.company.orchestrator.audit.model.AuditEvent;
import com.company.orchestrator.domain.facade.TransferOrchestrator;
import com.company.orchestrator.domain.model.TransferStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiConstants.API_VERSION)
@Validated
public class TransferOrchestratorController {

    private final TransferOrchestrator transferOrchestrator;
    private final TransferInitiateRequestValidator transferInitiateRequestValidator;

    /**
     * Registers custom validator for TransferRequestDto
     * This validator will be invoked when @Valid annotation is used
     */
    @InitBinder("transferRequestDto")
    protected void initBinder(WebDataBinder binder) {
        binder.addValidators(transferInitiateRequestValidator);
    }

    @PostMapping(ApiConstants.Path.TRANSFERS)
    public ResponseEntity<TransferResponseDto> initiateTransfer(@Valid @RequestBody TransferInitiateDto requestDto) {
        TransferResponseDto response = transferOrchestrator.initiateTransfer(requestDto);
        return ResponseEntity.ok(response);
    }

    @GetMapping(ApiConstants.Path.TRANSFER_BY_ID)
    public ResponseEntity<TransferStatus> getTransferStatus(@PathVariable Long id) {
        TransferStatus response = transferOrchestrator.getTransferStatus(id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping(ApiConstants.Path.TRANSFER_BY_ID)
    public ResponseEntity<Void> cancelTransfer(@PathVariable Long id) {
        transferOrchestrator.cancelTransfer(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(ApiConstants.Path.TRANSFERS)
    public ResponseEntity<PagedResponseContent<TransferDto>> getTransfers(@RequestParam(defaultValue = "0") @Min(0) int page,
                                                                          @RequestParam(defaultValue = "10") @Min(1) int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<TransferDto> transfers = transferOrchestrator.findTransfers(pageable);
        return ResponseEntity.ok(new PagedResponseContent<>(transfers));
    }


    @GetMapping(ApiConstants.Path.GET_AUDIT_LOGS)
    public ResponseEntity<ResponseContent<List<AuditEvent>>> getAuditLogsByTransferId(@PathVariable Long id) {
        List<AuditEvent> transfers = transferOrchestrator.getTransferAuditLogs(id);
        return ResponseEntity.ok(new ResponseContent<>(transfers));
    }
}
