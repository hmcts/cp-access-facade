package uk.gov.moj.cpp.access.demo.jms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "demo.audit", name = "log-consumer", havingValue = "true")
public class LogAuditConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogAuditConsumer.class);

    @JmsListener(destination = "jms.topic.auditing.event")
    public void onAuditEvent(final String message) {
        LOGGER.info("Audit payload: {}", message);
    }
}
