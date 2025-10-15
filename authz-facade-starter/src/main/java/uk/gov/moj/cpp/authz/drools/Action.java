package uk.gov.moj.cpp.authz.drools;

import java.util.Map;
import java.util.Objects;

public record Action(String name, Map<String, Object> attributes) {
    public Action(final String name, final Map<String, Object> attributes) {
        this.name = Objects.requireNonNull(name, "name");
        this.attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }

    public String getName() {
        return name;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }
}
