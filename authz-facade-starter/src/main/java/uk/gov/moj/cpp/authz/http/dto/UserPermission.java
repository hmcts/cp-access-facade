package uk.gov.moj.cpp.authz.http.dto;

public record UserPermission(
        String permissionId,
        String object,
        String action,
        String description
) {
}