package com.company.orchestrator.api.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Page;

@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PageInfo<T> {
    private Integer page;
    private Integer size;
    private Integer pageCount;
    private Long total;
    private boolean hasNextPage;

    public PageInfo(Page<T> page) {
        this.page = page.getNumber();
        this.size = page.getSize();
        this.pageCount = page.getTotalPages();
        this.total = page.getTotalElements();
        this.hasNextPage = page.hasNext();
    }
}
