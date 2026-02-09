# Entities

Entities are the core of your data model in KIST ORM. They represent the tables in your SQLite database.

## Defining an Entity

To define an entity, create a Kotlin `data class` and annotate it with `@Entity`. Use `@Column` and `@PrimaryKeyColumn` to map properties to database columns.

```kotlin
import io.knative.kist.Entity
import io.knative.kist.Column
import io.knative.kist.PrimaryKeyColumn

@Entity(tableName = "pets")
data class Pet(
    @PrimaryKeyColumn("id")
    var id: String,

    @Column("name")
    var name: String,

    @Column("age")
    var age: Int,
    
    @Column("is_adopted")
    var isAdopted: Boolean = false,
    
    @Column("birth_date")
    var birthDate: Long // Store dates as timestamps (INTEGER)
)
```

## Supported Annotations

### `@Entity`
Marks a class as a database entity.
*   `tableName`: **Required**. Specifies the name of the corresponding table in the database.

### `@PrimaryKeyColumn`
Marks a property as the primary key of the table.
*   `name`: **Required**. The column name in the database.
*   **Constraint**: Every entity must have exactly one property annotated with `@PrimaryKeyColumn`.

### `@Column`
Maps a standard property to a database column.
*   `name`: **Required**. The column name in the database.

## Data Types

KIST maps Kotlin types to SQLite types as follows:

| Kotlin Type | SQLite Type | Notes |
| :--- | :--- | :--- |
| `String` | `TEXT` | |
| `Int` | `INTEGER` | |
| `Long` | `INTEGER` | |
| `Boolean` | `INTEGER` | Stored as `1` (true) or `0` (false). |
| `Double` | `REAL` | |

### Complex Types (Enums, Dates)

Currently, KIST supports primarily primitive types. For complex types like `Enum` or `LocalDateTime`, you typically need to store them as primitives (e.g., `String`, `Int`, or `Long`) and handle the conversion manually or via property accessors if specific type converters are not yet configured.

Example with Enum:

```kotlin
enum class PetType { DOG, CAT }

@Entity(tableName = "animals")
data class Animal(
    @PrimaryKeyColumn("id") var id: Int,
    @Column("type") var typeString: String // Store as string
) {
    // Helper property to work with Enum in code
    var type: PetType
        get() = PetType.valueOf(typeString)
        set(value) { typeString = value.name }
}
```
