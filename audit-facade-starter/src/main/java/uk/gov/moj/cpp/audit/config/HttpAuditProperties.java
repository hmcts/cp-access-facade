package uk.gov.moj.cpp.audit.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;
import java.util.List;

@ConfigurationProperties(prefix = "audit.http")
public class HttpAuditProperties {
    private boolean enabled = true;
    private boolean includeRequestBody = false;
    private boolean includeResponseBody = false;
    private int maxBodyBytes = 4096;
    private List<String> redactHeaders = List.of("authorization", "cookie", "set-cookie", "x-api-key");
    private List<String> redactJsonFields = List.of("password", "token", "secret", "cardNumber", "cvv");
    private FailurePolicy failurePolicy = FailurePolicy.FAIL_OPEN;
    private int queueCapacity = 5000;
    private int workerThreads = 2;
    private Outbox outbox = new Outbox();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isIncludeRequestBody() {
        return includeRequestBody;
    }

    public void setIncludeRequestBody(boolean includeRequestBody) {
        this.includeRequestBody = includeRequestBody;
    }

    public boolean isIncludeResponseBody() {
        return includeResponseBody;
    }

    public void setIncludeResponseBody(boolean includeResponseBody) {
        this.includeResponseBody = includeResponseBody;
    }

    public int getMaxBodyBytes() {
        return maxBodyBytes;
    }

    public void setMaxBodyBytes(int maxBodyBytes) {
        this.maxBodyBytes = maxBodyBytes;
    }

    public List<String> getRedactHeaders() {
        return redactHeaders;
    }

    public void setRedactHeaders(List<String> redactHeaders) {
        this.redactHeaders = redactHeaders;
    }

    public List<String> getRedactJsonFields() {
        return redactJsonFields;
    }

    public void setRedactJsonFields(List<String> redactJsonFields) {
        this.redactJsonFields = redactJsonFields;
    }

    public FailurePolicy getFailurePolicy() {
        return failurePolicy;
    }

    public void setFailurePolicy(FailurePolicy failurePolicy) {
        this.failurePolicy = failurePolicy;
    }

    public int getQueueCapacity() {
        return queueCapacity;
    }

    public void setQueueCapacity(int queueCapacity) {
        this.queueCapacity = queueCapacity;
    }

    public int getWorkerThreads() {
        return workerThreads;
    }

    public void setWorkerThreads(int workerThreads) {
        this.workerThreads = workerThreads;
    }

    public Outbox getOutbox() {
        return outbox;
    }

    public void setOutbox(Outbox outbox) {
        this.outbox = outbox;
    }

    public enum FailurePolicy {FAIL_OPEN, FAIL_CLOSED}

    public static class Outbox {
        private boolean enabled = true;
        private Path dir = Path.of("./audit-outbox");
        private long maxFileSizeBytes = 10 * 1024 * 1024;
        private int maxFiles = 200;
        private int replayIntervalSec = 15;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Path getDir() {
            return dir;
        }

        public void setDir(Path dir) {
            this.dir = dir;
        }

        public long getMaxFileSizeBytes() {
            return maxFileSizeBytes;
        }

        public void setMaxFileSizeBytes(long maxFileSizeBytes) {
            this.maxFileSizeBytes = maxFileSizeBytes;
        }

        public int getMaxFiles() {
            return maxFiles;
        }

        public void setMaxFiles(int maxFiles) {
            this.maxFiles = maxFiles;
        }

        public int getReplayIntervalSec() {
            return replayIntervalSec;
        }

        public void setReplayIntervalSec(int replayIntervalSec) {
            this.replayIntervalSec = replayIntervalSec;
        }
    }
}
