package uk.gov.moj.cpp.authz.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import uk.gov.moj.cpp.authz.http.config.HttpAuthzProperties;
import uk.gov.moj.cpp.authz.http.dto.LoggedInUserPermissionsResponse;

import java.net.URI;
import java.time.Duration;

public final class IdentityClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(IdentityClient.class);
    private final HttpAuthzProperties properties;
    private final RestTemplate restTemplate;

    public IdentityClient(final HttpAuthzProperties properties) {
        this.properties = properties;
        final SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(20).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(21).toMillis());
        this.restTemplate = new RestTemplate(factory);
    }

    public IdentityResponse fetchIdentity(final String userId) {
        final String template = properties.getIdentityUrlTemplate();
        final String url = template.contains("{userId}") ? template.replace("{userId}", userId) : template;
        final HttpHeaders headers = new HttpHeaders();
        final IdentityResponse identityResponse ;
        headers.add("Accept", properties.getAcceptHeader());
        headers.add(properties.getUserIdHeader(), userId);
        final RequestEntity<Void> request = RequestEntity.get(URI.create(url)).headers(headers).build();
        final ResponseEntity<LoggedInUserPermissionsResponse> response = restTemplate.exchange(request, LoggedInUserPermissionsResponse.class);
        final LoggedInUserPermissionsResponse body = response.getBody();
        if (body == null) {
            LOGGER.warn("Empty identity response for userId={}", userId);
            identityResponse = new IdentityResponse(userId, java.util.List.of(), java.util.List.of());
        }
        else {
            identityResponse = new IdentityResponse(userId, body.groups(), body.permissions());
        }
        return identityResponse;
    }
}
