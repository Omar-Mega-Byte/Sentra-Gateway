# Java Concept Explanations For Sentra Gateway

This document expands the Java concepts used across the Sentra Gateway backend. Every item includes the problem it solves, one small Java example, and one common mistake.

## 1. Class

**Problem it solves:** A class groups data and behavior into one reusable type. In this project, services such as `ProfileService`, `OrderService`, and `PaymentService` use classes to keep business logic in one place.

**Java example:**

```java
public class PaymentService {
    public String capture(String reference) {
        return "Captured payment " + reference;
    }
}
```

**Common mistake:** Putting every method into one huge class. A class should have a clear responsibility, not become a dumping ground for unrelated logic.

## 2. Object

**Problem it solves:** An object is a real instance of a class with its own state. You use objects when the program needs actual working values, not just a type definition.

**Java example:**

```java
PaymentService service = new PaymentService();
String result = service.capture("ui-acme-order-1002");
```

**Common mistake:** Confusing the class with the object. `PaymentService` is the blueprint; `service` is the instance you can call.

## 3. Constructor

**Problem it solves:** A constructor creates an object in a valid initial state. It is commonly used to require dependencies or required values up front.

**Java example:**

```java
public class OrderService {
    private final OrderRepository repository;

    public OrderService(OrderRepository repository) {
        this.repository = repository;
    }
}
```

**Common mistake:** Allowing an object to be created with missing required dependencies, then failing later with a null pointer.

## 4. Encapsulation

**Problem it solves:** Encapsulation protects internal state by exposing controlled methods instead of letting other code change fields directly.

**Java example:**

```java
public class RateCounter {
    private int remaining = 10;

    public boolean consume() {
        if (remaining <= 0) {
            return false;
        }
        remaining--;
        return true;
    }
}
```

**Common mistake:** Making fields public and letting any caller mutate them, which makes bugs hard to trace.

## 5. Access Modifiers

**Problem it solves:** Access modifiers control what other code can see. They help keep APIs small and implementation details hidden.

**Java example:**

```java
public class AuditService {
    public void record(String decision) {
        String normalized = normalize(decision);
    }

    private String normalize(String value) {
        return value.trim().toUpperCase();
    }
}
```

**Common mistake:** Making helper methods `public` just because tests or another class want them. Prefer testing behavior through the public API.

## 6. `final`

**Problem it solves:** `final` prevents reassignment. It makes code easier to reason about, especially for dependencies and immutable values.

**Java example:**

```java
public class ProfileService {
    private final ProfileRepository repository;

    public ProfileService(ProfileRepository repository) {
        this.repository = repository;
    }
}
```

**Common mistake:** Thinking `final` makes an object deeply immutable. `final List<String> roles` means the variable cannot point to another list, but the list contents may still be mutable.

## 7. Interface

**Problem it solves:** An interface defines what a dependency can do without forcing callers to know how it is implemented. This is why repositories can have in-memory and database implementations.

**Java example:**

```java
public interface ProfileRepository {
    Optional<UserProfile> findBySubject(String tenantId, String subject);
}
```

**Common mistake:** Adding too many methods to one interface. A focused interface is easier to implement, test, and replace.

## 8. Implementation

**Problem it solves:** An implementation provides the actual behavior promised by an interface.

**Java example:**

```java
public class InMemoryProfileRepository implements ProfileRepository {
    private final Map<String, UserProfile> profiles = new HashMap<>();

    @Override
    public Optional<UserProfile> findBySubject(String tenantId, String subject) {
        return Optional.ofNullable(profiles.get(tenantId + ":" + subject));
    }
}
```

**Common mistake:** Depending on the concrete class everywhere. Prefer depending on the interface so the implementation can change.

## 9. Inheritance

**Problem it solves:** Inheritance lets one class reuse or specialize another class. It is useful for framework extension points and exception hierarchies.

**Java example:**

```java
public class GatewayException extends RuntimeException {
    public GatewayException(String message) {
        super(message);
    }
}
```

**Common mistake:** Using inheritance only to share code. If the relationship is not really "is a", composition is usually cleaner.

## 10. Composition

