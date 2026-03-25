# AGENTS.md — KIST ORM

Developer guide for AI agents and contributors working in this repository.

## Project Overview

**KIST ORM** is a lightweight, annotation-driven ORM for **Kotlin Multiplatform**, targeting SQLite on native (macOS, Linux, Windows) and JVM. It uses **KSP (Kotlin Symbol Processing)** to generate DAO implementations and entity metadata at compile time.

### Module Structure

| Module | Purpose |
|---|---|
| `kist-api` | Core annotations, interfaces, `DbOperations`, `MetadataRegistry`, platform delegates |
| `kist-ksp` | KSP symbol processor — scans annotations and generates Kotlin source files |
| `kist-sample-test` | Integration sample (native executable, not published) |

No Cursor rules or GitHub Copilot instructions file were found in this repository.

---

## Build / Lint / Test Commands

All commands are run from the repo root using the Gradle wrapper (`./gradlew`). JDK 17 is required (see CI workflow).

### Build

```bash
# Build all modules
./gradlew build

# Build a specific module
./gradlew :kist-api:build
./gradlew :kist-ksp:build
```

### Run All Tests

```bash
# Run all tests across all modules
./gradlew test

# Run kist-ksp JVM tests (Kotest + JUnit 5)
./gradlew :kist-ksp:test

# Run kist-api native tests (kotlin.test)
./gradlew :kist-api:nativeTest
```

### Run a Single Test

```bash
# Run a single test class in kist-ksp (JUnit 5 / Kotest)
./gradlew :kist-ksp:test --tests "io.github.kmupla.kist.ksp.EntityClassVisitorTest"

# Run a single test method in kist-ksp
./gradlew :kist-ksp:test --tests "io.github.kmupla.kist.ksp.DaoClassVisitorTest.visitClassDeclaration with valid dao"

# Run a single native test class in kist-api
./gradlew :kist-api:nativeTest --tests "io.github.kmupla.kist.MetadataRegistryTest"
```

### Publish (Local)

```bash
# Publish all modules to build/repo (for local verification)
./gradlew publish
```

### Run the Sample

```bash
# Build and run the native sample (host platform)
./gradlew :kist-sample-test:runDebugExecutableNative
```

---

## Code Style Guidelines

### Language and Toolchain

- **Kotlin** is the only language used. No Java source files in this project.
- Kotlin version: **2.2.0** (see `settings.gradle.kts`).
- Code style: `kotlin.code.style=official` (enforced via `gradle.properties`).
- Multiplatform hierarchy template is **not** used for `kist-api`; source sets are declared manually.

### Formatting

