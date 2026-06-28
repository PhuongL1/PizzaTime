---
applyTo: "**/*.java"
---

# Java — Coding Conventions

## Style

- Indentation: 4 spaces
- Naming: camelCase (methods/fields), PascalCase (classes/interfaces), UPPER_SNAKE_CASE (constants)
- Max line length: 120
- Braces: K&R style (opening brace on same line)
- Trailing commas: no

## Patterns

- Prefer composition over inheritance
- Use interfaces for abstraction — program to interface, not implementation
- Use `Optional<T>` for nullable returns — never return `null` from public methods
- Use `final` on local variables and parameters when practical
- Avoid raw types — always parameterize generics
- Use `Objects.requireNonNull()` at method entry for early validation

## File structure

- One public class per file
- Order: static fields → instance fields → constructors → public methods → private methods
- Group related methods together

## Testing

- Naming: `shouldDoX_whenY()` or `givenX_whenY_thenZ()`
- Use JUnit 5 (`@Test`, `@BeforeEach`, `@Nested`)
- Mocking: Mockito (`@Mock`, `@InjectMocks`, `when().thenReturn()`)
- Assertions: AssertJ preferred (`assertThat(x).isEqualTo(y)`)

## Notes

- Java version: 17+ — use records, sealed classes, pattern matching where appropriate
- Use `record` for immutable DTOs (instead of POJO with getters/setters)
- Use `sealed interface` for closed type hierarchies (Java 17+)
- Use text blocks (`"""..."""`) for multi-line strings
- Avoid checked exceptions for business logic — use unchecked + global handler
