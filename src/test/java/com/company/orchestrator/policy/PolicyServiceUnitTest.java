package com.company.orchestrator.policy;

import com.company.orchestrator.api.request.TransferInitiateDto;
import com.company.orchestrator.domain.model.DataType;
import com.company.orchestrator.policy.model.PolicyEvaluationResult;
import com.company.orchestrator.policy.service.impl.PolicyServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
public class PolicyServiceUnitTest {
    @Mock
    RedisTemplate<String, Object> redisTemplate;

    @Mock
    ZSetOperations<String, Object> zSetOps;
    PolicyServiceImpl policyService;

    @BeforeEach
    void setup() throws Exception {
        MockitoAnnotations.openMocks(this);

        when(redisTemplate.opsForZSet()).thenReturn(zSetOps);
        policyService = new PolicyServiceImpl(new ObjectMapper(), redisTemplate);
        policyService.loadPolicies();
    }

    @Test
    void shouldAllowWhenAllPoliciesPass() {
        TransferInitiateDto request = TransferInitiateDto.builder()
                .assetId("asset-001")
                .providerId("provider-01")
                .consumerId("Yamaha-bd")
                .consumerCertification("ISO9001")
                .consumerRegion("EU")
                .dataType(DataType.QUALITY)
                .usagePurpose("QualityAnalysis")
                .build();

        PolicyEvaluationResult result = policyService.evaluatePolicies(request);
        assertTrue(result.allowed());
    }

    @Test
    void shouldCallRedisZSetAdd() {
        TransferInitiateDto request = TransferInitiateDto.builder()
                .assetId("asset-001")
                .providerId("provider-01")
                .consumerId("Yamaha-bd")
                .consumerCertification("ISO9001")
                .consumerRegion("EU")
                .dataType(DataType.QUALITY)
                .usagePurpose("QualityAnalysis")
                .build();

        policyService.evaluatePolicies(request);
        verify(zSetOps, times(1)).add(anyString(), any(), anyDouble());
        verify(redisTemplate, times(1)).expire(anyString(), eq(1L), eq(TimeUnit.HOURS));
    }

    @Test
    void shouldFailWhenRateLimitExceeded() {
        // Mock that RateLimitPolicy sees count >= maxRequests
        when(zSetOps.count(anyString(), anyDouble(), anyDouble())).thenReturn(100L); // maxRequests = 100

        TransferInitiateDto request = TransferInitiateDto.builder()
                .assetId("asset-001")
                .providerId("provider-01")
                .consumerId("Yamaha-bd")
                .consumerCertification("ISO9001")
                .consumerRegion("DE")
                .dataType(DataType.SUPPLY_CHAIN)
                .usagePurpose("QualityAnalysis")
                .build();

        PolicyEvaluationResult result = policyService.evaluatePolicies(request);

        assertFalse(result.allowed());

        // Redis should not be updated as limit reached
        verify(zSetOps, never()).add(anyString(), any(), anyDouble());
        verify(redisTemplate, never()).expire(anyString(), anyLong(), any());
    }

}
