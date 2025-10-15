package uk.gov.moj.cpp.audit.jms;

import uk.gov.moj.cpp.audit.model.HttpAuditEvent;

public interface AuditPublisher {
    void publish(HttpAuditEvent event);
}
