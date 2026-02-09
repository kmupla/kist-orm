package io.github.kmupla.kist

import co.touchlab.kermit.Logger
import io.github.kmupla.kist.delegate.SqliteConnection
import io.github.kmupla.kist.delegate.SqliteCursor
import io.github.kmupla.kist.delegate.SqliteStatement
import io.github.kmupla.kist.delegate.bindByType
import kotlin.reflect.KClass

object DbOperations {

    fun <E> insert(connection: SqliteConnection, metadata: EntityMetadata<E>, entity: E): Long {
        val tableName = metadata.tableName
        val fields = metadata.fieldMetadata.joinToString(",") { it.columnName }
        val placeholders = metadata.fieldMetadata.joinToString(",") { "?" }
        val command = "INSERT INTO $tableName ($fields) VALUES ($placeholders)"

        Logger.d { "[SQL] $command" }

        return connection.withStatement(command) {
            val fieldToIndex = metadata.fieldMetadata.withIndex().associate { it.value.fieldName to (it.index + 1) }
            metadata.bindFields(entity, this, fieldToIndex)
            executeInsert()
        }
    }

    fun <E> update(connection: SqliteConnection, metadata: EntityMetadata<E>, entity: E): Int {
        val tableName = metadata.tableName
        val fields = metadata.fieldMetadata.joinToString(",") { "${it.columnName} = ?" }
        val idField = metadata.fieldMetadata.first { it.fieldName == metadata.keyField }
        val whereClause = "${idField.columnName} = ?"
        val command = "UPDATE $tableName SET $fields WHERE $whereClause"
        val id = metadata.getId(entity)
            ?: throw IllegalArgumentException("Cant update ${metadata.tableName} because id is null")

        Logger.d { "[SQL] $command" }

        return connection.withStatement(command) {
            val fieldToIndex = metadata.fieldMetadata.withIndex().associate { it.value.fieldName to (it.index + 1) }
            metadata.bindFields(entity, this, fieldToIndex)
            bindField(idField, metadata.fieldMetadata.lastIndex + 2, id)
            executeUpdateDelete()
        }
    }

    fun <E> deleteById(connection: SqliteConnection, metadata: EntityMetadata<E>, id: Any): Int {
        val tableName = metadata.tableName
        val idField = metadata.fieldMetadata.first { it.fieldName == metadata.keyField }
        val whereClause = "${idField.columnName} = ?"
        val command = "DELETE FROM $tableName WHERE $whereClause"

        Logger.d { "[SQL] $command" }

        return connection.withStatement(command) {
            bindField(idField, 1, id)
            executeUpdateDelete()
        }
    }

    fun <E : Any> findAll(connection: SqliteConnection, metadata: EntityMetadata<E>, clazz: KClass<E>): List<E> {
        val tableName = metadata.tableName
        val fields = metadata.fieldMetadata.joinToString(",") { it.columnName }
        val command = "SELECT $fields FROM $tableName"

        Logger.d { "[SQL] $command" }

        return connection.withStatement(command) {
            val cursor = query()

            buildList {
                while (cursor.next()) {
                    add(metadata.create(cursor))
                }
            }
        }
    }

    fun <E : Any> findById(connection: SqliteConnection, metadata: EntityMetadata<E>, clazz: KClass<E>, id: Any): E? {
        val tableName = metadata.tableName
        val idField = metadata.fieldMetadata.first { it.fieldName == metadata.keyField }
        val fields = metadata.fieldMetadata.joinToString(",") { it.columnName }
        val command = "SELECT $fields FROM $tableName WHERE ${idField.columnName} = ?"

        Logger.d { "[SQL] $command" }

        return connection.withStatement(command) {
            bindField(idField, 1, id)
            val cursor = query()

            if (!cursor.next()) {
                return@withStatement null
            }

            metadata.create(cursor)
        }
    }

    fun <E : Any> exists(connection: SqliteConnection, metadata: EntityMetadata<E>, id: Any): Boolean {
        val tableName = metadata.tableName
        val idField = metadata.fieldMetadata.first { it.fieldName == metadata.keyField }
        val command = "SELECT 1 FROM $tableName WHERE ${idField.columnName} = ?"

        Logger.d { "[SQL] $command" }

        return connection.withStatement(command) {
            bindField(idField, 1, id)
            val cursor = query()

            cursor.next()
        }
    }

    private fun SqliteStatement.bindField(idField: FieldMetadata, fieldIndex: Int, data: Any) {
        bindByType(idField.fieldType, fieldIndex, data)
    }

    fun <E : Any> listForGenericType(
        connection: SqliteConnection,
        dataConsumer: (Array<Any?>) -> E,
        clazz: KClass<E>,
        query: String,
        vararg params: Any?
    ): List<E> {
        return connection.withStatement(query) {
            params.forEachIndexed { idx, singleParam ->
                if (singleParam != null) {
                    bindByType(singleParam::class, idx + 1, singleParam)
                } else {
                    bindNull(idx + 1)
                }
            }

            val cursor = query()
            val columnCount = cursor.getColumnCount()
            Logger.d { "Columns retrieved: ${(0 until columnCount).map { cursor.getColumnName(it) }}" }

            buildList {
                while (cursor.next()) {
                    val rowData = Array<Any?>(columnCount) { null }
                    for (idx in 0 until columnCount) {
                        val effectiveValue = readValueByColumnType(cursor, idx)
                        rowData[idx] = effectiveValue
                    }

                    if (columnCount > 1 || rowData.firstOrNull() != null) {
                        add(dataConsumer(rowData))
                    }
                }
            }
        }
    }

    fun <E : Any> findSingleForGenericType(
        connection: SqliteConnection,
        dataConsumer: (Array<Any?>) -> E,
        clazz: KClass<E>,
        query: String,
        vararg params: Any
    ): E? {
        val queryResults = listForGenericType(connection, dataConsumer, clazz, query, *params)
        return when (queryResults.size) {
            0 -> null
            1 -> queryResults.first()
            else -> throw IllegalArgumentException("Query mapping expected a single element but ${queryResults.size} were retrieved")
        }
    }

    fun readValueByColumnType(cursor: SqliteCursor, idx: Int): Any? {
        val columnType = cursor.getType(idx)

        return when (columnType) {
            ColumnType.TYPE_LONG -> cursor.getLong(idx)
            ColumnType.TYPE_DOUBLE -> cursor.getDouble(idx)
            ColumnType.TYPE_TEXT -> cursor.getString(idx)
            ColumnType.TYPE_BLOB -> cursor.getBytes(idx)

            ColumnType.TYPE_NULL -> {
                Logger.d { "Column of type null at index: $idx" }
                null
            }
        }
    }
}
