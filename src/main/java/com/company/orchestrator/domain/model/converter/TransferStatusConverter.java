package com.company.orchestrator.domain.model.converter;

import com.company.orchestrator.domain.model.TransferStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class TransferStatusConverter implements AttributeConverter<TransferStatus, Integer> {

    @Override
    public Integer convertToDatabaseColumn(TransferStatus attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getId();
    }

    @Override
    public TransferStatus convertToEntityAttribute(Integer dbData) {
        if (dbData == null) {
            return null;
        }
        return TransferStatus.getById(dbData);
    }
}