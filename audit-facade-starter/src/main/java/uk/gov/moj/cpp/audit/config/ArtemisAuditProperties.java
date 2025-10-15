package uk.gov.moj.cpp.audit.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "audit.artemis")
public class ArtemisAuditProperties {
    private String destination = "jms.queue.audit";
    private int priority = 4;
    private long timeToLiveMs = 0;
    private String deliveryMode = "PERSISTENT";

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public long getTimeToLiveMs() {
        return timeToLiveMs;
    }

    public void setTimeToLiveMs(long timeToLiveMs) {
        this.timeToLiveMs = timeToLiveMs;
    }

    public String getDeliveryMode() {
        return deliveryMode;
    }

    public void setDeliveryMode(String deliveryMode) {
        this.deliveryMode = deliveryMode;
    }
}
