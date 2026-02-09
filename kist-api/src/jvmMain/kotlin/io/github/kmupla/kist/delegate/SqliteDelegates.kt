package io.github.kmupla.kist.delegate

import io.github.kmupla.kist.ColumnType
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types

actual class SqliteConnection(private val connection: Connection) {
    actual fun <T> withStatement(sql: String, block: SqliteStatement.() -> T): T {
        return connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS).use { stmt ->
            SqliteStatement(stmt).block()
        }
    }
}

actual class SqliteStatement(private val statement: PreparedStatement) {
    private var resultSet: ResultSet? = null

    actual fun bindLong(index: Int, value: Long) {
        statement.setLong(index, value)
    }

    actual fun bindDouble(index: Int, value: Double) {
        statement.setDouble(index, value)
    }

    actual fun bindString(index: Int, value: String) {
        statement.setString(index, value)
    }

    actual fun bindBlob(index: Int, value: ByteArray) {
        statement.setBytes(index, value)
    }

    actual fun bindNull(index: Int) {
        statement.setNull(index, java.sql.Types.NULL)
    }

    actual fun executeInsert(): Long {
        statement.executeUpdate()
        val generatedKeys = statement.generatedKeys
        return if (generatedKeys.next()) {
            generatedKeys.getLong(1)
        } else {
            throw IllegalStateException("Failed to retrieve generated key")
        }
    }

    actual fun executeUpdateDelete(): Int {
        return statement.executeUpdate()
    }

    actual fun query(): SqliteCursor {
        resultSet = statement.executeQuery()
        return SqliteCursor(resultSet!!)
    }
}

actual class SqliteCursor(private val resultSet: ResultSet) {

    private val metadata = resultSet.metaData

    actual fun next(): Boolean = resultSet.next()

    actual fun getLong(index: Int): Long {
        return resultSet.getLong(index)
    }

    actual fun getDouble(index: Int): Double {
        return resultSet.getDouble(index)
    }

    actual fun getString(index: Int): String {
        return resultSet.getString(index)
    }

    actual fun getBytes(index: Int): ByteArray {
        return resultSet.getBytes(index)
    }

    actual fun isNull(index: Int): Boolean {
        resultSet.getObject(index)
        return resultSet.wasNull()
    }

    actual fun getColumnCount(): Int {
        return resultSet.metaData.columnCount
    }

    actual fun getColumnName(index: Int): String {
        return resultSet.metaData.getColumnName(index)
    }

    actual fun getType(index: Int): ColumnType {
        return when (metadata.getColumnType(index)) {
            Types.INTEGER,
            Types.SMALLINT,
            Types.BIGINT -> ColumnType.TYPE_LONG

            Types.FLOAT,
            Types.DOUBLE -> ColumnType.TYPE_DOUBLE

            Types.VARCHAR -> ColumnType.TYPE_TEXT

            Types.BINARY,
            Types.BLOB,
            Types.VARBINARY -> ColumnType.TYPE_BLOB

            else -> ColumnType.TYPE_NULL
        }
    }
}
