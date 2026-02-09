package io.github.kmupla.kist.delegate

import co.touchlab.sqliter.Cursor
import co.touchlab.sqliter.DatabaseConnection
import co.touchlab.sqliter.FieldType
import co.touchlab.sqliter.Statement
import co.touchlab.sqliter.withStatement
import io.github.kmupla.kist.ColumnType

actual open class SqliteConnection(private val connection: DatabaseConnection) {
    actual fun <T> withStatement(sql: String, block: SqliteStatement.() -> T): T {
        return connection.withStatement(sql) {
            SqliteStatement(this).block()
        }
    }
}

actual open class SqliteStatement(private val statement: Statement) {
    private var cursor: Cursor? = null

    actual fun bindLong(index: Int, value: Long) {
        statement.bindLong(index, value)
    }

    actual fun bindDouble(index: Int, value: Double) {
        statement.bindDouble(index, value)
    }

    actual fun bindString(index: Int, value: String) {
        statement.bindString(index, value)
    }

    actual fun bindBlob(index: Int, value: ByteArray) {
        statement.bindBlob(index, value)
    }

    actual fun bindNull(index: Int) {
        statement.bindNull(index)
    }

    actual fun executeInsert(): Long {
        return statement.executeInsert()
    }

    actual fun executeUpdateDelete(): Int {
        return statement.executeUpdateDelete()
    }

    actual fun query(): SqliteCursor {
        cursor = statement.query()
        return SqliteCursor(cursor!!)
    }
}

actual open class SqliteCursor(private val cursor: Cursor) {

    private val columnIndices: Map<Int, String> by lazy {
        cursor.columnNames.map { it.value to it.key }.toMap()
    }

    actual fun next(): Boolean = cursor.next()

    actual fun getLong(index: Int): Long {
        return cursor.getLong(index)
    }

    actual fun getDouble(index: Int): Double {
        return cursor.getDouble(index)
    }

    actual fun getString(index: Int): String {
        return cursor.getString(index)
    }

    actual fun getBytes(index: Int): ByteArray {
        return cursor.getBytes(index)
    }

    actual fun isNull(index: Int): Boolean {
        return cursor.getType(index) == FieldType.TYPE_NULL
    }

    actual fun getColumnCount(): Int {
        return cursor.columnNames.size
    }

    actual fun getColumnName(index: Int): String {
        return columnIndices[index] ?: throw IndexOutOfBoundsException("Column index $index not found")
    }

    actual fun getType(index: Int): ColumnType {
        return when (cursor.getType(index)) {
            FieldType.TYPE_INTEGER -> ColumnType.TYPE_LONG
            FieldType.TYPE_FLOAT -> ColumnType.TYPE_DOUBLE
            FieldType.TYPE_BLOB -> ColumnType.TYPE_BLOB
            FieldType.TYPE_TEXT -> ColumnType.TYPE_TEXT
            FieldType.TYPE_NULL -> ColumnType.TYPE_NULL
        }
    }
}
