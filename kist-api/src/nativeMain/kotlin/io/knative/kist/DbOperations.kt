package io.knative.kist

import co.touchlab.kermit.Logger
import co.touchlab.sqliter.Cursor
import co.touchlab.sqliter.DatabaseConnection
import co.touchlab.sqliter.FieldType
import co.touchlab.sqliter.Statement
import co.touchlab.sqliter.withStatement
import kotlin.reflect.KClass

object DbOperations {

    fun <E> insert(connection: DatabaseConnection, metadata: EntityMetadata<E>, entity: E): Long {
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

    fun <E> update(connection: DatabaseConnection, metadata: EntityMetadata<E>, entity: E): Int {
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

    fun <E> deleteById(connection: DatabaseConnection, metadata: EntityMetadata<E>, id: Any): Int {
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

    fun <E : Any> findAll(connection: DatabaseConnection, metadata: EntityMetadata<E>, clazz: KClass<E>): List<E> {
        val tableName = metadata.tableName
        val fields = metadata.fieldMetadata.joinToString(",") { it.columnName }
        val command = "SELECT $fields FROM $tableName"

        Logger.d { "[SQL] $command" }

        return connection.withStatement(command) {
            val q = query()

            buildList {
                while (q.next()) {
                    add(metadata.create(q))
                }
            }
        }
    }

    fun <E : Any> findById(connection: DatabaseConnection, metadata: EntityMetadata<E>, clazz: KClass<E>, id: Any): E? {
        val tableName = metadata.tableName
        val idField = metadata.fieldMetadata.first { it.fieldName == metadata.keyField }
        val fields = metadata.fieldMetadata.joinToString(",") { it.columnName }
        val command = "SELECT $fields FROM $tableName WHERE ${idField.columnName} = ?"

        Logger.d { "[SQL] $command" }

        return connection.withStatement(command) {
            bindField(idField, 1, id)
            val q = query()

            if (!q.next()) {
                return@withStatement null
            }

            metadata.create(q)
        }
    }

    fun <E : Any> exists(connection: DatabaseConnection, metadata: EntityMetadata<E>, id: Any): Boolean {
        val tableName = metadata.tableName
        val idField = metadata.fieldMetadata.first { it.fieldName == metadata.keyField }
        val command = "SELECT 1 FROM $tableName WHERE ${idField.columnName} = ?"

        Logger.d { "[SQL] $command" }

        return connection.withStatement(command) {
            bindField(idField, 1, id)
            val q = query()

            q.next()
        }
    }

    private fun Statement.bindField(idField: FieldMetadata, fieldIndex: Int, data: Any) {
        bindAnyType(idField.fieldType, fieldIndex, data)
    }

    private fun Statement.bindAnyType(kClass: KClass<out Any>, fieldIndex: Int, actualValue: Any) {
        Logger.d { "bind: $kClass - $fieldIndex -> $actualValue" }

        when (kClass) {
            Int::class -> bindLong(fieldIndex, (actualValue as Int).toLong())
            Long::class -> bindLong(fieldIndex, actualValue as Long)
            Float::class -> bindDouble(fieldIndex, (actualValue as Float).toDouble())
            Double::class -> bindDouble(fieldIndex, actualValue as Double)
            String::class -> bindString(fieldIndex, actualValue as String)
            ByteArray::class -> bindBlob(fieldIndex, actualValue as ByteArray)

            else -> {
                if (actualValue is Enum<*>) {
                    bindString(fieldIndex, actualValue.name)
                } else {
                    throw UnsupportedOperationException("Unsupported field type: $kClass")
                }
            }
        }
    }

    fun <E : Any> listForGenericType(connection: DatabaseConnection, dataConsumer: (Array<Any?>) -> E, clazz: KClass<E>,
                                     query: String, vararg params: Any?): List<E> {
        return connection.withStatement(query) {
            params.forEachIndexed { idx, singleParam ->
                if (singleParam != null) {
                    bindAnyType(singleParam::class, idx + 1, singleParam)
                } else {
                    bindNull(idx + 1)
                }
            }

            val cursor = query()
            Logger.d { "Columns retrieved: ${cursor.columnNames}" }
            val columns = cursor.columnNames.map { it.value to it.key }.toMap() // invert key and index
            val maxIdx = columns.keys.maxOrNull() ?: -1

            buildList {
                while (cursor.next()) {
                    val rowData = Array<Any?>(maxIdx + 1) { null }
                    for (idx in 0 .. maxIdx) {
                        val effectiveValue = readValueByColumnType(cursor, idx, columns)
                        rowData[idx] = effectiveValue
                    }

                    if (columns.size > 1 || rowData.firstOrNull() != null) {
                        add(dataConsumer(rowData))
                    }
                }
            }
        }
    }

    fun <E : Any> findSingleForGenericType(
        connection: DatabaseConnection, dataConsumer: (Array<Any?>) -> E, clazz: KClass<E>,
        query: String, vararg params: Any
    ): E? {
        val queryResults = listForGenericType(connection, dataConsumer, clazz, query, *params)
        return when (queryResults.size) {
            0 -> null
            1 -> queryResults.first()
            else -> throw IllegalArgumentException("Query mapping expected a single element but ${queryResults.size} were retrieved")
        }
    }

    fun readValueByColumnType(cursor: Cursor, idx: Int, columns: Map<Int, String>): Any? {
        val columnType = cursor.getType(idx)

        return when (columnType) {
            FieldType.TYPE_INTEGER -> cursor.getLong(idx)
            FieldType.TYPE_FLOAT -> cursor.getDouble(idx)
            FieldType.TYPE_TEXT -> cursor.getString(idx)
            FieldType.TYPE_BLOB -> cursor.getBytes(idx)

            FieldType.TYPE_NULL -> {
                Logger.d { "Column of type null: ${columns[idx]}" }
                null
            }
        }
    }
}