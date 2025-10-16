package uk.gov.moj.cpp.access.demo.jms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import uk.gov.moj.cpp.audit.model.HttpAuditEvent;

@Component
@ConditionalOnProperty(prefix = "demo.audit", name = "log-consumer", havingValue = "true", matchIfMissing = false)
public class LogAuditConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogAuditConsumer.class);

    @JmsListener(destination = "${audit.artemis.destination:jms.queue.audit}")
    public void onAuditEvent(final HttpAuditEvent event) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("AUDIT RECEIVED: method={} path={} status={} userId={} latency={}ms",
                    event.method(), event.path(), event.requestBody(), event.userId(), event.latencyMs());
        }
    }
}
