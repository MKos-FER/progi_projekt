# Repository guidance

## Project and scope

Let's Do Sports is a student project. Prefer clear, small changes that the team can understand and explain. Implement the requested behavior using the existing stack; avoid speculative features, unnecessary dependencies, and abstractions with no concrete use.

- `backend/LetsDoSports/`: Spring Boot application, Maven, Java 23; see `pom.xml` for dependency versions.
- `frontend/`: React, TypeScript, Vite, and Bun.
- `db/`: PostgreSQL, Docker Compose, Flyway migrations, and development seed data.
- `docs/`: course documentation and requirements. Read the relevant requirements before changing domain behavior; distinguish proposals from implemented features.
- Read `backend/README.md`, `db/README.md`, or `frontend/README.md` as relevant for setup and integration details.

## Working conventions

- Inspect the affected code and existing tests before editing. Preserve unrelated work, including untracked files.
- Follow existing naming and formatting. Use English Java identifiers; keep project documentation concise and consistent with its existing Croatian language.
- Keep changes focused. Do not introduce microservices, generic base services/repositories, mapping frameworks, or interfaces for every class. Add an abstraction when it serves an actual boundary or repeated need.
- Keep the current dependencies and versions unless the task requires a change. Prefer existing Spring and Java facilities over a new library.
- Update relevant documentation when behavior, configuration, API contracts, or schema changes. Describe only what is implemented and verified.

## Spring Boot practices

- Keep code under `hr.fer.letsdosports`, organized by feature like the existing `user` package. Use small `api`, `model`, or other subpackages when useful; avoid reorganizing unrelated features.
- Keep controllers responsible for HTTP input/output, services for business rules and transaction boundaries, and repositories for SQL and persistence. Do not access the database directly from controllers.
- Use constructor injection with `final` fields for required dependencies. Avoid field injection in production code and mutable static application state.
- Services and Repository implementations should always implement interfaces as is practice in Spring.
- Use explicit request/response DTOs where they define an API boundary. Do not let clients write server-owned fields such as roles, ownership, or creation timestamps.
- Validate request shape at the HTTP boundary, using Jakarta Bean Validation where appropriate. Enforce domain rules in services so non-HTTP callers cannot bypass them. Preserve intentional PATCH semantics, including the difference between omitted and null values.
- Use service-level `@Transactional` for changes that must succeed or fail together. Consider database constraints and concurrent requests when enforcing uniqueness or capacity; a prior read alone is insufficient.
- Continue using Spring JDBC and parameterized SQL. Do not introduce JPA/Hibernate or automatic schema generation without a requirement to change the persistence approach.
- Use hexagonal architecture for folder structure. Keep domain objects inside the domain folder and expose ports like controllers and jdbc in their respective folders. The folder structure should follow optimal code practices.
- Implement exception classes where appropriate, place them where appropriate using hex architecture. Implement global exception handlers for common errors like Forbidden etc.
- Use timestamp class for time instances. Not clock. Use timestamp now() methods to get the current time.
- Test cases should be concise, readable and not bloated.

## Security and configuration

- Enforce authentication, ownership, and permissions on the server. Obtain the acting user's identity from the verified principal, never from a client-supplied user ID or role.
- Preserve session authentication and CSRF protection for state-changing requests. Keep CORS origins explicit; do not loosen security to make a test or frontend call pass.
- Treat OAuth identity as trusted only after provider verification. Follow the documented integration contract; do not link accounts using an unverified email address.
- Keep credentials and environment-specific values outside source control. Document configuration with safe placeholders or `.env.example`; never log tokens, secrets, or unnecessary personal data.
- Validate uploaded files and enforce size limits on the server; do not trust the supplied filename or content type alone.

## Database changes

- Follow `db/migrations/README.md`. Add the next available `V<number>__description.sql` migration for schema changes; do not edit, rename, or delete versioned migrations already shared or applied.
- Keep related foreign keys, uniqueness rules, and other data constraints in the database as well as useful application validation.
- Run database tests against an isolated test database. Development seed data is destructive; never apply it to a shared database or delete development volumes as a routine test step.

## Verification

Run commands in PowerShell from the indicated directory, with JDK 23 configured through `JAVA_HOME` and `PATH`.

| Change | Directory | Command |
| --- | --- | --- |
| Backend tests | `backend/LetsDoSports` | `.\mvnw.cmd test` |
| Backend package, including unit tests | `backend/LetsDoSports` | `.\mvnw.cmd package` |
| Frontend lint | `frontend` | `bun run lint` |
| Frontend production build | `frontend` | `bun run prod` |
| Whitespace errors in tracked changes | repository root | `git diff --check` |

- Use installed `mvn` if the Maven wrapper is unavailable. The integration script itself calls `mvn` and requires Maven on `PATH` plus Docker; it creates and removes its own isolated test environment.
- Unit tests do not prove database/API integration. The context-load test requires `LDS_INTEGRATION_TESTS=true`; integration tests run through the Maven `integration` profile used by the script.
- For changed business behavior and bug fixes, add focused tests covering the outcome, important invalid input, and relevant authorization or transaction failures. Use plain unit tests when possible; use Spring-context or PostgreSQL integration tests when the behavior depends on them. Avoid tests that only repeat implementation details.
- Run the checks relevant to the change. Documentation-only changes do not require starting the application or database. For migration changes, verify both upgrades and a fresh database using the documented isolated workflow.
- Report what changed, which checks actually ran, and any failures or skipped checks with their reason. Do not claim unrun tests passed or weaken tests to hide a failure.
