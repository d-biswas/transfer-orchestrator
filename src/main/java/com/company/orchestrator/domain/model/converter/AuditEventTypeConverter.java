package com.company.orchestrator.domain.model.converter;

import com.company.orchestrator.domain.model.AuditEventType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class AuditEventTypeConverter implements AttributeConverter<AuditEventType, Integer> {

    @Override
    public Integer convertToDatabaseColumn(AuditEventType attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getId();
    }

    @Override
    public AuditEventType convertToEntityAttribute(Integer dbData) {
        if (dbData == null) {
            return null;
        }
        return AuditEventType.getById(dbData);
    }
}