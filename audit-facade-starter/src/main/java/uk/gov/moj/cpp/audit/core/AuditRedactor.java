package uk.gov.moj.cpp.audit.core;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import uk.gov.moj.cpp.audit.config.HttpAuditProperties;

import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AuditRedactor {
    private static final String REDACT = "██REDACTED██";
    private final HttpAuditProperties props;

    public Map<String, String> redactHeaders(HttpServletRequest req) {
        var out = new LinkedHashMap<String, String>();
        var sensitive = props.getRedactHeaders().stream()
                .map(String::toLowerCase).collect(Collectors.toSet());
        Enumeration<String> names = req.getHeaderNames();
        while (names != null && names.hasMoreElements()) {
            String name = names.nextElement();
            String value = req.getHeader(name);
            out.put(name, sensitive.contains(name.toLowerCase()) ? REDACT : value);
        }
        return out;
    }

    public String redactBody(@Nullable String body) {
        if (body == null) return null;
        String truncated = body.length() > props.getMaxBodyBytes()
                ? body.substring(0, props.getMaxBodyBytes()) : body;
        for (String f : props.getRedactJsonFields()) {
            truncated = truncated.replaceAll("(\"" + Pattern.quote(f) + "\" *: *\")(.*?)(\")", "$1" + REDACT + "$3");
        }
        return truncated;
    }
}
