package uk.gov.moj.cpp.authz.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class RequestActionResolverTest {

    private static final String MEDIA_SJP_DELETE_FINANCIAL_MEANS =
            "application/vnd.sjp.delete-financial-means+json";
    private static final String MEDIA_HEARING_GET_DRAFT_RESULT =
            "application/vnd.hearing.get-draft-result+json";
    private static final String MEDIA_JSON = "application/json";

    private static final String HEADER_CPP_ACTION = "CPP-ACTION";
    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final String HEADER_ACCEPT = "Accept";

    private static final String PATH_HELLO = "/api/hello";
    private static final String METHOD_GET = "GET";
    private static final String COMPUTED_GET_HELLO = "GET /api/hello";

    @Test
    void extractVendorActionFromContentTypeWithSuffix() {
        final String token = RequestActionResolver.extractVendorAction(MEDIA_SJP_DELETE_FINANCIAL_MEANS);
        assertEquals("sjp.delete-financial-means", token, "Should parse vendor token with +json suffix");
    }

    @Test
    void extractVendorActionIsCaseInsensitiveAndLowerCased() {
        final String weirdCasing = "Application/VnD.HeArInG.GeT-DrAfT-ReSuLt+Json";
        final String token = RequestActionResolver.extractVendorAction(weirdCasing);
        assertEquals("hearing.get-draft-result", token, "Should lower-case and match case-insensitively");
    }

    @Test
    void extractVendorActionReturnsNullWhenNoVendor() {
        final String token = RequestActionResolver.extractVendorAction(MEDIA_JSON);
        assertNull(token, "No vendor token should be found in non-vendor media type");
    }

    @Test
    void extractFirstVendorFromHeaderListPicksFirstLeftToRight() {
        final String acceptHeader = "text/html, " + MEDIA_HEARING_GET_DRAFT_RESULT + ", " + MEDIA_SJP_DELETE_FINANCIAL_MEANS;
        final String token = RequestActionResolver.extractFirstVendorFromHeaderList(acceptHeader);
        assertEquals("hearing.get-draft-result", token, "Should pick the first vendor token left-to-right");
    }

    @Test
    void extractFirstVendorFromHeaderListReturnsNullWhenNoVendor() {
        final String acceptHeader = "text/html, " + MEDIA_JSON;
        final String token = RequestActionResolver.extractFirstVendorFromHeaderList(acceptHeader);
        assertNull(token, "Should return null when no vendor media types present");
    }

    @Test
    void resolvesPrefersContentTypeOverHeaderAndComputedName() {
        final MockHttpServletRequest request = new MockHttpServletRequest(METHOD_GET, PATH_HELLO);
        request.addHeader(HEADER_CONTENT_TYPE, MEDIA_HEARING_GET_DRAFT_RESULT);
        request.addHeader(HEADER_CPP_ACTION, COMPUTED_GET_HELLO);

        final RequestActionResolver.ResolvedAction resolved =
                RequestActionResolver.resolve(request, HEADER_CPP_ACTION, PATH_HELLO);

        assertEquals("hearing.get-draft-result", resolved.name(), "Content-Type vendor should win");
    }

    @Test
    void resolvesHeaderFlagFalseWhenContentTypeWins() {
        final MockHttpServletRequest request = new MockHttpServletRequest(METHOD_GET, PATH_HELLO);
        request.addHeader(HEADER_CONTENT_TYPE, MEDIA_HEARING_GET_DRAFT_RESULT);
        request.addHeader(HEADER_CPP_ACTION, COMPUTED_GET_HELLO);

        final RequestActionResolver.ResolvedAction resolved =
                RequestActionResolver.resolve(request, HEADER_CPP_ACTION, PATH_HELLO);

        assertFalse(resolved.headerSupplied(), "Header flag should be false when Content-Type wins");
    }

    @Test
    void resolvesVendorSuppliedTrueWhenFromContentType() {
        final MockHttpServletRequest request = new MockHttpServletRequest(METHOD_GET, PATH_HELLO);
        request.addHeader(HEADER_CONTENT_TYPE, MEDIA_SJP_DELETE_FINANCIAL_MEANS);

        final RequestActionResolver.ResolvedAction resolved =
                RequestActionResolver.resolve(request, HEADER_CPP_ACTION, PATH_HELLO);

        assertTrue(resolved.vendorSupplied(), "Vendor flag should be true when resolved from Content-Type");
    }

    @Test
    void resolvesAcceptUsedWhenNoContentTypeVendorName() {
        final MockHttpServletRequest request = new MockHttpServletRequest(METHOD_GET, PATH_HELLO);
        request.addHeader(HEADER_ACCEPT, MEDIA_SJP_DELETE_FINANCIAL_MEANS);

        final RequestActionResolver.ResolvedAction resolved =
                RequestActionResolver.resolve(request, HEADER_CPP_ACTION, PATH_HELLO);

        assertEquals("sjp.delete-financial-means", resolved.name(), "Should resolve from Accept when no Content-Type vendor");
    }

    @Test
    void resolvesVendorSuppliedTrueWhenFromAccept() {
        final MockHttpServletRequest request = new MockHttpServletRequest(METHOD_GET, PATH_HELLO);
        request.addHeader(HEADER_ACCEPT, MEDIA_HEARING_GET_DRAFT_RESULT);

        final RequestActionResolver.ResolvedAction resolved =
                RequestActionResolver.resolve(request, HEADER_CPP_ACTION, PATH_HELLO);

        assertTrue(resolved.vendorSupplied(), "Vendor flag should be true when resolved from Accept");
    }

    @Test
    void resolvesUsesExplicitHeaderWhenNoVendorPresentName() {
        final MockHttpServletRequest request = new MockHttpServletRequest(METHOD_GET, PATH_HELLO);
        request.addHeader(HEADER_CPP_ACTION, COMPUTED_GET_HELLO);
        request.addHeader(HEADER_CONTENT_TYPE, MEDIA_JSON);
        request.addHeader(HEADER_ACCEPT, MEDIA_JSON);

        final RequestActionResolver.ResolvedAction resolved =
                RequestActionResolver.resolve(request, HEADER_CPP_ACTION, PATH_HELLO);

        assertEquals(COMPUTED_GET_HELLO, resolved.name(), "Should use explicit header when no vendor present");
    }

    @Test
    void resolvesHeaderSuppliedTrueWhenExplicitHeaderPresent() {
        final MockHttpServletRequest request = new MockHttpServletRequest(METHOD_GET, PATH_HELLO);
        request.addHeader(HEADER_CPP_ACTION, COMPUTED_GET_HELLO);

        final RequestActionResolver.ResolvedAction resolved =
                RequestActionResolver.resolve(request, HEADER_CPP_ACTION, PATH_HELLO);

        assertTrue(resolved.headerSupplied(), "Header flag should be true when explicit header used");
    }

    @Test
    void resolvesComputedWhenNoVendorOrHeaderName() {
        final MockHttpServletRequest request = new MockHttpServletRequest(METHOD_GET, PATH_HELLO);
        request.addHeader(HEADER_CONTENT_TYPE, MEDIA_JSON);
        request.addHeader(HEADER_ACCEPT, MEDIA_JSON);

        final RequestActionResolver.ResolvedAction resolved =
                RequestActionResolver.resolve(request, HEADER_CPP_ACTION, PATH_HELLO);

        assertEquals(COMPUTED_GET_HELLO, resolved.name(), "Should compute '<METHOD> <PATH>' when no vendor/header");
    }

    @Test
    void resolvesVendorFlagFalseWhenComputed() {
        final MockHttpServletRequest request = new MockHttpServletRequest(METHOD_GET, PATH_HELLO);

        final RequestActionResolver.ResolvedAction resolved =
                RequestActionResolver.resolve(request, HEADER_CPP_ACTION, PATH_HELLO);

        assertFalse(resolved.vendorSupplied(), "Vendor flag should be false when computed");
    }

    @Test
    void resolvesHeaderFlagFalseWhenComputed() {
        final MockHttpServletRequest request = new MockHttpServletRequest(METHOD_GET, PATH_HELLO);

        final RequestActionResolver.ResolvedAction resolved =
                RequestActionResolver.resolve(request, HEADER_CPP_ACTION, PATH_HELLO);

        assertFalse(resolved.headerSupplied(), "Header flag should be false when computed");
    }

    @Test
    void resolveHandlesNullActionHeaderName() {
        final MockHttpServletRequest request = new MockHttpServletRequest(METHOD_GET, PATH_HELLO);
        // If header name is null, resolver must ignore any physical header present.
        request.addHeader(HEADER_CPP_ACTION, COMPUTED_GET_HELLO);

        final RequestActionResolver.ResolvedAction resolved =
                RequestActionResolver.resolve(request, null, PATH_HELLO);

        assertEquals(COMPUTED_GET_HELLO, resolved.name(), "Should compute '<METHOD> <PATH>' with null header name");
    }
}
