package uk.gov.moj.cpp.authz.http.dto;

public record UserGroup(
        String groupId,
        String groupName,
        String prosecutingAuthority
) {
}