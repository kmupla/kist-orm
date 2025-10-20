package io.rss.knative.tools.kist

import co.touchlab.sqliter.DatabaseConfiguration
import co.touchlab.sqliter.DatabaseConnection
import co.touchlab.sqliter.DatabaseManager
import co.touchlab.sqliter.createDatabaseManager

object PersistenceContext {

    private var _connection: DatabaseConnection? = null

    val connection: DatabaseConnection
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
            _connection = dbManager.createMultiThreadedConnection()
        }
    }

}