# Queries & DAOs

Data Access Objects (DAOs) provide the abstract interface to your database. They allow you to perform CRUD operations and execute custom SQL queries without writing boilerplate code.

## Defining a DAO

To create a DAO, define an interface that extends `KistDao<Entity, ID_Type>` and annotate it with `@Dao`.

```kotlin
import io.github.kmupla.kist.Dao
import io.github.kmupla.kist.KistDao

@Dao
interface PetDao : KistDao<Pet, String> {
    // KistDao provides built-in methods:
    // - insert(entity: Pet): Long?
    // - update(entity: Pet): Int
    // - deleteById(id: String): Int
    // - findById(id: String): Pet?
    // - findAll(): List<Pet>
    // - exists(id: String): Boolean
}
```

## Custom SELECT Queries (`@Query`)

Use `@Query` to declare custom SELECT statements. KSP generates the full implementation at compile time — no reflection at runtime.

### Positional Parameter Binding (`?`)

Parameters are bound left-to-right in the same order as the method signature.

```kotlin
@Query("SELECT * FROM pets WHERE name = ?")
fun findByName(name: String): List<Pet>

@Query("SELECT * FROM person_table WHERE name like ? AND street like ?")
fun findByNameStreet(name: String, streetPart: String): List<Person>
```

### Named Parameter Binding (`:paramName`)

Use `:paramName` placeholders when you want query readability independent of parameter order. Each placeholder must match a method parameter name exactly.

```kotlin
// Parameter order in the query does not have to match the method signature
@Query("SELECT * FROM person_table WHERE street like :streetPart AND name like :name")
fun findByNameStreetNamed(name: String, streetPart: String): List<Person>
```

> Mixing `?` and `:name` placeholders in the same query is not allowed and will cause a compile-time error.

### Aggregations and DTOs

`@Query` is not limited to returning entities. You can return primitives or custom Data Transfer Objects (DTOs).

```kotlin
data class PersonMinDto(val id: Int, val name: String)

@Dao
interface PersonDao : KistDao<Person, Int> {

    @Query("SELECT count(*) FROM person_table")
    fun countAll(): Long

    // Columns are mapped positionally to the DTO's primary constructor
    @Query("SELECT id, name FROM person_table")
    fun listMinimalReference(): List<PersonMinDto>
}
```

## Modifying Queries (`@ModifyingQuery`)

Use `@ModifyingQuery` for custom INSERT, UPDATE, or DELETE statements. The return type must be either `Long` (number of rows affected) or `Unit` (when the count is not needed).

```kotlin
import io.github.kmupla.kist.ModifyingQuery

@Dao
interface PersonDao : KistDao<Person, Int> {

    // Returns the number of rows updated
    @ModifyingQuery("UPDATE person_table SET is_active = 0 WHERE birthday_timestamp < :threshold")
    fun deactivateOlderThan(threshold: Long): Long

    // Returns Unit when the row count is not needed
    @ModifyingQuery("DELETE FROM person_table WHERE street = ?")
    fun deleteByStreet(street: String): Unit
}
```

Both positional (`?`) and named (`:paramName`) parameter styles are supported, with the same rules and validation as `@Query`. Mixing both styles in a single query is not allowed.

### Return Types

| Return type | Behaviour |
|---|---|
| `Long` | Returns the number of rows affected by the statement |
| `Unit` | Executes the statement and discards the row count |

## Injecting DAOs

Once your DAOs are defined and the application is configured, inject them into your services using the `injectDao` delegate.

```kotlin
import io.github.kmupla.kist.injectDao

object PersonService {
    // Automatically resolves the generated implementation for PersonDao
    private val personDao: PersonDao by injectDao()

    fun deactivateOldAccounts(cutoff: Long) {
        val affected = personDao.deactivateOlderThan(cutoff)
        println("$affected accounts deactivated")
    }
}
```

**Note:** `PersistenceContext.processAnnotations()` must be called at startup before any injected DAO is accessed.
