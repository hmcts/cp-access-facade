package uk.gov.moj.cpp.audit.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import uk.gov.moj.cpp.audit.core.AsyncAuditChannel;
import uk.gov.moj.cpp.audit.core.AuditOutbox;
import uk.gov.moj.cpp.audit.core.AuditRedactor;
import uk.gov.moj.cpp.audit.jms.ArtemisAuditPublisher;
import uk.gov.moj.cpp.audit.jms.AuditPublisher;
import uk.gov.moj.cpp.audit.web.HttpAuditFilter;

@AutoConfiguration
@EnableScheduling
@EnableConfigurationProperties({HttpAuditProperties.class, ArtemisAuditProperties.class})
@ConditionalOnProperty(prefix = "audit.http", name = "enabled", havingValue = "true")
public class AuditAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(AuditAutoConfiguration.class);

    private final HttpAuditProperties httpProps;
    private final ArtemisAuditProperties artProps;

    public AuditAutoConfiguration(HttpAuditProperties httpProps, ArtemisAuditProperties artProps) {
        this.httpProps = httpProps;
        this.artProps = artProps;
    }

    @PostConstruct
    void onStart() {
        log.info("CPP HTTP Audit starter ACTIVE -> destination='{}', queueCapacity={}, workers={}, includeBodies=req:{}, res:{}",
                artProps.getDestination(), httpProps.getQueueCapacity(), httpProps.getWorkerThreads(),
                httpProps.isIncludeRequestBody(), httpProps.isIncludeResponseBody());
    }

    @Bean
    @ConditionalOnMissingBean(ObjectMapper.class)
    ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    @ConditionalOnMissingBean(MeterRegistry.class)
    MeterRegistry meterRegistry() {
        return new SimpleMeterRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    AuditRedactor auditRedactor(HttpAuditProperties props) {
        return new AuditRedactor(props);
    }

    @Bean
    @ConditionalOnClass(JmsTemplate.class)
    @ConditionalOnMissingBean(AuditPublisher.class)
    AuditPublisher auditPublisher(JmsTemplate jms,
                                  ArtemisAuditProperties artemisProps,
                                  MeterRegistry meterRegistry) {
        return new ArtemisAuditPublisher(jms, artemisProps, meterRegistry);
    }

    @Bean
    @ConditionalOnMissingBean
    AuditOutbox auditOutbox(ObjectMapper mapper,
                            HttpAuditProperties httpProps,
                            MeterRegistry meterRegistry,
                            AuditPublisher publisher) {
        return new AuditOutbox(mapper, httpProps, meterRegistry, publisher);
    }

    @Bean
    @ConditionalOnMissingBean
    AsyncAuditChannel asyncAuditChannel(AuditPublisher publisher,
                                        HttpAuditProperties httpProps,
                                        AuditOutbox outbox,
                                        MeterRegistry meterRegistry) {
        return new AsyncAuditChannel(publisher, httpProps, outbox, meterRegistry);
    }

    @Bean
    FilterRegistrationBean<HttpAuditFilter> httpAuditFilterRegistration(HttpAuditProperties httpProps,
                                                                        AuditRedactor redactor,
                                                                        AsyncAuditChannel channel) {
        HttpAuditFilter filter = new HttpAuditFilter(httpProps, redactor, channel);
        FilterRegistrationBean<HttpAuditFilter> reg = new FilterRegistrationBean<>(filter);
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE + 20);
        reg.addUrlPatterns("/*");
        reg.setName("cppHttpAuditFilter");
        return reg;
    }

}


