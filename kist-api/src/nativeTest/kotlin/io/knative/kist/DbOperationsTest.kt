package io.knative.kist

import co.touchlab.sqliter.Cursor
import co.touchlab.sqliter.DatabaseConnection
import co.touchlab.sqliter.Statement
import dev.mokkery.answering.returns
import dev.mokkery.answering.sequentially
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify
import kotlin.test.*

class DbOperationsTest {
    private val underTest = DbOperations
    private lateinit var connection: DatabaseConnection
    private lateinit var statement: Statement
    private lateinit var cursor: Cursor

    @BeforeTest
    fun setUp() {
        connection = mock<DatabaseConnection>()
        statement = mock<Statement>()
        cursor = mock<Cursor>()

        every { connection.createStatement(any()) } returns statement
        every { statement.bindString(any(), any()) } returns Unit
        every { statement.bindLong(any(), any()) } returns Unit
        every { statement.query() } returns cursor
        every { statement.finalizeStatement() } returns Unit
//        every { cursor.close() } returns Unit
    }

    @Test
    fun `insert should insert data and return a new id`() {
        val person = Person(name = "John Doe", age = 30)
        val metadata = PersonMetadata()
        every { statement.executeInsert() } returns 1L

        val newId = underTest.insert(connection, metadata, person)

        assertEquals(1L, newId)
        verify { statement.bindString(any(), "John Doe") }
        verify { statement.bindLong(any(), 30) }
    }

    @Test
    @Ignore
    fun `update should modify existing data`() {
        val person = Person(id = 1L, name = "Jane Doe", age = 26)
        val metadata = PersonMetadata()
        every { statement.executeUpdateDelete() } returns 1

        val updatedRows = underTest.update(connection, metadata, person)

        assertEquals(1, updatedRows)
        verify { statement.bindString(any(), "Jane Doe") }
        verify { statement.bindLong(any(), 26) }
        verify { statement.bindLong(any(), 1L) }
    }

    @Test
    @Ignore
    fun `deleteById should remove data`() {
        val metadata = PersonMetadata()
        every { statement.executeUpdateDelete() } returns 1

        val deletedRows = underTest.deleteById(connection, metadata, 1L)

        assertEquals(1, deletedRows)
        verify { statement.bindLong(1, 1L) }
    }

    @Test
    fun `findAll should retrieve all records`() {
        val metadata = PersonMetadata()
        every { cursor.next() } sequentially {
            returns(true)
            returns(true)
            returns(false)
        }

        every { cursor.getLong(0) } sequentially {
            returns(1L)
            returns(2L)
        }
        every { cursor.getString(1) } sequentially {
            returns("Alice")
            returns("Bob")
        }
        every { cursor.getLong(2) } sequentially {
            returns(28L)
            returns(32L)
        }

        val people = underTest.findAll(connection, metadata, Person::class)

        assertEquals(2, people.size)
        assertEquals("Alice", people[0].name)
        assertEquals("Bob", people[1].name)
    }

    @Test
    @Ignore
    fun `findById should retrieve a single record`() {
        val metadata = PersonMetadata()
        every { cursor.next() } sequentially {
            returns(true)
            returns(false)
        }
        every { cursor.getLong(0) } returns 1L
        every { cursor.getString(1) } returns "Charlie"
        every { cursor.getLong(2) } returns 35L

        val person = underTest.findById(connection, metadata, Person::class, 1L)

        assertNotNull(person)
        assertEquals("Charlie", person.name)
        verify { statement.bindLong(1, 1L) }
    }

    @Test
    fun `listForGenericType should handle generic type mapping`() {
        every { cursor.next() } sequentially {
            returns(true)
            returns(false)
        }
        every { cursor.columnNames } returns mapOf("name" to 0, "age" to 1)
        every { cursor.getType(0) } returns co.touchlab.sqliter.FieldType.TYPE_TEXT
        every { cursor.getString(0) } returns "Frank"
        every { cursor.getType(1) } returns co.touchlab.sqliter.FieldType.TYPE_INTEGER
        every { cursor.getLong(1) } returns 50L

        val query = "SELECT name, age FROM person WHERE name = ?"
        val results =
            underTest.listForGenericType(connection, { row -> "${row[0]} is ${row[1]}" }, String::class, query, "Frank")

        assertEquals(1, results.size)
        assertEquals("Frank is 50", results.first())
    }

    @Test
    fun `findSingleForGenericType should retrieve a single generic record`() {
        every { cursor.next() } sequentially {
            returns(true)
            returns(false)
        }
        every { cursor.columnNames } returns mapOf("name" to 0, "age" to 1)
        every { cursor.getType(0) } returns co.touchlab.sqliter.FieldType.TYPE_TEXT
        every { cursor.getString(0) } returns "Grace"
        every { cursor.getType(1) } returns co.touchlab.sqliter.FieldType.TYPE_INTEGER
        every { cursor.getLong(1) } returns 60L

        val query = "SELECT name, age FROM person WHERE name = ?"
        val result = underTest.findSingleForGenericType(
            connection,
            { row -> "${row[0]} is ${row[1]}" },
            String::class,
            query,
            "Grace"
        )

        assertEquals("Grace is 60", result)
    }
}

private data class Person(val id: Long? = null, val name: String, val age: Int?)

private class PersonMetadata : EntityMetadata<Person> {
    override val tableName: String = "person"
    override val keyField: String = "id"
    override val fieldMetadata: List<FieldMetadata> = listOf(
        FieldMetadata("id", "id", Int::class, true),
        FieldMetadata("name", "name", String::class, false),
        FieldMetadata("age", "age", Int::class, true)
    )

    override fun bindFields(source: Person, statement: Statement, fieldIndexMap: Map<String, Int>) {
        fieldIndexMap["name"]?.let { statement.bindString(it, source.name) }
        fieldIndexMap["age"]?.let { source.age?.let { age -> statement.bindLong(it, age.toLong()) } }
        fieldIndexMap["id"]?.let { source.id?.let { id -> statement.bindLong(it, id) } }
    }

    override fun getId(source: Person): Any? {
        return source.id
    }

    override fun create(cursor: Cursor): Person {
        return Person(
            id = cursor.getLong(0),
            name = cursor.getString(1),
            age = cursor.getLong(2).toInt()
        )
    }
}