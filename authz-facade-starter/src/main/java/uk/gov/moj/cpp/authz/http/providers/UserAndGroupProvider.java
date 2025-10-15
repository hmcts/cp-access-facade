package uk.gov.moj.cpp.authz.http.providers;

import uk.gov.moj.cpp.authz.drools.Action;

@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface UserAndGroupProvider {
    boolean isMemberOfAnyOfTheSuppliedGroups(Action action, String... groups);
}
