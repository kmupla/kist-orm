 KIST ORM

**KIST ORM** is a lightweight Object-Relational Mapping (ORM) framework designed specifically for **Kotlin Native** and **Kotlin Multiplatform**, with a primary focus on native desktop development. It currently targets **SQLite** exclusively.

## Motivation

The ecosystem for native desktop development with Kotlin is still growing, and options for database interaction are limited. The most prominent solution, **SQLDelight**, adopts a "Database-First" approach, generating type-safe Kotlin code from SQL files. While powerful, this approach requires developers to switch contexts between SQL files and Kotlin code and maps objects to queries rather than tables.

Other older options like KTORM exist, but often rely on different philosophies or heavy reflection that might not be ideal for all native scenarios.

**KIST ORM** was created to fill a specific gap: providing an **annotation-based** ORM experience similar to **Android Room** or **Spring Data JPA**, but tailored for the Kotlin Native environment.

## Philosophy

KIST ORM follows a philosophy of developer convenience and familiarity:

*   **Annotation-Driven**: Define your entities and queries using standard Kotlin classes and interfaces annotated with metadata (e.g., `@Entity`, `@Dao`, `@Query`).
*   **Compile-Time Generation**: Leveraging **KSP (Kotlin Symbol Processing)**, KIST generates the necessary boilerplate code (DAOs, metadata) during the build process. This ensures type safety and reduces runtime overhead.
*   **Native Focus**: Targeted specifically at native desktop development.
*   **Familiarity**: If you have used Room on Android, Spring Data JPA, or Hibernate, KIST's "Code-First" approach will feel right at home.

## Key Features

*   **SQLite Support**: Built on top of SQLite.
*   **Code Generation**: Uses KSP to generate implementations for your DAOs.
*   **Simple API**: Clean interfaces for database connections and transactions.
*   **Object Mapping**: Maps logical entities directly to database tables.
