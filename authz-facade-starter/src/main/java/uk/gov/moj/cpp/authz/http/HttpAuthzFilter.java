package uk.gov.moj.cpp.authz.http;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UrlPathHelper;
import uk.gov.moj.cpp.authz.drools.Action;
import uk.gov.moj.cpp.authz.drools.DroolsAuthzEngine;
import uk.gov.moj.cpp.authz.http.RequestActionResolver.ResolvedAction;
import uk.gov.moj.cpp.authz.http.config.HttpAuthzProperties;
import uk.gov.moj.cpp.authz.http.providers.RequestUserAndGroupProvider;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class HttpAuthzFilter implements Filter {
    private final HttpAuthzProperties properties;
    private final IdentityClient identityClient;
    private final IdentityToGroupsMapper identityToGroupsMapper;
    private final DroolsAuthzEngine droolsAuthzEngine;

    public HttpAuthzFilter(final HttpAuthzProperties properties,
                           final IdentityClient identityClient,
                           final IdentityToGroupsMapper identityToGroupsMapper,
                           final DroolsAuthzEngine droolsAuthzEngine) {
        this.properties = properties;
        this.identityClient = identityClient;
        this.identityToGroupsMapper = identityToGroupsMapper;
        this.droolsAuthzEngine = droolsAuthzEngine;
    }

    @Override
    public void doFilter(final ServletRequest request,
                         final ServletResponse response,
                         final FilterChain filterChain) throws IOException, ServletException {

        boolean invokeChain = false;

        final HttpServletRequest httpRequest = (HttpServletRequest) request;
        final HttpServletResponse httpResponse = (HttpServletResponse) response;

        final String pathWithinApplication = new UrlPathHelper().getPathWithinApplication(httpRequest);

        boolean isExcluded = false;
        for (final String prefix : properties.getExcludePathPrefixes()) {
            if (pathWithinApplication.startsWith(prefix)) {
                isExcluded = true;
                break;
            }
        }

        if (isExcluded) {
            invokeChain = true;
        } else {
            final String userId = httpRequest.getHeader(properties.getUserIdHeader());
            if (StringUtils.hasText(userId)) {
                final ResolvedAction resolved =
                        RequestActionResolver.resolve(httpRequest, properties.getActionHeader(), pathWithinApplication);

                if (properties.isActionRequired() && !(resolved.vendorSupplied() || resolved.headerSupplied())) {
                    httpResponse.sendError(HttpServletResponse.SC_BAD_REQUEST,
                            "Missing header: " + properties.getActionHeader());
                } else {
                    final IdentityResponse identityResponse = identityClient.fetchIdentity(userId);
                    final Set<String> groups = identityToGroupsMapper.toGroups(identityResponse);
                    final AuthzPrincipal principal =
                            new AuthzPrincipal(identityResponse.userId(), null, null, null, groups);
                    httpRequest.setAttribute(AuthzPrincipal.class.getName(), principal);

                    final Map<String, Object> attributes = new HashMap<>();
                    attributes.put("method", httpRequest.getMethod());
                    attributes.put("path", pathWithinApplication);

                    final Action action = new Action(resolved.name(), attributes);
                    final RequestUserAndGroupProvider perRequestProvider =
                            new RequestUserAndGroupProvider(principal);

                    final boolean allowed = droolsAuthzEngine.evaluate(perRequestProvider, action);
                    if (allowed) {
                        invokeChain = true;
                    } else {
                        httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
                    }
                }

            } else {

                httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                        "Missing header: " + properties.getUserIdHeader());

            }
        }

        if (invokeChain) {
            filterChain.doFilter(request, response);
        }
    }
}
