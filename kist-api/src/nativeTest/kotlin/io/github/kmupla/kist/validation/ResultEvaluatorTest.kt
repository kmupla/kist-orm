package io.github.kmupla.kist.validation

import io.github.kmupla.kist.FieldMetadata
import kotlin.test.Test
import kotlin.test.assertFailsWith

class ResultEvaluatorTest {

    private val underTest = ResultEvaluator

    @Test
    fun `assertRequiredColumnsPresent should not throw exception when all required columns are present`() {
        val queryColumns = mapOf("id" to 0, "name" to 1, "age" to 2)
        val fieldMetadata = listOf(
            FieldMetadata("id", "id", Int::class, false),
            FieldMetadata("name", "name", String::class, false),
            FieldMetadata("age", "age", Int::class, true)
        )

        underTest.assertRequiredColumnsPresent(queryColumns, fieldMetadata)
    }

    @Test
    fun `assertRequiredColumnsPresent should throw exception when a required column is missing`() {
        val queryColumns = mapOf("id" to 0, "age" to 1)
        val fieldMetadata = listOf(
            FieldMetadata("id", "id", Int::class, false),
            FieldMetadata("name", "name", String::class, false),
            FieldMetadata("age", "age", Int::class, true)
        )

        assertFailsWith<IllegalArgumentException> {
            underTest.assertRequiredColumnsPresent(queryColumns, fieldMetadata)
        }
    }

    @Test
    fun `assertRequiredColumnsPresent should not throw exception when there are more columns than fields`() {
        val queryColumns = mapOf("id" to 0, "name" to 1, "age" to 2, "extra" to 3)
        val fieldMetadata = listOf(
            FieldMetadata("id", "id", Int::class, false),
            FieldMetadata("name", "name", String::class, false),
            FieldMetadata("age", "age", Int::class, true)
        )

        underTest.assertRequiredColumnsPresent(queryColumns, fieldMetadata)
    }

    @Test
    fun `assertRequiredColumnsPresent should not throw exception when no columns are required`() {
        val queryColumns = mapOf("id" to 0, "name" to 1)
        val fieldMetadata = listOf(
            FieldMetadata("id", "id", Int::class, true),
            FieldMetadata("name", "name", String::class, true)
        )

        underTest.assertRequiredColumnsPresent(queryColumns, fieldMetadata)
    }
}
