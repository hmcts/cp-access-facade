package uk.gov.moj.cpp.authz.http;

import uk.gov.moj.cpp.authz.http.config.HttpAuthzProperties;
import uk.gov.moj.cpp.authz.http.dto.UserGroup;

import java.util.LinkedHashSet;
import java.util.Set;

public final class DefaultIdentityToGroupsMapper implements IdentityToGroupsMapper {
    private final java.util.Map<String, String> aliases;

    public DefaultIdentityToGroupsMapper(final HttpAuthzProperties properties) {
        this.aliases = properties.getGroupAliases();
    }

    @Override
    public Set<String> toGroups(final IdentityResponse identityResponse) {
        final Set<String> groups = new LinkedHashSet<>();
        if (identityResponse != null && identityResponse.groups() != null) {

            for (final UserGroup userGroup : identityResponse.groups()) {
                if (userGroup == null) {
                    continue;
                }
                final String name = userGroup.groupName();
                if (name != null && !name.isBlank()) {
                    final String canonical = aliases.getOrDefault(name, name);
                    groups.add(canonical);
                }
                final String prosecutingAuthority = userGroup.prosecutingAuthority();
                if (prosecutingAuthority != null && !prosecutingAuthority.isBlank()) {
                    final String paCanonical = aliases.getOrDefault("Prosecuting Authority Access", "Prosecuting Authority Access");
                    groups.add(paCanonical);
                }

            }

        }
        return groups;
    }
}
