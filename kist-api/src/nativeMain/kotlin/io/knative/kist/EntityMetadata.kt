package io.knative.kist

import co.touchlab.sqliter.Cursor
import co.touchlab.sqliter.FieldType
import co.touchlab.sqliter.Statement
import kotlin.reflect.KClass

interface EntityMetadata<E> {
    val tableName: String
    val keyField: String
    val fieldMetadata: List<io.knative.kist.FieldMetadata>

    fun create(cursor: Cursor): E
    fun bindFields(source: E, statement: Statement, fieldIndexMap: Map<String, Int>)
    fun getId(source: E): Any?
}

data class FieldMetadata(
    val fieldName: String,
    val columnName: String,
    val fieldType: KClass<*>,
    val nullable: Boolean,
)