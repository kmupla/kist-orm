# Queries & DAOs

Data Access Objects (DAOs) provide the abstract interface to your database. They allow you to perform CRUD operations and execute custom SQL queries without writing boilerplate code.

## Defining a DAO

To create a DAO, define an interface that extends `KistDao<Entity, ID_Type>` and annotate it with `@Dao`.

```kotlin
import io.knative.kist.Dao
import io.knative.kist.KistDao
import io.knative.kist.Query

@Dao
interface PetDao : KistDao<Pet, String> {
    // KistDao provides built-in methods:
    // - insert(entity: Pet)
    // - update(entity: Pet)
    // - deleteById(id: String)
    // - findById(id: String): Pet?
    // - findAll(): List<Pet>
    // - exists(id: String): Boolean
}
```

## Custom Queries

You can define custom operations using the `@Query` annotation.

### Basic Selection

Use standard SQL syntax.

```kotlin
@Query("SELECT * FROM pets WHERE name = 'Fido'")
fun findFido(): Pet?
```

### Parameter Binding


~Bind method parameters to your query using the `:` prefix.~ (not yet available)

```kotlin
@Query("SELECT * FROM pets WHERE name = ?")
fun findByName(name: String): List<Pet>

@Query("SELECT * FROM pets WHERE age >= ?")
fun findOlderThan(minAge: Int): List<Pet>
```

Bind the parameters using `?` and declaring the parameters in the same order:

```kotlin
@Query("SELECT * FROM person_table WHERE name like ? AND street like ?")
fun findByNameStreet(name: String, streetPart: String): List<Person>
```

### Dynamic Parameters & Null Checks

KIST supports checking for optional parameters directly in SQL.

```kotlin
// If type is null, ignore the filter (return all types)
@Query("SELECT * FROM pets WHERE (? IS NULL OR type = ?)")
fun findByType(type: String?, type: String?): List<Pet>
```

### List Parameters (IN Clause)

You can pass lists to handle SQL `IN` clauses.

```kotlin
@Query("SELECT * FROM pets WHERE id IN (?)")
fun findByIds(ids: List<String>): List<Pet>
```

### Aggregations and DTOs

Queries are not restricted to returning Entities. You can return primitives, counts, or custom Data Transfer Objects (DTOs).

```kotlin
data class PetSummary(val name: String, val age: Int)

@Dao
interface PetDao : KistDao<Pet, String> {
    
    @Query("SELECT count(*) FROM pets")
    fun countAll(): Long

    // Mapping columns to DTO properties automatically
    @Query("SELECT name, age FROM pets WHERE is_adopted = 1")
    fun getAdoptedSummaries(): List<PetSummary>
}
```

## Injecting DAOs

Once your DAOs are defined and the application is configured, you can inject them into your services or controllers.

Use the `injectDao` delegate for lazy retrieval.

```kotlin
import io.knative.kist.injectDao

object PetService {
    // Automatically finds the generated implementation for PetDao
    private val petDao: PetDao by injectDao()

    fun registerPet(pet: Pet) {
        petDao.insert(pet)
    }

    fun find(id: String) = petDao.findById(id)
}
```

**Note:** Ensure `PersistenceContext.processAnnotations()` is called at startup before accessing any injected DAO.
