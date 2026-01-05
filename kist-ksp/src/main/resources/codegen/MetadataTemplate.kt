package io.knative.kist.entities

import co.touchlab.kermit.Logger
import co.touchlab.sqliter.Cursor
import co.touchlab.sqliter.FieldType
import co.touchlab.sqliter.Statement
import co.touchlab.sqliter.bindString
import co.touchlab.sqliter.bindLong
import co.touchlab.sqliter.bindDouble
import co.touchlab.sqliter.bindBlob

import io.knative.kist.EntityMetadata
import io.knative.kist.FieldMetadata
import io.knative.kist.validation.ResultEvaluator
import io.knative.kist.DbOperations
import kotlin.Int

import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.toInstant

import ${entity.qualifiedName}

@OptIn(ExperimentalTime::class)
object ${entity.simpleName}OrmMetadata: EntityMetadata<${entity.simpleName}> {
    override val tableName: String = "${entity.tableName}"
    override val keyField = "${entity.keyField}"
    override val fieldMetadata: List<FieldMetadata> = listOf(

        ${fieldMetadataList}
    )

    override fun create(cursor: Cursor): ${entity.simpleName} {
        val queryFields = cursor.columnNames
        ResultEvaluator.assertRequiredColumnsPresent(queryFields, fieldMetadata)
        val columnsIdxToNameMap = cursor.columnNames.map { it.value to it.key }.toMap() // invert key and index

        val columnValues = queryFields.map { field ->
            val (_, idx) = field
            val effectiveValue = DbOperations.readValueByColumnType(cursor, idx, columnsIdxToNameMap)

            // TODO: maybe check double/long in range for float/int

            field.key to effectiveValue
        }.toMap()

        return ${entity.simpleName} (
            ${classConstructorAssignments}
        )
    }

    override fun bindFields(source: ${entity.simpleName}, statement: Statement, fieldIndexMap: Map<String, Int>) {
        ${bindingsDeclarations}
    }

    override fun getId(source: ${entity.simpleName}): Any? {
        return source.${entity.keyField}
    }
}