package io.github.kmupla.kist

import io.github.kmupla.kist.delegate.SqliteCursor
import io.github.kmupla.kist.delegate.SqliteStatement
import kotlin.reflect.KClass

interface EntityMetadata<E> {
    val tableName: String
    val keyField: String
    val fieldMetadata: List<FieldMetadata>

    fun create(cursor: SqliteCursor): E
    fun bindFields(source: E, statement: SqliteStatement, fieldIndexMap: Map<String, Int>)
    fun getId(source: E): Any?
}

data class FieldMetadata(
    val fieldName: String,
    val columnName: String,
    val fieldType: KClass<*>,
    val nullable: Boolean,
)

enum class ColumnType {
    TYPE_LONG,
    TYPE_DOUBLE,
    TYPE_TEXT,
    TYPE_BLOB,
    TYPE_NULL,
}