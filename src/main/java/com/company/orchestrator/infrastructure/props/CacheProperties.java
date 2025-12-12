package com.company.orchestrator.infrastructure.props;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "spring.data.redis")
public class CacheProperties {
    private String host;
    private int port;
    private boolean clusterModeEnabled;
}
