package io.github.kmupla.kist.daos

import io.github.kmupla.kist.delegate.SqliteConnection

import io.github.kmupla.kist.EntityMetadata
import io.github.kmupla.kist.DbOperations
import io.github.kmupla.kist.MetadataRegistry

import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.toInstant

import ${entity.qualifiedName}
import ${dao.qualifiedName}

@Suppress("UNCHECKED_CAST")
@OptIn(ExperimentalTime::class)
class ${dao.simpleName}Impl(private val connection: SqliteConnection): ${dao.simpleName} {

    ${dao.standardMethods}

    ${dao.customQuery}
}