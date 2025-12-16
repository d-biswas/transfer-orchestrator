package com.company.orchestrator.api.controller;

import com.company.orchestrator.api.constants.ApiConstants;
import com.company.orchestrator.api.error.ApiError;
import com.company.orchestrator.api.error.FieldValidationError;
import com.company.orchestrator.api.exception.ApiException;
import com.company.orchestrator.api.request.DateParameters;
import com.company.orchestrator.api.request.PageParameters;
import com.company.orchestrator.api.request.TransferInitiateDto;
import com.company.orchestrator.api.response.*;
import com.company.orchestrator.api.validator.DateParametersValidator;
import com.company.orchestrator.api.validator.PageParametersValidator;
import com.company.orchestrator.api.validator.TransferInitiateRequestValidator;
import com.company.orchestrator.audit.model.AuditEvent;
import com.company.orchestrator.audit.model.ComplianceReport;
import com.company.orchestrator.domain.facade.TransferOrchestrator;
import com.company.orchestrator.domain.model.TransferStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiConstants.API_VERSION)
public class TransferOrchestratorController {

    private final TransferOrchestrator transferOrchestrator;
    private final TransferInitiateRequestValidator transferInitiateRequestValidator;
    private final PageParametersValidator pageParametersValidator;
    private final DateParametersValidator dateParametersValidator;

    @Operation(summary = "Initiate a transfer")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Initiated a transfer successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation error",
                    content = @Content(schema = @Schema(implementation = FieldValidationError.class))
            )
    })
    @PostMapping(ApiConstants.Path.TRANSFERS)
    public ResponseEntity<ResponseContent<TransferResponseDto>> initiateTransfer(@RequestBody TransferInitiateDto requestDto,
                                                                BindingResult result) {
        transferInitiateRequestValidator.validate(requestDto, result);
        if (result.hasErrors()) {
            throw ApiException.fieldValidationError(result.getFieldErrors());
        }
        TransferResponseDto response = transferOrchestrator.initiateTransfer(requestDto);
        return ResponseEntity.ok(new ResponseContent<>(response));
    }

    @Operation(summary = "Get transfer status by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200",
                    description = "Transfer status retrieved successfully"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class))
            )
    })
    @GetMapping(ApiConstants.Path.TRANSFER_BY_ID)
    public ResponseEntity<ResponseContent<TransferStatus>> getTransferStatus(@PathVariable Long id) {
        TransferStatus response = transferOrchestrator.getTransferStatus(id);
        return ResponseEntity.ok(new ResponseContent<>(response));
    }

    @Operation(summary = "Cancel a transfer")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Transfer cancelled successfully"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Transfer not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class))
            )
    })
    @DeleteMapping(ApiConstants.Path.TRANSFER_BY_ID)
    public ResponseEntity<Void> cancelTransfer(@PathVariable Long id) {
        transferOrchestrator.cancelTransfer(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "List transfers")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Transfers retrieved successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid page parameters",
                    content = @Content(schema = @Schema(implementation = FieldValidationError.class))
            )
    })
    @GetMapping(ApiConstants.Path.TRANSFERS)
    public ResponseEntity<PagedResponseContent<TransferDto>> getTransfers(@ModelAttribute PageParameters pageParameters,
                                                                          BindingResult result) {
        pageParametersValidator.validate(pageParameters, result);
        if (result.hasErrors()) {
            throw ApiException.fieldValidationError(result.getFieldErrors());
        }
        Page<TransferDto> transfers = transferOrchestrator.findTransfers(pageParameters.getPageable());
        return ResponseEntity.ok(new PagedResponseContent<>(transfers));
    }

    @Operation(summary = "List audit logs of a transfer")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Audit logs retrieved successfully"
            )
    })
    @GetMapping(ApiConstants.Path.GET_AUDIT_LOGS)
    public ResponseEntity<ResponseContent<List<AuditEvent>>> getAuditLogsByTransferId(@PathVariable Long id) {
        List<AuditEvent> transfers = transferOrchestrator.getTransferAuditLogs(id);
        return ResponseEntity.ok(new ResponseContent<>(transfers));
    }

    @Operation(summary = "Generate compliance report")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Compliance report generated successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid date parameters",
                    content = @Content(schema = @Schema(implementation = FieldValidationError.class))
            )
    })
    @GetMapping(ApiConstants.Path.GET_TRANSFER_ANALYTICS)
    public ResponseEntity<ResponseContent<ComplianceReport>> getTransferAnalytics(@ModelAttribute DateParameters dateParameters,
                                                                                  BindingResult result) {
        dateParametersValidator.validate(dateParameters, result);
        if (result.hasErrors()) {
            throw ApiException.fieldValidationError(result.getFieldErrors());
        }
        ComplianceReport complianceReport = transferOrchestrator.getTransferAnalytics(dateParameters);
        return ResponseEntity.ok(new ResponseContent<>(complianceReport));
    }

    @Operation(summary = "Evaluate policies without initiating a transfer")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Policies evaluated successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation error",
                    content = @Content(schema = @Schema(implementation = FieldValidationError.class))
            )
    })
    @PostMapping(ApiConstants.Path.POLICY_EVALUATION)
    public ResponseEntity<ResponseContent<PolicyEvaluationResponse>> evaluatePolicies(@RequestBody TransferInitiateDto request,
                                                                                      BindingResult result) {
        transferInitiateRequestValidator.validate(request, result);
        if (result.hasErrors()) {
            throw ApiException.fieldValidationError(result.getFieldErrors());
        }
        PolicyEvaluationResponse evaluationResponse = transferOrchestrator.evaluatePolicies(request);
        return ResponseEntity.ok(new ResponseContent<>(evaluationResponse));
    }
}
