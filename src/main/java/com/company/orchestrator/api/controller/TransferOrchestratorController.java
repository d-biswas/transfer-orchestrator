package com.company.orchestrator.api.controller;

import com.company.orchestrator.api.constants.ApiConstants;
import com.company.orchestrator.api.dto.TransferRequestDto;
import com.company.orchestrator.api.dto.TransferResultDto;
import com.company.orchestrator.api.validator.TransferRequestValidator;
import com.company.orchestrator.domain.facade.TransferOrchestrator;
import com.company.orchestrator.domain.model.TransferStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiConstants.API_VERSION)
public class TransferOrchestratorController {

    private final TransferOrchestrator transferOrchestrator;
    private final TransferRequestValidator transferRequestValidator;

    /**
     * Registers custom validator for TransferRequestDto
     * This validator will be invoked when @Valid annotation is used
     */
    @InitBinder("transferRequestDto")
    protected void initBinder(WebDataBinder binder) {
        binder.addValidators(transferRequestValidator);
    }

    @PostMapping(ApiConstants.Path.TRANSFERS)
    public ResponseEntity<TransferResultDto> initiateTransfer(@Valid @RequestBody TransferRequestDto requestDto) {
        TransferResultDto response = transferOrchestrator.initiateTransfer(requestDto);
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
}