**Problem it solves:** Composition builds behavior by giving one object references to other objects. This is the normal service style in this backend.

**Java example:**

```java
public class OrderService {
    private final OrderRepository repository;
    private final OrderValidator validator;

    public OrderService(OrderRepository repository, OrderValidator validator) {
        this.repository = repository;
        this.validator = validator;
    }
}
```

**Common mistake:** Creating dependencies inside the class with `new` when they should be passed in. That makes testing and replacement harder.

## 11. Enum

**Problem it solves:** An enum restricts a value to a known set. This is safer than passing arbitrary strings for statuses, actions, and categories.

**Java example:**

```java
public enum PaymentStatus {
    AUTHORIZED,
    CAPTURED,
    DECLINED,
    REFUNDED
}
```

**Common mistake:** Parsing user input with `PaymentStatus.valueOf(value)` without handling bad casing or unknown values.

## 12. Record

**Problem it solves:** A record creates a compact immutable data carrier. This is useful for DTOs like request bodies, responses, and domain snapshots.

**Java example:**

```java
public record ErrorDetail(String field, String code, String message) {}
```

**Common mistake:** Putting complex mutable business behavior into records. Records are best when they mainly represent data.

## 13. Generics

**Problem it solves:** Generics let one class or method work with many types while keeping compile-time type safety.

**Java example:**

```java
public record PageResponse<T>(int page, int size, List<T> items) {}
```

**Common mistake:** Using raw types like `List items`. That loses type safety and pushes errors to runtime.

## 14. `List`

**Problem it solves:** `List` stores ordered values and allows duplicates. It is useful for request items, route methods, scopes, and response rows.

**Java example:**

```java
List<String> scopes = List.of("orders:read", "orders:write");
String first = scopes.get(0);
```

**Common mistake:** Assuming `List.of(...)` is mutable. It is immutable; calling `add` on it throws `UnsupportedOperationException`.

## 15. `Set`

**Problem it solves:** `Set` stores unique values. It is useful when duplicates do not make sense, such as roles or scopes after normalization.

**Java example:**

```java
Set<String> roles = new HashSet<>();
roles.add("USER_ADMIN");
roles.add("USER_ADMIN");
System.out.println(roles.size()); // 1
```

**Common mistake:** Expecting a normal `HashSet` to preserve insertion order. Use `LinkedHashSet` if order matters.

## 16. `Map`

**Problem it solves:** `Map` stores key-value pairs. It is useful for in-memory repositories, headers, metadata, and lookup tables.

**Java example:**

```java
Map<String, String> headers = new HashMap<>();
headers.put("X-Request-Id", "ui-smoke-001");
String requestId = headers.get("X-Request-Id");
```

**Common mistake:** Calling `map.get(key).trim()` without checking if the key exists. `get` can return `null`.

## 17. `Optional`

**Problem it solves:** `Optional` makes a missing value explicit in method returns. Repositories use it to say "not found" without returning `null`.

**Java example:**

```java
Optional<UserProfile> profile = repository.findBySubject("tenant-demo", "sentra-user-omar");
String name = profile.map(UserProfile::displayName).orElse("Unknown user");
```

**Common mistake:** Calling `optional.get()` without checking `isPresent()`. Prefer `map`, `orElse`, `orElseThrow`, or `ifPresent`.

## 18. Exception

**Problem it solves:** Exceptions stop normal execution when a request cannot be processed. Custom exceptions let the API return consistent error codes.

**Java example:**

```java
if (amount.signum() <= 0) {
    throw new PaymentServiceException("Amount must be positive");
}
```

**Common mistake:** Catching every exception and returning a vague error. Preserve meaningful error codes and messages.

## 19. Runtime Exception

**Problem it solves:** A runtime exception represents a programming or business failure that callers are not forced to catch at compile time.

**Java example:**

```java
public class OrderServiceException extends RuntimeException {
    public OrderServiceException(String message) {
        super(message);
    }
}
```

**Common mistake:** Using runtime exceptions for expected control flow instead of clear validation results.

## 20. Try-With-Resources

**Problem it solves:** Try-with-resources closes resources automatically, even when an exception happens.

