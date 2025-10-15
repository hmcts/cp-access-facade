package uk.gov.moj.cpp.access.demo.web;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/usersgroups-query-api/query/api/rest/usersgroups/users")
public class UsersGroupsController {

    public record UserGroup(String groupId, String groupName, String prosecutingAuthority) {}
    public record SwitchableRole(String roleId, String roleName) {}
    public record UserPermission(String permissionId, String object, String action, String description) {}
    public record LoggedInUserPermissionsResponse(
            List<UserGroup> groups, List<SwitchableRole> switchableRoles, List<UserPermission> permissions) {}

    @GetMapping(
            value = "/logged-in-user/permissions",
            produces = "application/vnd.usersgroups.get-logged-in-user-permissions+json")
    public ResponseEntity<LoggedInUserPermissionsResponse> getPermissions(
            @RequestHeader("CJSCPPUID") final String userId) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.usersgroups.get-logged-in-user-permissions+json"))
                .body(sampleFor(userId));
    }

    private LoggedInUserPermissionsResponse sampleFor(final String userId) {
        if ("la-user-1".equalsIgnoreCase(userId)) {
            final List<UserGroup> groups = List.of(
                    new UserGroup("63cae459-0e51-4d60-bcf8-c5324be50ba4", "Legal Advisers", "ALL"),
                    new UserGroup("53292fc8-d164-4a6c-8722-cdbc795cf83a", "Court Administrators", "ALL")
            );
            return new LoggedInUserPermissionsResponse(groups, List.of(), List.of());
        }
        if ("da-user-1".equalsIgnoreCase(userId)) {
            final List<UserGroup> groups = List.of(
                    new UserGroup("63cae459-0e51-4d60-bcf8-c5324be50ba4", "Defence Lawyer", "ALL"),
                    new UserGroup("53292fc8-d164-4a6c-8722-cdbc795cf83a", "Court Clerk", "ALL")
            );
            return new LoggedInUserPermissionsResponse(groups, List.of(), List.of());
        }
        final List<UserGroup> groups = List.of(new UserGroup("guest", "Guests", null));
        return new LoggedInUserPermissionsResponse(groups, List.of(), List.of());
    }
}
