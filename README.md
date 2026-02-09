# KIST ORM

**KIST ORM** is a lightweight Object-Relational Mapping (ORM) framework designed for **Kotlin Multiplatform**, with a primary focus on **Kotlin Native** desktop development. It provides an annotation-based experience similar to Android Room or Spring Data JPA, specifically tailored for SQLite databases in native environments.

## Goal

The goal of KIST ORM is to provide a "Code-First" developer experience for database persistence in Kotlin Native. It aims to fill the gap for developers who prefer defining entities and queries using Kotlin annotations rather than the "Database-First" approach common in other tools.

## High-Level Architecture

KIST ORM leverages **KSP (Kotlin Symbol Processing)** to generate type-safe database access code at compile time, ensuring minimal runtime overhead and maximum performance on native targets.

- **`kist-api`**: Contains the core annotations (`@Entity`, `@Dao`, `@Query`), common interfaces, and runtime configuration classes.
- **`kist-ksp`**: The symbol processor that scans your code for annotated entities and DAOs to generate their concrete implementations automatically.

## Quick Start

### 1. Define your Entity
```kotlin
@Entity(tableName = "users")
data class User(
    @PrimaryKeyColumn("id") var id: Long,
    @Column("name") var name: String
)
```

### 2. Define your DAO
```kotlin
@Dao
interface UserDao : KistDao<User, Long> {
    @Query("SELECT * FROM users WHERE name = ?")
    fun findByName(name: String): List<User>
}
```

### 3. Initialize and Use
```kotlin
// Setup connection
PersistenceContext.createConnection(SqlLiteFileConfig(dbName = "app.db"))
PersistenceContext.processAnnotations()

// Inject and use
val userDao: UserDao by injectDao()
userDao.insert(User(1, "Alice"))
```

For more detailed information, please refer to the [documentation](docs/index.md).

## Contributions

Contributions are welcome! If you have suggestions or find bugs, feel free to contribute. 

**Note**: We prefer Pull Requests (PRs) over opening new issues. Please submit a PR with your proposed changes or fixes directly.
