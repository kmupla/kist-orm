# Kist ORM Framework Usage

This project uses **Kist**, a custom ORM framework for Kotlin Native/Multiplatform, which shares similarities with Spring Data JPA and Android Room. Below is a guide on how to use it based on a PetShop example.

## 1. Entity Definition

Entities are Kotlin data classes annotated with `@Entity`. They map to database tables.

### Annotations
- `@Entity(tableName = "...")`: Marks the class as an entity and specifies the table name.
- `@PrimaryKeyColumn("...")`: Marks the primary key field and its column name.
- `@Column("...")`: Maps a class property to a database column.

### Example (`Pet.kt`)
```kotlin
package com.example.petshop.model

import io.rss.knative.tools.kist.Column
import io.rss.knative.tools.kist.Entity
import io.rss.knative.tools.kist.PrimaryKeyColumn
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "pets")
data class Pet (

    @PrimaryKeyColumn("id")
    var id: String,

    @Column("name")
    var name: String,

    @Column("type")
    var type: PetType,

    @Column("owner_id")
    var ownerId: String?,

    @Column("birth_date")
    var birthDate: Long,

    @Column("status")
    var status: PetStatus,

)
```

## 2. DAO Definition

Data Access Objects (DAOs) are interfaces that define database operations. They must be annotated with `@Dao` and extend `KistDao`.

### Basic Usage
Extend `KistDao<Entity, ID_Type>` to inherit basic CRUD operations.

```kotlin
@Dao
interface OwnerDao: KistDao<Owner, String> {
}
```

### Custom Queries
Use the `@Query` annotation to define custom SQL queries. Method parameters can be bound to query parameters using the `:` prefix.

### Example (`PetDao.kt`)
```kotlin
@Dao
interface PetDao: KistDao<Pet, String> {

    @Query("""
        SELECT * 
        FROM pets p 
        WHERE p.birth_date >= :minBirthDate AND p.status = 'AVAILABLE'
        ORDER BY p.birth_date
        LIMIT 1
    """)
    fun findYoungestAvailablePet(minBirthDate: Long): Pet?

    @Query("""
        SELECT MIN(p.birth_date) 
        FROM pets p 
        WHERE p.status = 'AVAILABLE'
    """)
    fun findOldestAvailablePetDate(): Long?
    
    // Dynamic query parameters
    @Query("""
        SELECT * 
        FROM pets p 
        WHERE p.status <> 'SOLD'  
          AND (:petType IS NULL OR p.type = :petType)
    """)
    fun findAvailablePets(petType: PetType? = null): List<Pet>
}
```

### Advanced Query Examples (`OrderDao.kt`)
The framework supports aggregation, `DELETE` operations, and `IN` clauses with list parameters.

```kotlin
@Dao
interface OrderDao: KistDao<Order, String> {

    @Query("SELECT COUNT(*) as count FROM orders")
    fun countAllOrders(): Long?
}

@Dao
interface OrderItemDao: KistDao<OrderItem, String> {

    @Query("DELETE FROM order_items WHERE order_id = :orderId")
    fun deleteByOrder(orderId: String)

    // List parameter for IN clause
    @Query("SELECT * FROM order_items WHERE product_id IN (:productIds)")
    fun findByProductIds(productIds: List<String>): List<OrderItem>
}
```

## 3. Configuration & Initialization

The framework needs to be initialized with a database configuration and schema definitions.

### Setup Steps
1.  **Define Schema**: Prepare the SQL statements for creating tables.
2.  **Create Connection**: Use `PersistenceContext.createConnection` with `SqlLiteFileConfig`.
3.  **Process Annotations**: Call `PersistenceContext.processAnnotations()` to finalize setup.

### Example (`PetShopConfigurer.kt`)
```kotlin
object PetShopConfigurer {

    fun configureDb(filePath: String) {
        // Prepare SQL schema statements
        val sqlList = cleanUpSqlSource(R.DB_SCHEMA_SQL)
            .plus(cleanUpSqlSource(Assets.INITIAL_DATA_SQL.decodeToString()))

        // Ensure directory exists
        Path(filePath).parent?.let {
            Files.createDirectories(it)
        }

        // Initialize PersistenceContext
        PersistenceContext.createConnection(
            SqlLiteFileConfig("petshop.db.sqlite",
                path = filePath,
                createStatements = sqlList))
        
        // Process annotations (Essential step)
        PersistenceContext.processAnnotations()
    }
    
    // ... helper methods ...
}
```

## 4. Transactions

There is a `Transactional` object in `com.example.petshop.dao.Transactional`, but it appears to be a placeholder implementation currently.

```kotlin
package com.example.petshop.dao

import kotlin.native.concurrent.ThreadLocal

@ThreadLocal
object Transactional {
    fun <T> runInTransaction(block: () -> T): T {
        // Placeholder: executes block directly
        return block()
    }
}
```

## 5. Dependencies

The framework relies on:
- `io.rss.knative.tools.kist.*` packages.
- `kotlinx.serialization` (often used with Entities).

