package io.github.kmupla.kist.ksp

import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.google.devtools.ksp.validate
import io.github.kmupla.kist.Column
import io.github.kmupla.kist.Entity
import io.github.kmupla.kist.PrimaryKeyColumn

class EntityClassVisitor(
    private val environment: SymbolProcessorEnvironment,
    private val resultMap: MutableMap<KSName, String>,
    private val templateContent: String
) : KSVisitorVoid() {

    constructor(environment: SymbolProcessorEnvironment, resultMap: MutableMap<KSName, String>) : this(
        environment,
        resultMap,
        EntityClassVisitor::class.java.classLoader.getResourceAsStream("codegen/MetadataTemplate.kt")?.bufferedReader()
            ?.readText()
            ?: throw IllegalStateException("Template file codegen/MetadataTemplate.kt not found in resources")
    )

    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
        environment.logger.info("Processing class: ${classDeclaration.simpleName.asString()}")

        if (!classDeclaration.validate()) {
            environment.logger.error("Class is invalid. Aborting processing")
            return
        }

        val tableName = classDeclaration.annotations.find {
            it.annotationType.resolve().declaration.qualifiedName?.asString() == _root_ide_package_.io.github.kmupla.kist.Entity::class.qualifiedName
        }
            ?.arguments
            ?.find { it.name?.asString() == "tableName" }
            ?.value as? String
            ?: throw IllegalArgumentException("Table name is required in an Entity")

        val primaryKeyColumnFields = classDeclaration.getAllProperties()
            .filter { field -> field.annotations.any { it.annotationType.resolve().declaration.qualifiedName?.asString() == _root_ide_package_.io.github.kmupla.kist.PrimaryKeyColumn::class.qualifiedName } }
            .toList()
        if (primaryKeyColumnFields.size != 1) {
            throw IllegalArgumentException("Entity must have exactly one PrimaryKeyColumn")
        }
        val primaryKeyFieldName = primaryKeyColumnFields.first().simpleName

        val fields = classDeclaration.getAllProperties()
            .also {
                println("Properties: ${it.joinToString { it.simpleName.asString() }}")
            }
            .map { field ->
                val columnSpec = field.annotations.find {
                    val annQualifiedName = it.annotationType.resolve().declaration.qualifiedName?.asString()
                    annQualifiedName == _root_ide_package_.io.github.kmupla.kist.Column::class.qualifiedName || annQualifiedName == _root_ide_package_.io.github.kmupla.kist.PrimaryKeyColumn::class.qualifiedName
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

                val isPrimary = annotation?.annotationType?.resolve()?.declaration?.simpleName?.asString() == _root_ide_package_.io.github.kmupla.kist.PrimaryKeyColumn::class.simpleName
                val fieldType = field.type.resolve()

                val typeName = fieldType.declaration.qualifiedName ?: fieldType.declaration.simpleName
                val isEnum = (fieldType.declaration as? KSClassDeclaration)?.classKind == ClassKind.ENUM_CLASS

                Metadata(
                    simpleName = field.simpleName.asString(),
                    column = columnName,
                    type = typeName,
                    nullable = fieldType.isMarkedNullable,
                    enum = isEnum,
                    primaryKey = isPrimary
                )
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
            val optMark = "?".takeIf { fd.nullable } ?: ""

            val castDeclaration = when (fd.type.asString()) {
                String::class.qualifiedName -> "(columnValues[\"${fd.column}\"] as String$optMark)"
                Boolean::class.qualifiedName -> "(columnValues[\"${fd.column}\"] as Long$optMark)$optMark.let { it != 0L }"
                Long::class.qualifiedName -> "(columnValues[\"${fd.column}\"] as Long$optMark)"
                Double::class.qualifiedName -> "(columnValues[\"${fd.column}\"] as Double$optMark)"
                Int::class.qualifiedName -> "(columnValues[\"${fd.column}\"] as Long$optMark)$optMark.toInt()"
                Float::class.qualifiedName -> "(columnValues[\"${fd.column}\"] as Double$optMark)$optMark.toFloat()"
                ByteArray::class.qualifiedName -> "(columnValues[\"${fd.column}\"] as ByteArray$optMark)"

                "kotlinx.datetime.LocalDateTime" -> "(columnValues[\"${fd.column}\"] as Long$optMark)$optMark.let (Instant::fromEpochSeconds)$optMark.toLocalDateTime(TimeZone.currentSystemDefault())"

                else -> {
                    if (fd.enum) {
                        "${fd.type.asString()}.valueOf(columnValues[\"${fd.column}\"] as String)"
                    } else {
                        println("Constructor type: ${fd.type.asString()} is not supported by Kist. Null will be passed instead.")
                        null
                    }
                }
            }
            """${fd.simpleName} = $castDeclaration"""
        }

        val bindings = fields.map { fd ->
            val optMark = "?".takeIf { fd.nullable } ?: ""

            val bindingFnToGetValue = when (fd.type.asString()) {
                String::class.qualifiedName -> "bindString" to fd.simpleName
                Int::class.qualifiedName -> "bindLong" to "${fd.simpleName}$optMark.toLong()"
                Long::class.qualifiedName -> "bindLong" to fd.simpleName
                Float::class.qualifiedName -> "bindDouble" to "${fd.simpleName}$optMark.toDouble()"
                Double::class.qualifiedName -> "bindDouble" to fd.simpleName
                Boolean::class.qualifiedName -> "bindLong" to "${fd.simpleName}$optMark.let { if (it) 1L else 0L }"
                ByteArray::class.qualifiedName -> "bindBlob" to fd.simpleName

                "kotlinx.datetime.LocalDateTime" -> "bindLong" to "${fd.simpleName}$optMark.toInstant(TimeZone.currentSystemDefault())$optMark.epochSeconds"

                else -> {
                    if (fd.enum) {
                        "bindString" to "${fd.simpleName}.name"
                    } else {
                        "bindNull" to fd.simpleName
                    }
                }
            }

            val (bindingFn, fieldReference) = bindingFnToGetValue

            $$"""
        if ("$${fd.simpleName}" in fieldIndexMap) {
            val fieldIndex = fieldIndexMap.getValue("$${fd.simpleName}")
            Logger.d { "binding value [${source.$${fieldReference}}] on idx $fieldIndex" }
            statement.$$bindingFn(fieldIndex, source.$${fieldReference})
        }
            """.trimEnd()
        }

        val resultSource = templateContent
            .replace($$"${entity.qualifiedName}", (classDeclaration.qualifiedName ?: classDeclaration.simpleName).asString())
            .replace($$"${entity.simpleName}", classDeclaration.simpleName.asString())
            .replace($$"${entity.tableName}", tableName)
            .replace($$"${entity.keyField}", primaryKeyFieldName.asString())
            .replace($$"${fieldMetadataList}", fieldMetadataList.joinToString(", "))
            .replace($$"${classConstructorAssignments}", classConstructorAssignments.joinToString(",\n" + " ".repeat(12)))
            .replace($$"${bindingsDeclarations}", bindings.joinToString(System.lineSeparator()))

        resultMap[classDeclaration.qualifiedName ?: classDeclaration.simpleName] = resultSource
    }

}

private data class Metadata(
    val simpleName: String,
    val column: String,
    val type: KSName,
    val nullable: Boolean,
    val enum: Boolean = false,
    val primaryKey: Boolean = false,
)