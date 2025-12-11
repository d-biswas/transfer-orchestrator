package com.company.orchestrator.infrastructure.props;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "http.client")
public class HttpClientProperties {

    /** Connection timeout in milliseconds */
    private int connectTimeout;

    /** Response read timeout in milliseconds */
    private int readTimeout;

    /** Maximum total connections in the pool */
    private int maxTotalConnection;

    /** Maximum connections per route/host */
    private int maxConnectionPerRoute;
}
