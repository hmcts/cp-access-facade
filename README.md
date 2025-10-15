# CP Access Facade — AuthZ & Audit Starters + Demo Service

This repository contains three Gradle modules:

- **`authz-facade-starter`** – A Spring Boot auto-configuration that plugs an HTTP authorization filter in front of your endpoints. It resolves an *action* from the incoming request (vendor media type, header, or method+path), fetches the caller’s groups from an identity endpoint, and evaluates Drools rules to allow/deny.
- **`audit-facade-starter`** – A lightweight audit publisher/consumer foundation (Artemis/JMS). The demo service uses it to emit audit events.
- **`access-facade-demo-service`** – A runnable Spring Boot app that wires the starters and exposes minimal endpoints for manual testing and integration tests.

> Requires **Java 21** and **Gradle 8+**.
>
> All commands below use **system Gradle** (`gradle`) — there is no wrapper dependency.

---

## Quick start

```bash
# Ensure Java 21 is active
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
java -version

# Build all modules (runs unit tests, PMD, etc.)
gradle clean build

# Run the demo service
gradle :access-facade-demo-service:bootRun
```

When the demo is running, try:

```bash
# Allow by group via computed action ("GET /api/hello")
curl -v -H 'CJSCPPUID: la-user-1' http://localhost:8080/api/hello

# Permit via vendor media type action (highest priority)
# Action resolved to: hearing.get-draft-result
curl -v \
  -H 'CJSCPPUID: 5d35a9ac-e1f6-4f8e-9cc9-8184cf9fdb2d' \
  -H 'Accept: application/vnd.hearing.get-draft-result+json' \
  'http://localhost:8080/hearing-query-api/query/api/rest/hearing/hearings/4d35a9ac-e1f6-4f8e-9cc9-8184cf9fdb2d/draft-result'

# Vendor action from Content-Type on POST (highest priority)
# Action resolved to: sjp.delete-financial-means
curl -v -X POST \
  -H 'CJSCPPUID: 5d35a9ac-e1f6-4f8e-9cc9-8184cf9fdb2d' \
  -H 'Content-Type: application/vnd.sjp.delete-financial-means+json' \
  --data '{}' \
  'http://localhost:8080/sjp-command-api/command/api/rest/sjp/cases/1d35a9ac-e1f6-4f8e-9cc9-8184cf9fdb2d/defendant/2d35a9ac-e1f6-4f8e-9cc9-8184cf9fdb2d/financial-means'

# Explicit header action (second priority)
curl -v \
  -H 'CJSCPPUID: la-user-1' \
  -H 'CPP-ACTION: GET /api/hello' \
  http://localhost:8080/api/hello
```

---

## How authorization works

### Request → Action resolution (priority order)

The filter determines the action name using `RequestActionResolver`:

1. **Vendor media type (highest priority)**
    - If **`Content-Type`** (for requests with bodies) contains a vendor type `application/vnd.<vendor>+json`, then **action = `<vendor>`** (e.g., `sjp.delete-financial-means`).
    - Else if **`Accept`** contains a vendor type, **action = `<vendor>`** (e.g., `hearing.get-draft-result`).
    - First vendor in a comma-separated list wins.
2. **Explicit header (`CPP-ACTION`)**
    - If present, **action = header value** (e.g., `GET /api/hello`).
3. **Computed fallback**
    - **action = `"<METHOD> <PATH>"`** (e.g., `GET /api/hello`).

The filter also populates action **attributes**:
- `method`: HTTP method
- `path`: path within application (no scheme/host/query)

### Identity → Groups

`IdentityClient` calls an identity endpoint to obtain the caller’s groups. The default mapper (`DefaultIdentityToGroupsMapper`) converts the JSON response into a `Set<String>` of group names (e.g., “Legal Advisers”, “Prosecuting Authority Access”).

### Drools evaluation

`DroolsAuthzEngine` loads `.drl` files from the classpath (configurable pattern), sets a global `UserAndGroupProvider`, inserts the `Action` and a mutable `Outcome`, and fires the rules. If any rule sets `Outcome.success = true`, access is **allowed**; otherwise **denied**.

---

## Configuration (application.yml)

The demo app ships with sensible defaults. To integrate elsewhere, add a section like this:

```yaml
authz:
  http:
    enabled: true
    user-id-header: "CJSCPPUID"
    action-header: "CPP-ACTION"
    accept-header: "application/vnd.usersgroups.get-logged-in-user-permissions+json"

    # Identity endpoint: resolves groups for the logged-in user.
    # The demo controller serves this locally for convenience.
    identity-url-template: "http://localhost:${server.port}/usersgroups-query-api/query/api/rest/usersgroups/users/logged-in-user/permissions"

    # DRL discovery (package → folder alignment recommended)
    drools-classpath-pattern: "classpath*:/uk/gov/moj/cpp/authz/demo/*.drl"

    # Dev ergonomics
    reload-on-each-request: false
    action-required: false   # set true if CPP-ACTION must be present when no vendor media type
    deny-when-no-rules: true

    # Requests bypassing the filter entirely
    exclude-path-prefixes:
      - "/usersgroups-query-api/"
      - "/actuator/"

    # Optional aliases
    group-aliases:
      "Legal Advisers": "Legal Advisers"
      "Prosecuting Authority Access": "Prosecuting Authority Access"

    # Servlet filter order (higher precedence runs earlier)
    filter-order: 30
```

