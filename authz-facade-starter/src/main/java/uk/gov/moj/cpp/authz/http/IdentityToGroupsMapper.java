package uk.gov.moj.cpp.authz.http;

import java.util.Set;

@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface IdentityToGroupsMapper {
    Set<String> toGroups(IdentityResponse identityResponse);
}
