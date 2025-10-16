package uk.gov.moj.cpp.authz.http.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.Ordered;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "authz.http")
public class HttpAuthzProperties {
    private boolean enabled;
    private String identityUrlTemplate = "http://localhost:8080/usersgroups-query-api/query/api/rest/usersgroups/users/logged-in-user/permissions?";
    private String userIdHeader = "CJSCPPUID";
    private String actionHeader = "CPP-ACTION";
    private String acceptHeader = "application/vnd.usersgroups.get-logged-in-user-permissions+json";
    private String droolsClasspathPattern = "classpath:/acl/**/*.drl";
    private boolean reloadOnEachRequest = true;
    private boolean actionRequired;
    private boolean denyWhenNoRules = true;
    private Map<String, String> groupAliases = new LinkedHashMap<>();
    private Integer filterOrder = Ordered.HIGHEST_PRECEDENCE + 30;
    private List<String> excludePathPrefixes = new ArrayList<>(List.of("/usersgroups-query-api/", "/actuator", "/error"));

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    public String getIdentityUrlTemplate() {
        return identityUrlTemplate;
    }

    public void setIdentityUrlTemplate(final String identityUrlTemplate) {
        this.identityUrlTemplate = identityUrlTemplate;
    }

    public String getUserIdHeader() {
        return userIdHeader;
    }

    public void setUserIdHeader(final String userIdHeader) {
        this.userIdHeader = userIdHeader;
    }

    public String getActionHeader() {
        return actionHeader;
    }

    public void setActionHeader(final String actionHeader) {
        this.actionHeader = actionHeader;
    }

    public String getAcceptHeader() {
        return acceptHeader;
    }

    public void setAcceptHeader(final String acceptHeader) {
        this.acceptHeader = acceptHeader;
    }

    public String getDroolsClasspathPattern() {
        return droolsClasspathPattern;
    }

    public void setDroolsClasspathPattern(final String droolsClasspathPattern) {
        this.droolsClasspathPattern = droolsClasspathPattern;
    }

    public boolean isReloadOnEachRequest() {
        return reloadOnEachRequest;
    }

    public void setReloadOnEachRequest(final boolean reloadOnEachRequest) {
        this.reloadOnEachRequest = reloadOnEachRequest;
    }

    public boolean isActionRequired() {
        return actionRequired;
    }

    public void setActionRequired(final boolean actionRequired) {
        this.actionRequired = actionRequired;
    }

    public boolean isDenyWhenNoRules() {
        return denyWhenNoRules;
    }

    public void setDenyWhenNoRules(final boolean denyWhenNoRules) {
        this.denyWhenNoRules = denyWhenNoRules;
    }

    public Map<String, String> getGroupAliases() {
        return groupAliases;
    }

    public void setGroupAliases(final Map<String, String> groupAliases) {
        this.groupAliases = groupAliases;
    }

    public Integer getFilterOrder() {
        return filterOrder;
    }

    public void setFilterOrder(final Integer filterOrder) {
        this.filterOrder = filterOrder;
    }

    public List<String> getExcludePathPrefixes() {
        return excludePathPrefixes;
    }

    public void setExcludePathPrefixes(final List<String> excludePathPrefixes) {
        this.excludePathPrefixes = excludePathPrefixes == null ? java.util.Collections.emptyList() : excludePathPrefixes;
    }
}
