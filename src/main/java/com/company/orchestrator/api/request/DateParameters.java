package com.company.orchestrator.api.request;

import com.company.orchestrator.api.validator.DateParametersValidator;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Date parameters to be used in API requests
 * <p>
 * Validated by {@link DateParametersValidator}
 * </p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Date parameters")
public class DateParameters {
    private LocalDate fromDate;
    private LocalDate toDate;
}
