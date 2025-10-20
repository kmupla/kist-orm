package io.rss.knative.tools.kist.entities

import co.touchlab.sqliter.Cursor
import co.touchlab.sqliter.FieldType
import co.touchlab.sqliter.Statement
import co.touchlab.sqliter.bindString

import io.rss.knative.tools.kist.EntityMetadata
import io.rss.knative.tools.kist.FieldMetadata
import io.rss.knative.tools.kist.ResultEvaluator

import ${entity.qualifiedName}

object ${entity.simpleName}OrmMetadata: EntityMetadata<${entity.simpleName}> {
    override val tableName: String = "${entity.tableName}"
    override val keyField = "${entity.keyField}"
    override val fieldMetadata: List<FieldMetadata> = listOf(

        ${fieldMetadataList}
    )

    override fun create(cursor: Cursor): ${entity.simpleName} {
        val queryFields = cursor.columnNames
        ResultEvaluator.assertRequiredColumnsPresent(queryFields, fieldMetadata)

        val columnValues = queryFields.map { field ->
            val (_, idx) = field
            val cType = cursor.getType(idx) // TODO: assert column type matches field type
            val effectiveValue = when (cType) {
                FieldType.TYPE_INTEGER -> cursor.getLong(idx)
                FieldType.TYPE_FLOAT -> cursor.getDouble(idx)
                FieldType.TYPE_TEXT -> cursor.getString(idx)
                FieldType.TYPE_NULL -> {
                    // ??
                }
                else -> cursor.getBytes(idx)
            }

            // maybe check double/long in range for float/int

            field.key to effectiveValue
        }.toMap()

        return ${entity.simpleName} (
            ${classConstructorAssignments}
        )
    }

    override fun bindFields(source: ${entity.simpleName}, statement: Statement, fieldIndexMap: Map<String, Int>) {
        ${bindingsDeclarations}
    }
}