**Java example:**

```java
try (InputStream input = Files.newInputStream(Path.of("config.json"))) {
    byte[] bytes = input.readAllBytes();
}
```

**Common mistake:** Manually closing resources only on the success path, which leaks files, sockets, or streams on failure.

## 21. Immutability

**Problem it solves:** Immutable values cannot be changed after creation, which reduces accidental mutation and makes concurrent code safer.

**Java example:**

```java
public record ApiKeyPrincipal(String clientId, List<String> scopes) {
    public ApiKeyPrincipal {
        scopes = List.copyOf(scopes);
    }
}
```

**Common mistake:** Storing a caller-provided mutable list directly inside an object. Use defensive copies for collections.

## 22. `BigDecimal`

**Problem it solves:** `BigDecimal` represents money precisely. It avoids floating-point rounding errors.

**Java example:**

```java
BigDecimal amount = new BigDecimal("125.50");
BigDecimal tax = amount.multiply(new BigDecimal("0.14"));
```

**Common mistake:** Creating money values with `new BigDecimal(125.50)`. Use strings because doubles are already imprecise.

## 23. `UUID`

**Problem it solves:** `UUID` represents globally unique identifiers, which are useful for resources such as users, orders, payments, and audit events.

**Java example:**

```java
UUID paymentId = UUID.fromString("40000000-0000-4000-8000-000000000001");
```

**Common mistake:** Accepting arbitrary strings as IDs and only failing later. Parse and validate UUIDs at the boundary.

## 24. `Instant`

**Problem it solves:** `Instant` stores a precise UTC timestamp. It is safer for APIs and audit logs than local date-time values.

**Java example:**

```java
Instant expiresAt = Instant.parse("2026-12-31T23:59:59Z");
boolean expired = Instant.now().isAfter(expiresAt);
```

**Common mistake:** Mixing local time zones into persisted security timestamps. Store UTC instants and format for users at the edge.

## 25. `Duration`

**Problem it solves:** `Duration` represents elapsed time clearly, such as replay windows, retention periods, and timeout settings.

**Java example:**

```java
Duration replayWindow = Duration.ofMinutes(5);
Instant expiresAt = Instant.now().plus(replayWindow);
```

**Common mistake:** Passing naked numbers like `300` without saying whether they are milliseconds, seconds, or minutes.

## 26. Streams

**Problem it solves:** Streams process collections declaratively with operations like `filter`, `map`, and `toList`.

**Java example:**

```java
List<String> activeRouteIds = routes.stream()
        .filter(GatewayRoute::enabled)
        .map(GatewayRoute::id)
        .toList();
```

**Common mistake:** Making stream chains so clever that they are harder to read than a simple loop.

## 27. Lambda

**Problem it solves:** A lambda passes small behavior as a value. It is often used with streams, callbacks, and functional interfaces.

**Java example:**

```java
List<String> upper = scopes.stream()
        .map(scope -> scope.toUpperCase())
        .toList();
```

**Common mistake:** Capturing mutable state inside lambdas and then being surprised by ordering or concurrency behavior.

## 28. Method Reference

**Problem it solves:** A method reference is a shorter way to pass an existing method where a lambda is expected.

**Java example:**

```java
List<String> ids = routes.stream()
        .map(GatewayRoute::id)
        .toList();
```

**Common mistake:** Using method references when they hide too much context. A clear lambda is better when arguments need explanation.

## 29. Annotation

**Problem it solves:** An annotation attaches metadata to code. Frameworks and validators use this metadata to enforce behavior without manual boilerplate.

**Java example:**

```java
public record CreatePaymentRequest(
        @NotBlank String merchantReference,
        @NotBlank String amount,
        @NotBlank String currency) {
}
```

**Common mistake:** Thinking annotations always run by themselves. Validation annotations need a validator integration, such as Spring validation.

## 30. Bean Validation

**Problem it solves:** Bean Validation declares request rules near the DTO fields, such as required text, maximum size, and future dates.

**Java example:**

```java
public record KeyRequest(
        @Size(max = 50) List<@NotBlank String> scopes,
        @Future Instant expiresAt) {
}
```

