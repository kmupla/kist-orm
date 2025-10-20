package io.rss.knative.tools.kist

import co.touchlab.sqliter.Cursor
import co.touchlab.sqliter.FieldType
import co.touchlab.sqliter.Statement
import kotlin.reflect.KClass

interface EntityMetadata<E> {
    val tableName: String
    val keyField: String
    val fieldMetadata: List<FieldMetadata>

    fun create(cursor: Cursor): E
    fun bindFields(source: E, statement: Statement, fieldIndexMap: Map<String, Int>)
}

data class FieldMetadata(
    val fieldName: String,
    val columnName: String,
    val fieldType: KClass<*>,
//    val fieldType: FieldType,
    val nullable: Boolean,
)