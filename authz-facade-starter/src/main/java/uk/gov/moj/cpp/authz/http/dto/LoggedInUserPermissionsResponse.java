package uk.gov.moj.cpp.authz.http.dto;

import java.util.List;

public record LoggedInUserPermissionsResponse(
        List<UserGroup> groups,
        List<SwitchableRole> switchableRoles,
        List<UserPermission> permissions
) {
}
