package io.knative.kist.config

abstract class PersistenceConfig(
    open val dbName: String,
    open val version: Int = 1,
    open val createStatements: List<String>,
    open val alterStatements: List<String> = emptyList()
)

class InMemoryConfig(
    dbName: String,
    version: Int = 1,
    createStatements: List<String>,
    alterStatements: List<String> = emptyList(),
): PersistenceConfig(dbName, version, createStatements, alterStatements)

class SqlLiteFileConfig(
    dbName: String,
    version: Int = 1,
    createStatements: List<String>,
    alterStatements: List<String> = emptyList(),
    val path: String,
): PersistenceConfig(dbName, version, createStatements, alterStatements)