**Common mistake:** Validating only at the database layer. API boundaries should reject invalid input before business logic runs.

## 31. Static Method

**Problem it solves:** A static method belongs to the class instead of an object. It is useful for pure helpers and factories that do not need object state.

**Java example:**

```java
public final class RequestIds {
    public static String next(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }
}
```

**Common mistake:** Making everything static. Heavy static usage makes testing and dependency replacement harder.

## 32. Method Overloading

**Problem it solves:** Overloading allows methods with the same name but different parameter lists.

**Java example:**

```java
public class AuditService {
    public void record(String decision) {
        record(decision, null);
    }

    public void record(String decision, String routeId) {
        // persist audit event
    }
}
```

**Common mistake:** Creating overloads that do very different things. Same name should mean same concept.

## 33. Equality

**Problem it solves:** Equality decides whether two values should be treated as the same logical value. Records generate sensible equality automatically.

**Java example:**

```java
record RouteKey(String routeId, String method) {}

RouteKey a = new RouteKey("user-profile-read", "GET");
RouteKey b = new RouteKey("user-profile-read", "GET");
System.out.println(a.equals(b)); // true
```

**Common mistake:** Comparing strings with `==`. Use `equals` because `==` checks whether both references point to the same object.

## 34. Null Handling

**Problem it solves:** Careful null handling prevents unexpected `NullPointerException` failures.

**Java example:**

```java
String normalized = value == null ? "" : value.trim();
```

**Common mistake:** Passing null through many layers and only discovering it far from the source. Normalize or reject null at boundaries.

## 35. Synchronization

**Problem it solves:** `synchronized` protects shared mutable state so only one thread can execute a critical section at a time.

**Java example:**

```java
public class InMemoryCounter {
    private int value;

    public synchronized int increment() {
        value++;
        return value;
    }
}
```

**Common mistake:** Synchronizing one method but reading the same mutable field elsewhere without synchronization.

## 36. Defensive Copy

**Problem it solves:** Defensive copies stop callers from mutating data after an object has accepted it.

**Java example:**

```java
public record GatewayRoute(String id, List<String> methods) {
    public GatewayRoute {
        methods = List.copyOf(methods);
    }
}
```

**Common mistake:** Returning internal mutable collections directly from getters or accessors.

## 37. Package

**Problem it solves:** Packages organize related classes and avoid name collisions. The project separates routing, security, audit, request handling, and domain code into packages.

**Java example:**

```java
package com.omar.sentra.gateway.routing;

public record GatewayRoute(String id) {}
```

**Common mistake:** Creating vague packages like `utils` or `misc` that hide ownership and responsibility.

## 38. Import

**Problem it solves:** Imports let a class refer to another type by its short name instead of the full package name.

**Java example:**

```java
import java.util.Optional;

Optional<String> value = Optional.of("tenant-demo");
```

**Common mistake:** Using wildcard imports everywhere. Explicit imports make dependencies clearer and reduce name conflicts.

## 39. Unit Test

**Problem it solves:** A unit test checks a small piece of logic quickly. This is useful for validators, parsers, matchers, and in-memory repositories.

**Java example:**

```java
@Test
void rejectsNegativeAmount() {
    AmountValidator validator = new AmountValidator();

    assertThrows(IllegalArgumentException.class,
            () -> validator.validate("-1.00"));
}
```

**Common mistake:** Testing only the happy path. Validation code should test invalid and edge-case inputs too.

## 40. Dependency Injection

**Problem it solves:** Dependency injection supplies dependencies from the outside. It keeps services testable and avoids hardcoded construction.

**Java example:**

```java
public class PaymentService {
    private final PaymentRepository repository;

    public PaymentService(PaymentRepository repository) {
        this.repository = repository;
    }
}
```

**Common mistake:** Calling `new JdbcPaymentRepository()` inside `PaymentService`. That couples business logic to one storage implementation.

## 41. Spring Controller

**Problem it solves:** A controller maps HTTP requests to Java methods. It is the boundary between external API traffic and internal service logic.

**Java example:**

