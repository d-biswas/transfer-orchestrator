package com.company.orchestrator.api.validator;

import com.company.orchestrator.api.request.PageParameters;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

/**
 * Custom validator for Page parameters
 */
@Component
@RequiredArgsConstructor
public class PageParametersValidator implements Validator {

    private static final String FIELD_PAGE_NO = "page";
    private static final String FIELD_PAGE_SIZE = "size";

    @Override
    public boolean supports(@Nonnull Class<?> clazz) {
        return PageParameters.class.equals(clazz);
    }

    @Override
    public void validate(@Nonnull Object target, @Nonnull Errors errors) {
        PageParameters parameters = (PageParameters) target;
        if (parameters.hasPage() &&  parameters.getPage() < 0) {
            errors.rejectValue(FIELD_PAGE_NO, StringUtils.EMPTY, "Invalid page number");
        } else if (parameters.hasPageSize() && parameters.getSize() <= 0) {
            errors.rejectValue(FIELD_PAGE_SIZE, StringUtils.EMPTY, "Invalid page size");
        }
    }
}
