package io.rss.knative.tools.kist.ksp

import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.google.devtools.ksp.validate
import io.rss.knative.tools.kist.Column
import io.rss.knative.tools.kist.Entity
import io.rss.knative.tools.kist.PrimaryKeyColumn

class EntityClassVisitor(private val environment: SymbolProcessorEnvironment,
                         private val resultMap: MutableMap<KSName, String>) : KSVisitorVoid() {

    private val templateContent: String
     init {
         val templatePath = "codegen/MetadataTemplate.kt"
         templateContent = this::class.java.classLoader.getResourceAsStream(templatePath)?.bufferedReader()?.readText()
             ?: throw IllegalStateException("Template file $templatePath not found in resources")
     }

    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
        environment.logger.info("Processing class: ${classDeclaration.simpleName.asString()}")

        if (!classDeclaration.validate()) {
            environment.logger.error("Class is invalid. Aborting processing")
            return
        }

        val tableName = classDeclaration.annotations.find {
            it.annotationType.resolve().declaration.qualifiedName?.asString() == Entity::class.qualifiedName
        }
            ?.arguments
            ?.find { it.name?.asString() == "tableName" }
            ?.value as? String
            ?: throw IllegalArgumentException("Table name is required in an Entity")

        val primaryKeyColumnFields = classDeclaration.getAllProperties()
            .filter { field -> field.annotations.any { it.annotationType.resolve().declaration.qualifiedName?.asString() == PrimaryKeyColumn::class.qualifiedName } }
            .toList()
        // assert it's only 1
        val primaryKeyFieldName = primaryKeyColumnFields.first().simpleName

        val fields = classDeclaration.getAllProperties()
            .also {
                println("Properties: ${it.joinToString { it.simpleName.asString() }}")
            }
            .map { field ->
                val columnSpec = field.annotations.find {
                    val annQualifiedName = it.annotationType.resolve().declaration.qualifiedName?.asString()
                    annQualifiedName == Column::class.qualifiedName || annQualifiedName == PrimaryKeyColumn::class.qualifiedName
                }
                field to columnSpec
            }
            .filter { it.second != null }
            .map { fieldPair ->
                val (field, annotation) = fieldPair

                val columnName = annotation?.arguments
                    ?.find { it.name?.asString() == "name" }
                    ?.value as? String
                    ?: field.simpleName.asString()

                val isPrimary = annotation?.annotationType?.resolve()?.declaration?.simpleName?.asString() == PrimaryKeyColumn::class.simpleName
                val fieldType = field.type.resolve()
                Metadata(field.simpleName.asString(), columnName,
                    fieldType.declaration.qualifiedName ?: fieldType.declaration.simpleName,
                    fieldType.isMarkedNullable,
                    isPrimary)
            }

        val fieldMetadataList = fields.map { meta ->
            """
            FieldMetadata(
                fieldName = "${meta.simpleName}",
                columnName = "${meta.column}",
                fieldType = ${meta.type.asString()}::class,
                nullable = ${meta.nullable},
            )
            """.trimEnd()
        }

        val classConstructorAssignments = fields.map { fd ->
            val castDeclaration = when (fd.type.asString()) {
                String::class.qualifiedName -> "(columnValues[\"${fd.column}\"] as String)"
                Boolean::class.qualifiedName -> "(columnValues[\"${fd.column}\"] as Boolean)"
                Long::class.qualifiedName -> "(columnValues[\"${fd.column}\"] as Long)"
                Double::class.qualifiedName -> "(columnValues[\"${fd.column}\"] as Double)"
                Int::class.qualifiedName -> "(columnValues[\"${fd.column}\"] as Long).toInt()"
                Float::class.qualifiedName -> "(columnValues[\"${fd.column}\"] as Double).toFloat()"
                else -> "" // no as - Idk what could happen
            }
            """${fd.simpleName} = $castDeclaration"""
        }

        val bindings = fields.map { fd ->
            val bindingFnToGetValue = when (fd.type.asString()) {
                String::class.qualifiedName -> "bindString" to fd.simpleName
                Int::class.qualifiedName -> "bindLong" to "${fd.simpleName}.toLong()"
                Long::class.qualifiedName -> "bindLong" to fd.simpleName
                Float::class.qualifiedName -> "bindDouble" to "${fd.simpleName}.toDouble()"
                Double::class.qualifiedName -> "bindDouble" to fd.simpleName
                Boolean::class.qualifiedName -> "bindString" to "${fd.simpleName}.toString()" // TODO check
                else -> "bindNull" to fd.simpleName
            }

            val (bindingFn, fieldReference) = bindingFnToGetValue

            """
            if ("${fd.simpleName}" in fieldIndexMap) {
//                print("binding " + source.${fieldReference} + " / ")
//                println(fieldIndexMap.getValue("${fd.simpleName}"))
                statement.$bindingFn(fieldIndexMap.getValue("${fd.simpleName}"), source.${fieldReference})
            }
            """.trimEnd()
        }

        val resultSource = templateContent
            .replace("\${entity.qualifiedName}", (classDeclaration.qualifiedName ?: classDeclaration.simpleName).asString())
            .replace("\${entity.simpleName}", classDeclaration.simpleName.asString())
            .replace("\${entity.tableName}", tableName)
            .replace("\${entity.keyField}", primaryKeyFieldName.asString())
            .replace("\${fieldMetadataList}", fieldMetadataList.joinToString(", "))
            .replace("\${classConstructorAssignments}", classConstructorAssignments.joinToString(", "))
            .replace("\${bindingsDeclarations}", bindings.joinToString(System.lineSeparator()))

        resultMap[classDeclaration.qualifiedName ?: classDeclaration.simpleName] = resultSource
    }
}

private data class Metadata(
    val simpleName: String,
    val column: String,
    val type: KSName,
    val nullable: Boolean,
    val primaryKey: Boolean = false
)