```java
@RestController
class ProfileController {
    record ProfileResponse(String subject, String tenantId) {}

    @GetMapping("/api/v1/users/me")
    ProfileResponse me() {
        return new ProfileResponse("sentra-user-omar", "tenant-demo");
    }
}
```

**Common mistake:** Putting business rules directly in controllers. Controllers should parse, authorize, and delegate.

## 42. Spring Service

**Problem it solves:** A service holds business logic that should be reusable across controllers, jobs, or tests.

**Java example:**

```java
@Service
public class ProfileService {
    public ProfileResponse currentProfile(String tenantId, String subject) {
        // load and return profile
    }

    public record ProfileResponse(String tenantId, String subject) {}
}
```

**Common mistake:** Treating service classes as thin pass-through wrappers with no clear responsibility.

## 43. Repository Pattern

**Problem it solves:** A repository hides storage details from business logic. The service asks for profiles, orders, or payments without knowing whether data is in memory or in a database.

**Java example:**

```java
public interface OrderRepository {
    Optional<Order> findById(UUID id);
    Order save(Order order);
}
```

**Common mistake:** Returning database-specific objects from repositories, which leaks persistence details into business code.

## 44. DTO

**Problem it solves:** A DTO carries data across a boundary, such as an HTTP request or response. It keeps API shapes separate from internal domain objects.

**Java example:**

```java
public record CreateOrderRequest(List<CreateOrderItemRequest> items) {}
public record CreateOrderItemRequest(String sku, int quantity) {}
```

**Common mistake:** Reusing database entities as API DTOs. That can expose internal fields and make API changes risky.

## 45. Reactive `Mono`

**Problem it solves:** `Mono<T>` represents an asynchronous result that will produce zero or one value. The gateway uses it because Spring Cloud Gateway is reactive.

**Java example:**

```java
public Mono<GatewayRoute> find(String id) {
    return repository.findById(id)
            .switchIfEmpty(Mono.error(new GatewayException("Route not found")));
}
```

**Common mistake:** Calling `block()` inside reactive request handling. That can hurt scalability and deadlock under load.

## 46. Reactive `Flux`

**Problem it solves:** `Flux<T>` represents an asynchronous stream of zero or many values. It fits list endpoints such as routes, policies, and audit events.

**Java example:**

```java
public Flux<GatewayRoute> listRoutes() {
    return repository.findAll()
            .filter(GatewayRoute::enabled);
}
```

**Common mistake:** Treating `Flux` like an already-loaded `List`. Reactive streams are lazy until subscribed by the framework.

## 47. Idempotency

**Problem it solves:** Idempotency lets clients safely retry mutation requests without creating duplicate orders, payments, or refunds.

**Java example:**

```java
public record IdempotencyRequest(String key, String fingerprint) {}

if (store.contains(key, fingerprint)) {
    return store.replay(key);
}
```

**Common mistake:** Keying idempotency only by request ID and ignoring the request body fingerprint. The same key with a different body should be rejected.

## 48. Optimistic Versioning

**Problem it solves:** Optimistic versioning prevents lost updates when two clients edit the same resource.

**Java example:**

```java
public record ProfileLifecycleRequest(int version) {}

if (current.version() != request.version()) {
    throw new UserServiceException("Version conflict");
}
```

**Common mistake:** Updating a record without checking the expected version. The last write silently wins and may overwrite another user's change.

## 49. Request Context

**Problem it solves:** A request context carries trusted identity, tenant, roles, scopes, and route data through the service. This avoids reparsing security headers everywhere.

**Java example:**

```java
public record TrustedRequestContext(
        String requestId,
        String subject,
        String tenantId,
        List<String> roles,
        List<String> scopes) {
}
```

**Common mistake:** Trusting client-supplied identity headers directly on public service ports. Only the gateway should be allowed to provide trusted context.

## 50. Validation Before Persistence

**Problem it solves:** Validating before persistence keeps bad data out of repositories and gives clients clear API errors.

**Java example:**

```java
if (request.items() == null || request.items().isEmpty()) {
    throw new OrderServiceException("Order must contain at least one item");
}
```

**Common mistake:** Letting invalid data reach the database and relying on generic SQL errors as API responses.
