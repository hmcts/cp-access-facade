package uk.gov.moj.cpp.audit.jms;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Primary;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import uk.gov.moj.cpp.audit.config.ArtemisAuditProperties;
import uk.gov.moj.cpp.audit.model.HttpAuditEvent;

@Component
@Primary
@ConditionalOnClass(JmsTemplate.class)
@RequiredArgsConstructor
public class ArtemisAuditPublisher implements AuditPublisher {
    private static final Logger log = LoggerFactory.getLogger(ArtemisAuditPublisher.class);

    private final JmsTemplate jms;
    private final ArtemisAuditProperties props;
    private final MeterRegistry meter;

    @PostConstruct
    void init() {
        jms.setDeliveryPersistent("PERSISTENT".equalsIgnoreCase(props.getDeliveryMode()));
    }


    @Override
    public void publish(HttpAuditEvent event) {
        Timer.Sample sample = Timer.start(meter);
        log.debug("Publishing audit id={} dest='{}' method={} path={} status={} corr={}",
                event.eventId(), props.getDestination(), event.method(), event.path(), event.status(), event.correlationId());
        try {
            jms.convertAndSend(props.getDestination(), event, m -> {
                m.setJMSTimestamp(System.currentTimeMillis());
                m.setJMSPriority(props.getPriority());
                if (props.getTimeToLiveMs() > 0) {
                    m.setJMSExpiration(System.currentTimeMillis() + props.getTimeToLiveMs());
                }
                m.setStringProperty("service", event.service());
                m.setStringProperty("method", event.method());
                m.setIntProperty("status", event.status());
                return m;
            });
            meter.counter("audit.jms.sent").increment();
            sample.stop(meter.timer("audit.jms.latency"));
            log.debug("Published audit id={} to dest='{}'", event.eventId(), props.getDestination());
        } catch (Exception ex) {
            meter.counter("audit.jms.failed").increment();
            log.error("Publish FAILED id={} dest='{}' : {}", event.eventId(), props.getDestination(), ex.toString(), ex);
            throw ex;
        }
    }

}
