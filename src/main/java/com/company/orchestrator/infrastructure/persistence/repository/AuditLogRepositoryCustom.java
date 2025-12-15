package com.company.orchestrator.infrastructure.persistence.repository;

import com.company.orchestrator.audit.model.ComplianceReport;
import com.company.orchestrator.audit.model.DateRange;

public interface AuditLogRepositoryCustom {

    ComplianceReport generateComplianceReport(DateRange dateRange);
}