- Follow the [Kotlin official coding conventions](https://kotlinlang.org/docs/coding-conventions.html).
- Indent with **4 spaces** (no tabs).
- Max line length: **~130 characters** (soft guideline; match existing code).
- Trailing commas are used on multi-line parameter/argument lists (see existing data classes).
- Opening braces on the same line as the declaration.

### Imports

- Use explicit imports — do **not** use wildcard imports (`import foo.*`) except in test files where star-importing KSP symbols is acceptable.
- Group imports: stdlib → third-party → project-local.
- Remove unused imports before committing.

### Naming Conventions

| Element | Convention | Example |
|---|---|---|
| Classes / Objects | `PascalCase` | `DbOperations`, `MetadataRegistry` |
| Interfaces | `PascalCase` | `EntityMetadata<E>`, `KistDao<T, P>` |
| Functions | `camelCase` | `findById`, `bindByType` |
| Properties / vals | `camelCase` | `tableName`, `keyField` |
| Constants (top-level `val`) | `SCREAMING_SNAKE_CASE` | `BASE_PKG` |
| Annotations | `PascalCase` | `@Entity`, `@PrimaryKeyColumn` |
| Test names | Backtick strings | `` `findById when entity missing then returns null` `` |
| Private data classes in visitors | `PascalCase`, local to file | `Metadata`, `CustomSignature` |

### Types

- Prefer **explicit return types** on public functions.
- Use **nullable types** (`T?`) rather than optional wrappers.
- Use `KClass<*>` for runtime class references; avoid `Class<*>`.
- Prefer `data class` for value-carrying types (e.g., `FieldMetadata`).
- Prefer `object` for singletons (`DbOperations`, `MetadataRegistry`, `ResultEvaluator`).
- Use `expect`/`actual` for platform-specific SQLite delegates (`SqliteConnection`, `SqliteStatement`, `SqliteCursor`).

### Error Handling

- Throw `IllegalArgumentException` for invalid user-provided input (missing annotations, wrong type, null IDs).
- Throw `IllegalStateException` for lifecycle/initialization errors (e.g., "Kist is not initialized").
- Throw `UnsupportedOperationException` for unsupported field types in binding code.
- Use `require(condition) { "message" }` and `requireNotNull(value)` for precondition checks instead of manual `if + throw`.
- Avoid swallowing exceptions silently; always log or rethrow.
- Use `error("message")` (which throws `IllegalStateException`) for unreachable states.

### Logging

- Use **Kermit** (`co.touchlab:kermit`) for logging at runtime via `Logger.d { ... }`.
- Use `environment.logger` (KSP logger) inside KSP processors and visitors — never `println` in production processor code (debug `println` calls in visitors should be cleaned up before merging).
- Log SQL commands with `[SQL]` prefix at debug level.

### Annotations

This project's own annotations (source-retained, used by KSP):

| Annotation | Target | Purpose |
|---|---|---|
| `@Entity(tableName)` | Class | Marks a data class as a mapped table |
| `@PrimaryKeyColumn(name)` | Field | Exactly one per `@Entity` |
| `@Column(name)` | Field | Maps a field to a column |
| `@Transient(name)` | Field | Excluded from persistence |
| `@Dao` | Class | Marks an interface for DAO code generation |
| `@Query(value)` | Function | Custom SELECT query |
| `@ModifyingQuery(value)` | Function | Custom INSERT/UPDATE/DELETE |

### KSP Processor Conventions

- All KSP visitors extend `KSVisitorVoid`.
- Processors guard against double-invocation (`alreadyInvoked` flag in `KistProcessor`).
- Generated files go to fixed packages: `io.github.kmupla.kist.entities` (metadata) and `io.github.kmupla.kist.daos` (DAO impls).
- Templates are stored as resources under `src/main/resources/codegen/` and loaded via `ClassLoader.getResourceAsStream`.
- Template placeholders use the `${variable.path}` convention (e.g., `${entity.qualifiedName}`).
- Fail fast via `environment.logger.error(...)` rather than silently skipping invalid symbols.

### Testing

- **`kist-ksp`** tests: JUnit 5 + [Kotest](https://kotest.io/) runner + [Mockito-Kotlin](https://github.com/mockito/mockito-kotlin) for mocking KSP symbols.
- **`kist-api`** tests: `kotlin.test` (multiplatform), running under the `nativeTest` source set with no mocking framework.
- Test class names end in `Test`.
- Test subject is named `underTest`.
- Use `@BeforeEach` / `@BeforeTest` for setup; reset global state (e.g., `MetadataRegistry.reset()`) before each test.
- Test names use backtick strings describing behavior: `` `action when condition then result` ``.
- Do **not** add integration tests to `kist-sample-test`; that module is a standalone native executable sample.

### Package Structure

```
io.github.kmupla.kist          # Core API (kist-api commonMain)
io.github.kmupla.kist.config   # PersistenceConfig, PersistenceContext
io.github.kmupla.kist.delegate # SQLite expect/actual delegates
io.github.kmupla.kist.validation
io.github.kmupla.kist.ksp      # KSP processor (kist-ksp)
io.github.kmupla.kist.entities # Generated entity metadata (at compile time)
io.github.kmupla.kist.daos     # Generated DAO implementations (at compile time)
io.github.kmupla.kist.processed # Generated KistRegister (at compile time)
```

### Multiplatform Targets

`kist-api` supports: `jvm`, `macosX64`, `macosArm64`, `linuxX64`, `linuxArm64`, `mingwX64`.  
`kist-ksp` is JVM-only (KSP processors always run on JVM).  
When adding platform-specific code, place it in the appropriate `jvmMain`/`nativeMain` source set and use `expect`/`actual`.
