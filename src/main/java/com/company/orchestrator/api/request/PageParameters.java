package com.company.orchestrator.api.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Page parameters")
public class PageParameters {
    private static final int DEFAULT_PAGE_SIZE = 10;
    private Integer page;
    private Integer size;

    public boolean hasPageSize() {
        return size != null;
    }

    public boolean hasPage() {
        return page != null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    public Pageable getPageable() {
        return getPageable(DEFAULT_PAGE_SIZE);
    }

    @JsonIgnore
    @Schema(hidden = true)
    public Pageable getPageable(Integer defaultLimit) {
        return PageParameters.createPageable(page, size, defaultLimit);
    }

    public static Pageable createPageable(Integer page, Integer limit, Integer defaultLimit) {
        int pageNo = page != null ? Math.max(0, page) : 0;
        int pageSize = limit != null && limit > 0 ? limit : defaultLimit;
        return PageRequest.of(pageNo, pageSize);
    }
}
