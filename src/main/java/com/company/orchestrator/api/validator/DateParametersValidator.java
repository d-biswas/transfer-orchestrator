package com.company.orchestrator.api.validator;

import com.company.orchestrator.api.request.DateParameters;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class DateParametersValidator implements Validator {
    private static final String FIELD_FROM_DATE = "fromDate";
    private static final String FIELD_TO_DATE = "toDate";

    @Override
    public boolean supports(@Nonnull Class<?> clazz) {
        return DateParameters.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(@Nonnull Object target, @Nonnull Errors errors) {
        DateParameters params = (DateParameters) target;

        LocalDate from = params.getFromDate();
        LocalDate to = params.getToDate();

        if (from == null) {
            errors.rejectValue(FIELD_FROM_DATE, "fromDate.null", "fromDate must not be null");
        }
        if (to == null) {
            errors.rejectValue(FIELD_TO_DATE, "toDate.null", "toDate must not be null");
        }
        if (from != null && to != null && from.isAfter(to)) {
            errors.rejectValue(FIELD_FROM_DATE, "dateRange.invalid", "fromDate must be before or equal to toDate");
        }
    }
}
