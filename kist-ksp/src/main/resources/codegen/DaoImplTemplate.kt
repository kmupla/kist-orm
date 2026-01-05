package io.knative.kist.daos

import co.touchlab.sqliter.DatabaseConnection

import io.knative.kist.EntityMetadata
import io.knative.kist.DbOperations
import io.knative.kist.MetadataRegistry

import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.toInstant

import ${entity.qualifiedName}
import ${dao.qualifiedName}

@Suppress("UNCHECKED_CAST")
@OptIn(ExperimentalTime::class)
class ${dao.simpleName}Impl(private val connection: DatabaseConnection): ${dao.simpleName} {

    ${dao.standardMethods}

    ${dao.customQuery}
}