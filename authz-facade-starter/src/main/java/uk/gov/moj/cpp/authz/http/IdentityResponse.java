package uk.gov.moj.cpp.authz.http;

import uk.gov.moj.cpp.authz.http.dto.UserGroup;
import uk.gov.moj.cpp.authz.http.dto.UserPermission;

import java.util.List;

public record IdentityResponse(
        String userId,
        List<UserGroup> groups,
        List<UserPermission> permissions
) {
}