@file:OptIn(ExperimentalTime::class)

package io.github.kmupla.kist.delegate

import io.github.kmupla.kist.ColumnType
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.reflect.KClass
import kotlin.time.ExperimentalTime

expect class SqliteConnection {
    fun <T> withStatement(sql: String, block: SqliteStatement.() -> T): T
}

expect class SqliteStatement {
    fun bindLong(index: Int, value: Long)
    fun bindDouble(index: Int, value: Double)
    fun bindString(index: Int, value: String)
    fun bindBlob(index: Int, value: ByteArray)
    fun bindNull(index: Int)

    fun executeInsert(): Long
    fun executeUpdateDelete(): Int
    fun query(): SqliteCursor
}

expect class SqliteCursor {
    fun next(): Boolean

    fun getLong(index: Int): Long
    fun getDouble(index: Int): Double
    fun getString(index: Int): String
    fun getBytes(index: Int): ByteArray

    fun getType(index: Int): io.github.kmupla.kist.ColumnType
    fun isNull(index: Int): Boolean
    fun getColumnCount(): Int
    fun getColumnName(index: Int): String
}

fun SqliteCursor.getColumnNames(): Map<String, Int> {
    return (0 until getColumnCount())
        .associateBy { getColumnName(it) }
}

// Helper to bind any type
fun SqliteStatement.bindByType(kClass: KClass<out Any>, fieldIndex: Int, actualValue: Any?) {
    if (actualValue == null) {
        bindNull(fieldIndex)
        return
    }

    when (kClass) {
        Int::class -> bindLong(fieldIndex, (actualValue as Int).toLong())
        Long::class -> bindLong(fieldIndex, actualValue as Long)
        Float::class -> bindDouble(fieldIndex, (actualValue as Float).toDouble())
        Double::class -> bindDouble(fieldIndex, actualValue as Double)
        String::class -> bindString(fieldIndex, actualValue as String)
        ByteArray::class -> bindBlob(fieldIndex, actualValue as ByteArray)
        Boolean::class -> bindLong(fieldIndex, if (actualValue as Boolean) 10 else 0L)

        LocalDateTime::class -> bindLong(fieldIndex,
            (actualValue as LocalDateTime).toInstant(TimeZone.currentSystemDefault()).epochSeconds)

        else -> {
            if (actualValue is Enum<*>) {
                bindString(fieldIndex, actualValue.name)
            } else {
                throw UnsupportedOperationException("Unsupported field type: $kClass")
            }
        }
    }
}
