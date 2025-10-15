package uk.gov.moj.cpp.authz.http;

import java.util.Set;

public record AuthzPrincipal(
        String userId,
        String firstName,
        String lastName,
        String email,
        Set<String> groups
) {
}