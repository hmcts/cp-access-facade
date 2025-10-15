package uk.gov.moj.cpp.authz.http;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RequestActionResolverTest {

    private static final String ACTION_HEADER = "CPP-ACTION";
    private static final String PATH_SAMPLE = "/some/path";
    private static final String METHOD_GET = "GET";
    private static final String METHOD_POST = "POST";
    private static final String VENDOR_SJP_DELETE = "sjp.delete-financial-means";
    private static final String VENDOR_HEARING_DRAFT = "hearing.get-draft-result";

    // --- extractVendorAction -------------------------------------------------

    @Test
    void extractVendorAction_fromContentTypeWithSuffix() {
        final String input = "application/vnd.sjp.delete-financial-means+json";
        final String found = RequestActionResolver.extractVendorAction(input);
        assertEquals(VENDOR_SJP_DELETE, found, "Should extract vendor token from Content-Type");
    }

    @Test
    void extractVendorAction_caseInsensitiveLowerCased() {
        final String input = "APPLICATION/VND.SJP.DELETE-FINANCIAL-MEANS+JSON";
        final String found = RequestActionResolver.extractVendorAction(input);
        assertEquals(VENDOR_SJP_DELETE, found, "Should normalize token to lowercase");
    }

    @Test
    void extractVendorAction_returnsNullWhenNoVendor() {
        final String input = "application/json";
        final String found = RequestActionResolver.extractVendorAction(input);
        assertEquals(null, found, "Should return null when no vendor token present");
    }

    // --- extractFirstVendorFromHeaderList (Accept) ---------------------------

    @Test
    void extractFirstVendorFromHeaderList_picksFirstVendorLeftToRight() {
        final String header = "application/json, application/vnd.hearing.get-draft-result+json;q=0.9, application/vnd.other+json";
        final String found = RequestActionResolver.extractFirstVendorFromHeaderList(header);
        assertEquals(VENDOR_HEARING_DRAFT, found, "Should pick first vendor token from Accept");
    }

    @Test
    void extractFirstVendorFromHeaderList_returnsNullWhenNoVendor() {
        final String header = "application/json, text/plain;q=0.5";
        final String found = RequestActionResolver.extractFirstVendorFromHeaderList(header);
        assertEquals(null, found, "Should return null when Accept has no vendor tokens");
    }

    // --- resolve() priority: Content-Type > Accept > Header > Computed -------

    @Test
    void resolve_prefersContentTypeOverHeaderAndComputed_name() {
        final MockHttpServletRequest req = new MockHttpServletRequest(METHOD_POST, PATH_SAMPLE);
        req.addHeader("Content-Type", "application/vnd.sjp.delete-financial-means+json");
        req.addHeader(ACTION_HEADER, "POST /ignored/by/content-type");

        final RequestActionResolver.ResolvedAction ra =
                RequestActionResolver.resolve(req, ACTION_HEADER, PATH_SAMPLE);

        assertEquals(VENDOR_SJP_DELETE, ra.name(), "Content-Type vendor token must win");
    }

    @Test
    void resolve_acceptUsedWhenNoContentTypeVendor_name() {
        final MockHttpServletRequest req = new MockHttpServletRequest(METHOD_GET, PATH_SAMPLE);
        req.addHeader("Accept", "application/json, application/vnd.hearing.get-draft-result+json");

        final RequestActionResolver.ResolvedAction ra =
                RequestActionResolver.resolve(req, ACTION_HEADER, PATH_SAMPLE);

        assertEquals(VENDOR_HEARING_DRAFT, ra.name(), "Accept vendor token must be used when no Content-Type token");
    }

    @Test
    void resolve_usesExplicitHeaderWhenNoVendor_name() {
        final MockHttpServletRequest req = new MockHttpServletRequest(METHOD_GET, PATH_SAMPLE);
        req.addHeader(ACTION_HEADER, "GET /explicit");

        final RequestActionResolver.ResolvedAction ra =
                RequestActionResolver.resolve(req, ACTION_HEADER, PATH_SAMPLE);

        assertEquals("GET /explicit", ra.name(), "Header-based action must be used when no vendor tokens");
    }

    @Test
    void resolve_computedWhenNoVendorOrHeader_name() {
        final MockHttpServletRequest req = new MockHttpServletRequest(METHOD_GET, PATH_SAMPLE);

        final RequestActionResolver.ResolvedAction ra =
                RequestActionResolver.resolve(req, ACTION_HEADER, PATH_SAMPLE);

        assertEquals(METHOD_GET + " " + PATH_SAMPLE, ra.name(), "Computed action must be method + path");
    }

    // --- resolve() flags -----------------------------------------------------

    @Test
    void resolve_flags_vendorSuppliedTrueWhenFromContentType() {
        final MockHttpServletRequest req = new MockHttpServletRequest(METHOD_POST, PATH_SAMPLE);
        req.addHeader("Content-Type", "application/vnd.sjp.delete-financial-means+json");

        final RequestActionResolver.ResolvedAction ra =
                RequestActionResolver.resolve(req, ACTION_HEADER, PATH_SAMPLE);

        assertEquals(true, ra.vendorSupplied(), "vendorSupplied must be true when resolved from vendor media");
    }

    @Test
    void resolve_flags_headerSuppliedFalseWhenFromVendor() {
        final MockHttpServletRequest req = new MockHttpServletRequest(METHOD_POST, PATH_SAMPLE);
        req.addHeader("Content-Type", "application/vnd.sjp.delete-financial-means+json");
        req.addHeader(ACTION_HEADER, "POST /ignored");

        final RequestActionResolver.ResolvedAction ra =
                RequestActionResolver.resolve(req, ACTION_HEADER, PATH_SAMPLE);

        assertEquals(false, ra.headerSupplied(), "headerSupplied must be false when vendor wins");
    }

    @Test
    void resolve_flags_vendorSuppliedTrueWhenFromAccept() {
        final MockHttpServletRequest req = new MockHttpServletRequest(METHOD_GET, PATH_SAMPLE);
        req.addHeader("Accept", "application/vnd.hearing.get-draft-result+json");

        final RequestActionResolver.ResolvedAction ra =
                RequestActionResolver.resolve(req, ACTION_HEADER, PATH_SAMPLE);

        assertEquals(true, ra.vendorSupplied(), "vendorSupplied must be true when resolved from Accept");
    }

    @Test
    void resolve_flags_headerSuppliedTrueWhenFromHeader() {
        final MockHttpServletRequest req = new MockHttpServletRequest(METHOD_GET, PATH_SAMPLE);
        req.addHeader(ACTION_HEADER, "GET /from-header");

        final RequestActionResolver.ResolvedAction ra =
                RequestActionResolver.resolve(req, ACTION_HEADER, PATH_SAMPLE);

        assertEquals(true, ra.headerSupplied(), "headerSupplied must be true when resolved from header");
    }

    @Test
    void resolve_flags_vendorAndHeaderFalseWhenComputed() {
        final MockHttpServletRequest req = new MockHttpServletRequest(METHOD_GET, PATH_SAMPLE);

        final RequestActionResolver.ResolvedAction ra =
                RequestActionResolver.resolve(req, ACTION_HEADER, PATH_SAMPLE);

        assertEquals(false, ra.vendorSupplied() || ra.headerSupplied(),
                "Both flags must be false when using computed action");
    }

    // --- null-safety ---------------------------------------------------------

    @Test
    void resolve_handlesNullActionHeaderName() {
        final MockHttpServletRequest req = new MockHttpServletRequest(METHOD_GET, PATH_SAMPLE);

        final RequestActionResolver.ResolvedAction ra =
                RequestActionResolver.resolve(req, null, PATH_SAMPLE);

        assertEquals(METHOD_GET + " " + PATH_SAMPLE, ra.name(), "Should compute action when header name is null");
    }
}
