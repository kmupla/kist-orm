package io.rss.knative.tools.kist

import co.touchlab.sqliter.DatabaseConnection
import co.touchlab.sqliter.FieldType
import co.touchlab.sqliter.Statement
import co.touchlab.sqliter.interop.SQLiteExceptionErrorCode
import co.touchlab.sqliter.withStatement
import kotlin.reflect.KClass

object DbOperations {

    fun <E> insert(connection: DatabaseConnection, metadata: EntityMetadata<E>, entity: E): Long {
        val tableName = metadata.tableName
        val fields = metadata.fieldMetadata.joinToString(",") { it.columnName }
        val placeholders = metadata.fieldMetadata.joinToString(",") { "?" }
        val command = "INSERT INTO $tableName ($fields) VALUES ($placeholders)"

        println(command) //FIXME remove

        return connection.withStatement(command) {
            val fieldToIndex = metadata.fieldMetadata.withIndex().associate { it.value.fieldName to (it.index + 1) }
            metadata.bindFields(entity, this, fieldToIndex)
            executeInsert()
        }
    }

    fun <E> update(connection: DatabaseConnection, metadata: EntityMetadata<E>, entity: E): Int {
        val tableName = metadata.tableName
        val fields = metadata.fieldMetadata.joinToString(",") { "${it.columnName} = ?" }
        val command = "UPDATE $tableName SET $fields"

        return connection.withStatement(command) {
            val fieldToIndex = metadata.fieldMetadata.withIndex().associate { it.value.fieldName to (it.index + 1) }
            metadata.bindFields(entity, this, fieldToIndex)
            executeUpdateDelete()
        }
    }

    fun <E> deleteById(connection: DatabaseConnection, metadata: EntityMetadata<E>, id: Any): Int {
        val tableName = metadata.tableName
        val idField = metadata.fieldMetadata.first { it.fieldName == metadata.keyField }
        val whereClause = "${idField.columnName} = ?"
        val command = "DELETE FROM $tableName WHERE $whereClause"

        return connection.withStatement(command) {
            bindField(idField, 1, id)
            executeUpdateDelete()
        }
    }

    fun <E : Any> findAll(connection: DatabaseConnection, metadata: EntityMetadata<E>, clazz: KClass<E>): List<E> {
        val tableName = metadata.tableName
        val fields = metadata.fieldMetadata.joinToString(",") { it.columnName }
        val command = "SELECT $fields FROM $tableName"

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

        return connection.withStatement(command) {
            bindField(idField, 1, id)
            val q = query()

            if (!q.next()) {
                return@withStatement null
            }

            metadata.create(q)
        }
    }

    fun <E : Any> listByQuery(connection: DatabaseConnection, metadata: EntityMetadata<E>, clazz: KClass<E>,
                              query: String, vararg params: Any): List<E> {
        // TODO: check, can be removed?
//        val fields = metadata.fieldMetadata.joinToString(",") { "${it.columnName} = ?" }

        return connection.withStatement(query) {
            params.forEachIndexed { idx, singleParam ->
                bindAnyType(singleParam::class, idx + 1, singleParam)
            }
            val q = query()

            buildList {
                while (q.next()) {
                    add(metadata.create(q))
                }
            }
        }
    }

    private fun Statement.bindField(idField: FieldMetadata, fieldIndex: Int, data: Any) {
        bindAnyType(idField.fieldType, fieldIndex, data)
    }

    private fun Statement.bindAnyType(kClass: KClass<out Any>, fieldIndex: Int, actualValue: Any) {
        when (kClass) {
            Int::class -> bindLong(fieldIndex, (actualValue as Int).toLong())
            Long::class -> bindLong(fieldIndex, actualValue as Long)
            Float::class -> bindDouble(fieldIndex, (actualValue as Float).toDouble())
            Float::class -> bindDouble(fieldIndex, actualValue as Double)
            String::class -> bindString(fieldIndex, actualValue as String)
            else -> throw UnsupportedOperationException("Unsupported field type")
        }
    }

    fun <E : Any> listForGenericType(connection: DatabaseConnection, dataConsumer: (Array<Any?>) -> E, clazz: KClass<E>,
                                     query: String, vararg params: Any): List<E> {
        return connection.withStatement(query) {
            params.forEachIndexed { idx, singleParam ->
                bindAnyType(singleParam::class, idx + 1, singleParam)
            }

            val cursor = query()
            val columns = cursor.columnNames.map { it.value to it.key }.toMap() // invert key and index
            val maxIdx = columns.maxOf { it.key }

            buildList {
                while (cursor.next()) {
                    val rowData = Array<Any?>(maxIdx + 1) { null }
                    for (idx in 0 .. maxIdx) {
                        val cType = cursor.getType(idx)

                        // SAME as in metadata - refactor / reuse
                        val effectiveValue = when (cType) {
                            FieldType.TYPE_INTEGER -> cursor.getLong(idx)
                            FieldType.TYPE_FLOAT -> cursor.getDouble(idx)
                            FieldType.TYPE_TEXT -> cursor.getString(idx)
                            FieldType.TYPE_NULL -> {
                                // ??
                            }
                            else -> cursor.getBytes(idx)
                        }

                        rowData[idx] = effectiveValue
                    }

                    add(dataConsumer(rowData))
                }
            }
        }
    }
}