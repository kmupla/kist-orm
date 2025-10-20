package io.rss.knative.tools.kist.daos

import co.touchlab.sqliter.DatabaseConnection

import io.rss.knative.tools.kist.EntityMetadata
import io.rss.knative.tools.kist.DbOperations
import io.rss.knative.tools.kist.MetadataRegistry

import ${entity.qualifiedName}
import ${dao.qualifiedName}

@Suppress("UNCHECKED_CAST")
class ${dao.simpleName}Impl(private val connection: DatabaseConnection): ${dao.simpleName} {

    ${dao.standardMethods}

    ${dao.customQuery}
}