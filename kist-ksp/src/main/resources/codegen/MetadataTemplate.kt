package io.github.kmupla.kist.entities

import co.touchlab.kermit.Logger
import io.github.kmupla.kist.delegate.*

import io.github.kmupla.kist.EntityMetadata
import io.github.kmupla.kist.FieldMetadata
import io.github.kmupla.kist.validation.ResultEvaluator
import io.github.kmupla.kist.DbOperations
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

    override fun create(cursor: SqliteCursor): ${entity.simpleName} {
        val queryFields = cursor.getColumnNames()
        ResultEvaluator.assertRequiredColumnsPresent(queryFields, fieldMetadata)

        val columnValues = queryFields.map { field ->
            val (_, idx) = field
            val effectiveValue = DbOperations.readValueByColumnType(cursor, idx)

            field.key to effectiveValue
        }.toMap()

        return ${entity.simpleName} (
            ${classConstructorAssignments}
        )
    }

    override fun bindFields(source: ${entity.simpleName}, statement: SqliteStatement, fieldIndexMap: Map<String, Int>) {
        ${bindingsDeclarations}
    }

    override fun getId(source: ${entity.simpleName}): Any? {
        return source.${entity.keyField}
    }
}