> **Tip:** To avoid Drools package warnings, put your `.drl` under a folder that mirrors its `package` declaration, e.g.  
> `src/main/resources/uk/gov/moj/cpp/authz/demo/demo-rules.drl` with `package uk.gov.moj.cpp.authz.demo`.

---

## Example DRL

```drools
package uk.gov.moj.cpp.authz.demo

import uk.gov.moj.cpp.authz.drools.Outcome;
import uk.gov.moj.cpp.authz.drools.Action;

global uk.gov.moj.cpp.authz.http.providers.UserAndGroupProvider userAndGroupProvider;

// 1) Computed/header action
rule "Allow GET /api/hello for LA or PA"
when
  $o: Outcome()
  $a: Action(name == "GET /api/hello")
  eval(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups($a, "Legal Advisers", "Prosecuting Authority Access"))
then
  $o.setSuccess(true);
end

// 2) Vendor action from Accept (GET) or Content-Type (POST)
rule "Allow hearing.get-draft-result for LA"
when
  $o: Outcome()
  $a: Action(name == "hearing.get-draft-result")
  eval(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups($a, "Legal Advisers"))
then
  $o.setSuccess(true);
end

rule "Allow sjp.delete-financial-means for LA"
when
  $o: Outcome()
  $a: Action(name == "sjp.delete-financial-means")
  eval(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups($a, "Legal Advisers"))
then
  $o.setSuccess(true);
end
```

---

## Building, testing, linting

```bash
# Build everything (compiles, tests)
gradle clean build

# Run tests for all modules
gradle test

# Run tests for a single module
gradle :authz-facade-starter:test

# PMD (main + test) for all modules
gradle pmdMain pmdTest

# PMD for one module
gradle :authz-facade-starter:pmdMain :authz-facade-starter:pmdTest
```

### Java 21

Make sure Java 21 is the active runtime:

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
gradle --version
```

---

## Project layout

```
cp-access-facade/
├─ access-facade-demo-service/       # Demo Spring Boot app
│  └─ src/main/resources/
│     └─ application.yml             # Demo properties
├─ authz-facade-starter/             # Authorization starter
│  ├─ src/main/java/uk/gov/moj/cpp/authz/
│  │  ├─ http/                       # Filter, Identity client, resolver, contracts
│  │  └─ drools/                     # Engine, models
│  └─ src/main/resources/uk/gov/moj/cpp/authz/demo/
│     └─ demo-rules.drl              # Example rules
└─ audit-facade-starter/             # Audit starter
```

---

## Troubleshooting

- **401 Unauthorized** – Missing user id header. Ensure `CJSCPPUID` is present:  
  `curl -H 'CJSCPPUID: some-user' ...`

- **400 Bad Request** – If `authz.http.action-required=true`, the filter expects `CPP-ACTION` when no vendor media type is present.

- **403 Forbidden** – Drools denied. Confirm:
    - The user belongs to the required groups
    - The action name resolved as expected (vendor/header/computed)
    - Your `.drl` rules allow it

- **404 Not Found (identity endpoint)** – The demo identity endpoint lives under  
  `/usersgroups-query-api/query/api/rest/usersgroups/users/logged-in-user/permissions`.  
  If you changed the port or path, update `identity-url-template`.

- **Drools warnings about package/folder alignment** – Move your `.drl` under a path that mirrors its `package`.

- **MVEL / JIT / parser quirks** in tests – The test suite sets:
  ```
  -Dmvel2.disable.jit=true
  -Ddrools.compiler=ECLIPSE
  -Ddrools.dialect.default=java
  ```
  If you run tests externally, keep these system properties (already handled in the JUnit setup).

---

## Security model recap

1. The filter can be bypassed for configured path prefixes.
2. Otherwise it resolves the **action** (vendor → header → computed).
3. It fetches the caller’s groups from the identity endpoint.
4. Drools decides allow/deny. No match → deny (configurable).

---

## License

MIT (or your organizational license).

---

## Support

Open an issue in your internal tracker with:
- Request URL and method
- Headers (`CJSCPPUID`, `CPP-ACTION`, `Content-Type`, `Accept`)
- Resolved action (log snippet, if available)
- Groups returned by identity service
- Active DRL rule set and classpath pattern