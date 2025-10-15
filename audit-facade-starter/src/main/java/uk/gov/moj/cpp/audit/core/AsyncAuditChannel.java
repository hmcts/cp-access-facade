package uk.gov.moj.cpp.audit.core;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.moj.cpp.audit.config.HttpAuditProperties;
import uk.gov.moj.cpp.audit.jms.AuditPublisher;
import uk.gov.moj.cpp.audit.model.HttpAuditEvent;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.stream.IntStream;

@Component
@RequiredArgsConstructor
public class AsyncAuditChannel {

    private final AuditPublisher publisher;
    private final HttpAuditProperties props;
    private final AuditOutbox outbox;
    private final MeterRegistry meter;

    private BlockingQueue<HttpAuditEvent> queue;
    private List<Thread> workers;

    @PostConstruct
    void start() {
        queue = new ArrayBlockingQueue<>(props.getQueueCapacity());
        workers = IntStream.range(0, props.getWorkerThreads())
                .mapToObj(i -> Thread.ofVirtual().name("audit-worker-" + i).unstarted(this::loop))
                .peek(Thread::start)
                .toList();
    }

    public void submit(HttpAuditEvent e) {
        boolean offered = queue.offer(e);
        if (!offered) {
            meter.counter("audit.queue.dropped").increment();
            if (props.getOutbox().isEnabled()) outbox.append(e);
        }
    }

    private void loop() {
        while (true) {
            try {
                HttpAuditEvent ev = queue.take();
                try {
                    publisher.publish(ev);
                } catch (Exception ex) {
                    if (props.getOutbox().isEnabled()) outbox.append(ev);
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}
