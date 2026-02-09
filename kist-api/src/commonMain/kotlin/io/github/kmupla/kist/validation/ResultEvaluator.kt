package io.github.kmupla.kist.validation

import io.github.kmupla.kist.FieldMetadata

object ResultEvaluator {

    fun assertRequiredColumnsPresent(queryColumns: Map<String, Int>, fieldMetadata: List<io.github.kmupla.kist.FieldMetadata>) {
        val requiredColumns = fieldMetadata.filterNot { it.nullable }.map { it.columnName }

        require(requiredColumns.all { it in queryColumns.keys }) {
            val missingColumns = requiredColumns.filter { it !in queryColumns.keys }
            "The query must return all non-null fields of the target entity. " +
            "Following are present: ${queryColumns}. "
            "But [$missingColumns] are missing."
        }
    }
}