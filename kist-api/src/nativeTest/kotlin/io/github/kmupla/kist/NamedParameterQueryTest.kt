package io.github.kmupla.kist

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NamedParameterQueryTest {

    private val underTest = NamedParameterQuery

    // --- isNamed ---

    @Test
    fun `isNamed when query has named params then returns true`() {
        assertTrue(underTest.isNamed("SELECT * FROM t WHERE name = :name"))
    }

    @Test
    fun `isNamed when query has only positional params then returns false`() {
        assertFalse(underTest.isNamed("SELECT * FROM t WHERE name = ?"))
    }

    @Test
    fun `isNamed when query has no params then returns false`() {
        assertFalse(underTest.isNamed("SELECT * FROM t"))
    }

    @Test
    fun `isNamed when colon is inside string literal then returns false`() {
        assertFalse(underTest.isNamed("SELECT * FROM t WHERE label = ':notAParam'"))
    }

    // --- parse: positional pass-through ---

    @Test
    fun `parse when query has no named params then returns original sql with empty param names`() {
        val result = underTest.parse("SELECT * FROM t WHERE id = ?")
        assertEquals("SELECT * FROM t WHERE id = ?", result.sql)
        assertEquals(emptyList(), result.paramNames)
    }

    @Test
    fun `parse when query has no params at all then returns unchanged sql`() {
        val result = underTest.parse("SELECT * FROM t")
        assertEquals("SELECT * FROM t", result.sql)
        assertEquals(emptyList(), result.paramNames)
    }

    // --- parse: named params ---

    @Test
    fun `parse when query has single named param then rewrites sql and captures name`() {
        val result = underTest.parse("SELECT * FROM t WHERE name = :name")
        assertEquals("SELECT * FROM t WHERE name = ?", result.sql)
        assertEquals(listOf("name"), result.paramNames)
    }

    @Test
    fun `parse when query has multiple named params then rewrites all and preserves order`() {
        val result = underTest.parse("SELECT * FROM t WHERE name = :name AND street = :street")
        assertEquals("SELECT * FROM t WHERE name = ? AND street = ?", result.sql)
        assertEquals(listOf("name", "street"), result.paramNames)
    }

    @Test
    fun `parse when named param appears multiple times then captures each occurrence`() {
        val result = underTest.parse("SELECT * FROM t WHERE a = :val OR b = :val")
        assertEquals("SELECT * FROM t WHERE a = ? OR b = ?", result.sql)
        assertEquals(listOf("val", "val"), result.paramNames)
    }

    @Test
    fun `parse when named param contains underscore and digits then is parsed correctly`() {
        val result = underTest.parse("SELECT * FROM t WHERE foo_bar2 = :foo_bar2")
        assertEquals("SELECT * FROM t WHERE foo_bar2 = ?", result.sql)
        assertEquals(listOf("foo_bar2"), result.paramNames)
    }

    @Test
    fun `parse when colon is inside string literal then literal is preserved and not parsed as param`() {
        val result = underTest.parse("SELECT * FROM t WHERE label != ':skip' AND name = :name")
        assertEquals("SELECT * FROM t WHERE label != ':skip' AND name = ?", result.sql)
        assertEquals(listOf("name"), result.paramNames)
    }

    // --- parse: mixed mode rejection ---

    @Test
    fun `parse when query mixes positional and named params then throws`() {
        assertFailsWith<IllegalArgumentException> {
            underTest.parse("SELECT * FROM t WHERE a = ? AND b = :b")
        }
    }

    // --- orderedValues ---

    @Test
    fun `orderedValues when all names present then returns values in query order`() {
        val parsed = underTest.parse("SELECT * FROM t WHERE name = :name AND age = :age")
        val values = underTest.orderedValues(parsed, mapOf("age" to 30, "name" to "Alice"))
        assertEquals(listOf("Alice", 30), values)
    }

    @Test
    fun `orderedValues when param appears twice then value is returned twice`() {
        val parsed = underTest.parse("SELECT * FROM t WHERE a = :x OR b = :x")
        val values = underTest.orderedValues(parsed, mapOf("x" to 42))
        assertEquals(listOf(42, 42), values)
    }

    @Test
    fun `orderedValues when null value present then null is returned in position`() {
        val parsed = underTest.parse("SELECT * FROM t WHERE name = :name")
        val values = underTest.orderedValues(parsed, mapOf("name" to null))
        assertEquals(listOf(null), values)
    }

    @Test
    fun `orderedValues when referenced name missing from map then throws`() {
        val parsed = underTest.parse("SELECT * FROM t WHERE name = :name")
        assertFailsWith<IllegalArgumentException> {
            underTest.orderedValues(parsed, emptyMap())
        }
    }

    @Test
    fun `orderedValues when map has extra keys then succeeds without error`() {
        val parsed = underTest.parse("SELECT * FROM t WHERE name = :name")
        val values = underTest.orderedValues(parsed, mapOf("name" to "Bob", "unused" to 99))
        assertEquals(listOf("Bob"), values)
    }
}
