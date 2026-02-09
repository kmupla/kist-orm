# Get Started

This guide will help you get up and running with KIST ORM in your Kotlin Native project.

## Why Use KIST ORM?

*   **Productivity**: Write less boilerplate code. specific implementations are generated for you.
*   **Readability**: Keep your database logic near your domain code using annotated interfaces.
*   **Performance**: Generated code is optimized for Kotlin Native.

## Minimal Example

### 1. Define an Entity

```kotlin
import io.knative.kist.Entity
import io.knative.kist.PrimaryKeyColumn
import io.knative.kist.Column

@Entity(tableName = "users")
data class User(
    @PrimaryKeyColumn("id") var id: Long,
    @Column("name") var name: String
)
```

### 2. Define a DAO

```kotlin
import io.knative.kist.Dao
import io.knative.kist.KistDao
import io.knative.kist.Query

@Dao
interface UserDao : KistDao<User, Long> {
    @Query("SELECT * FROM users WHERE name = ?")
    fun findByName(name: String): List<User>
}
```

### 3. Usage

```kotlin
import io.knative.kist.config.PersistenceContext
import io.knative.kist.config.SqlLiteFileConfig
import io.knative.kist.injectDao

fun main() {
    // 1. Initialize Connection
    PersistenceContext.createConnection(
        SqlLiteFileConfig(
            dbName = "my_app.db",
            createStatements = listOf(
                "CREATE TABLE IF NOT EXISTS users (id INTEGER PRIMARY KEY, name TEXT)"
            )
        )
    )

    // 2. Process Annotations (Register Generated Code)
    PersistenceContext.processAnnotations()

    // 3. Use DAO
    val userDao: UserDao by injectDao() 

    val user = User(1, "Alice")
    userDao.insert(user)

    val alice = userDao.findByName("Alice")
    println(alice)
}
```
