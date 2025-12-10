package com.company.orchestrator.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

@Getter
@Setter
@MappedSuperclass
@NoArgsConstructor
public abstract class BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 8589226040485574229L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    protected Long id;

    @PrePersist
    private void __prePersist() {
        if (this instanceof Auditable auditable) {
            Instant now = Instant.now();
            if (auditable.getModifiedAt() == null) {
                auditable.setModifiedAt(now);
            }
            if (auditable.getCreatedAt() == null) {
                auditable.setCreatedAt(now);
            }
        }
    }

    @PreUpdate
    private void __preUpdate() {
        if (this instanceof Auditable auditable) {
            auditable.setModifiedAt(Instant.now());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }
        BaseEntity that = (BaseEntity) o;
        return (id != null) && (id.equals(that.id));
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : System.identityHashCode(this);
    }

    @Override
    public String toString() {
        return "Entity " + getClass().getSimpleName() + '[' + "id=" + id + ']';
    }
}
