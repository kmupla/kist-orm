package io.github.kmupla.kist.config

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
): io.github.kmupla.kist.config.PersistenceConfig(dbName, version, createStatements, alterStatements)

class SqlLiteFileConfig(
    dbName: String,
    version: Int = 1,
    createStatements: List<String>,
    alterStatements: List<String> = emptyList(),
    val path: String,
): io.github.kmupla.kist.config.PersistenceConfig(dbName, version, createStatements, alterStatements)