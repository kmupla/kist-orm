package io.github.kmupla.kist.config

import co.touchlab.sqliter.DatabaseConfiguration
import co.touchlab.sqliter.DatabaseManager
import co.touchlab.sqliter.createDatabaseManager
import io.github.kmupla.kist.delegate.SqliteConnection

object PersistenceContext {

    private var _connection: SqliteConnection? = null

    val connection: SqliteConnection
        get() = _connection ?: error("DatabaseConnection not initialized. Call createConnection() first.")

    fun createConnection(config: PersistenceConfig) {
        with(config) {
            val dbConfig = DatabaseConfiguration(
                name = dbName,
                version = version,
                inMemory = config is InMemoryConfig,

                extendedConfig = DatabaseConfiguration.Extended(
                    basePath = (config as? SqlLiteFileConfig)?.path
                ),

                create = { connection ->
                    createStatements.forEach { stt ->
                        connection.createStatement(stt).execute()
                    }
                },
                upgrade = { connection, oldVersion, newVersion ->
                    alterStatements.forEach { stt ->
                        connection.createStatement(stt).execute()
                    }
                },
            )

            val dbManager: DatabaseManager = createDatabaseManager(dbConfig)
            _connection = SqliteConnection (dbManager.createMultiThreadedConnection())
        }
    }

}