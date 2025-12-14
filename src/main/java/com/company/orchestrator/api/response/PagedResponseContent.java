package com.company.orchestrator.api.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PagedResponseContent<T> {
    private List<T> content;
    private PageInfo<T> pageInfo;

    public PagedResponseContent(Page<T> page) {
        this.pageInfo = new PageInfo<>(page);
        this.content = page.getContent();
    }
}

 