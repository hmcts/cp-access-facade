package uk.gov.moj.cpp.audit.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.gov.moj.cpp.audit.config.HttpAuditProperties;
import uk.gov.moj.cpp.audit.jms.AuditPublisher;
import uk.gov.moj.cpp.audit.model.HttpAuditEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@Component
@RequiredArgsConstructor
public class AuditOutbox {

    private final ObjectMapper mapper;
    private final HttpAuditProperties props;
    private final MeterRegistry meter;
    private final AuditPublisher publisher;

    private final ReentrantLock lock = new ReentrantLock();

    public void append(HttpAuditEvent ev) {
        try {
            lock.lock();
            Files.createDirectories(props.getOutbox().getDir());
            Path file = rotateIfNeeded();
            Files.writeString(file, mapper.writeValueAsString(ev) + "\n",
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            meter.counter("audit.outbox.appended").increment();
        } catch (Exception ex) {
            meter.counter("audit.outbox.failed").increment();
        } finally {
            lock.unlock();
        }
    }

    @Scheduled(fixedDelayString = "#{${audit.http.outbox.replay-interval-sec:15} * 1000}")
    public void replay() {
        if (!props.getOutbox().isEnabled()) return;
        Path dir = props.getOutbox().getDir();
        try {
            if (!Files.exists(dir)) return;
            try (var stream = Files.list(dir).sorted()) {
                for (Path f : stream.toList()) {
                    List<String> lines = Files.readAllLines(f);
                    List<String> keep = new ArrayList<>();
                    for (String line : lines) {
                        try {
                            var ev = mapper.readValue(line, HttpAuditEvent.class);
                            publisher.publish(ev);
                        } catch (Exception ex) {
                            keep.add(line);
                        }
                    }
                    if (keep.isEmpty()) {
                        Files.deleteIfExists(f);
                        meter.counter("audit.outbox.replayed").increment(lines.size());
                    } else {
                        Files.write(f, keep);
                    }
                }
            }
        } catch (IOException ignored) {
        }
    }

    private Path rotateIfNeeded() throws IOException {
        Path dir = props.getOutbox().getDir();
        Path current = dir.resolve("audit-" + LocalDate.now() + ".jsonl");
        if (!Files.exists(current)) return current;
        if (Files.size(current) < props.getOutbox().getMaxFileSizeBytes()) return current;
        String ts = DateTimeFormatter.ofPattern("HHmmss").format(LocalTime.now());
        return dir.resolve("audit-" + LocalDate.now() + "-" + ts + ".jsonl");
    }
}
