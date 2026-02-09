package io.github.kmupla.kist.config

import io.github.kmupla.kist.delegate.SqliteConnection
import java.sql.DriverManager

object PersistenceContext {

    private var _connection: SqliteConnection? = null

    val connection: SqliteConnection
        get() = _connection ?: error("Connection not initialized. Call createConnection() first.")

    fun createConnection(config: PersistenceConfig) {
        with(config) {
            val url = when (config) {
                is SqlLiteFileConfig -> "jdbc:sqlite:${config.path}/$dbName"
                else -> "jdbc:sqlite::memory:"
            }

            Class.forName("org.sqlite.JDBC")
            val conn = DriverManager.getConnection(url)

            createStatements.forEach { stt ->
                conn.createStatement().use { statement ->
                    statement.execute(stt)
                }
            }

            alterStatements.forEach { stt ->
                conn.createStatement().use { statement ->
                    statement.execute(stt)
                }
            }

            _connection = SqliteConnection(conn!!)
        }
    }

}