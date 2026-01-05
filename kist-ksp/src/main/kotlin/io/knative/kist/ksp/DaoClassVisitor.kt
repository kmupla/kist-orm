package io.knative.kist.ksp

import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import io.knative.kist.KistDao
import io.knative.kist.Query
import kotlin.io.resolve

class DaoClassVisitor(
    private val environment: SymbolProcessorEnvironment,
    private val resultMap: MutableMap<KSName, String>,
    private val templateContent: String
) : KSVisitorVoid() {

    constructor(environment: SymbolProcessorEnvironment, resultMap: MutableMap<KSName, String>) : this(
        environment,
        resultMap,
        DaoClassVisitor::class.java.classLoader.getResourceAsStream("codegen/DaoImplTemplate.kt")?.bufferedReader()
            ?.readText()
            ?: throw IllegalStateException("Template file codegen/DaoImplTemplate.kt not found in resources")
    )

    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
        environment.logger.info("Processing DAO interface: ${classDeclaration.simpleName.asString()}")

        if (!classDeclaration.validate()) {
            environment.logger.error("Class is invalid. Aborting processing")
            return
        }

        val kistDaoDecl = classDeclaration.superTypes.find { it.resolve().declaration.qualifiedName?.asString() == _root_ide_package_.io.knative.kist.KistDao::class.qualifiedName }
            ?: return fail("Class ${classDeclaration.qualifiedName} must implement KistDao")
        val (targetEntity, entityKeyType) = kistDaoDecl.resolve().arguments.map { it.type?.resolve() }
        requireNotNull(targetEntity)
        requireNotNull(entityKeyType)

        val standardMethods = buildStandardMethods(targetEntity, entityKeyType)
        val customQueries = processCustomTypes(classDeclaration)

        val resultSource = templateContent
            .replace($$"${entity.qualifiedName}", targetEntity.declaration.qualifiedName?.asString() ?: "")
            .replace($$"${dao.simpleName}", classDeclaration.simpleName.asString())
            .replace($$"${dao.qualifiedName}", (classDeclaration.qualifiedName ?: classDeclaration.simpleName).asString())
            .replace($$"${dao.standardMethods}", standardMethods)
            .replace($$"${dao.customQuery}", customQueries.joinToString (System.lineSeparator()))

        resultMap[classDeclaration.qualifiedName ?: classDeclaration.simpleName] = resultSource
    }

    private fun processCustomTypes(classDeclaration: KSClassDeclaration): List<String> = classDeclaration.getDeclaredFunctions()
        .associateWith { target ->
            target.annotations.find { it.annotationType.resolve().declaration.qualifiedName?.asString() == Query::class.qualifiedName }
        }
        .filter { it.value != null }
        .mapNotNull { buildQueryFunction(it.key, it.value?.arguments?.firstOrNull()) }

    private fun buildQueryFunction(
        functionDeclaration: KSFunctionDeclaration,
        queryValue: KSValueArgument?
    ): String? {
        if (queryValue == null) {
            return null
        }

        val returnType = functionDeclaration.returnType?.resolve()?.declaration
            ?: throw IllegalArgumentException("A query function must have a return type declared")

        if (returnType.typeParameters.isNotEmpty()) {
            require(returnType.qualifiedName?.asString() == List::class.qualifiedName) {
                "Query functions must return either a single element (like an Entity) or List<E>"
            }
        }

        val query = queryValue.value as String
        val (signature, targetType) = getFunctionSignature(functionDeclaration)
        val returnMany = returnType.qualifiedName?.asString() == List::class.qualifiedName

        val parameterNames = functionDeclaration.parameters
            .map { it.name?.getShortName() }
            .joinToString (",")

        val constructorParams = buildReturnTypeConstructor(functionDeclaration.returnType)

        return writeCustomQueryResult(signature, targetType, constructorParams, parameterNames, query, returnMany)
    }

    private fun writeCustomQueryResult(
        signature: String, targetType: String?,
        constructorParams: String, parameterNames: String,
        query: String,
        returnMany: Boolean,
    ) = buildString {
        append("     ")
        append("override ")
        append(signature)

        val dbOperation = "listForGenericType".takeIf { returnMany } ?: "findSingleForGenericType"
        val primitiveResult = targetType in SUPPORTED_PRIMITIVE_TYPES

        val resultFnBody = when {
            primitiveResult -> """data[0] as $targetType"""

            targetType == Unit::class.qualifiedName -> """Unit"""
            targetType == "kotlinx.datetime.LocalDateTime" -> """
                (data[0] as Long).let (Instant::fromEpochSeconds).toLocalDateTime(TimeZone.currentSystemDefault())""".trimIndent()

            else -> """
            $targetType (
                ${constructorParams.takeIf { it.isNotEmpty() } ?: ""}
            )
            """
        }

        append(
            """ {
            fun createFromGenericData(data: Array<Any?>) = ${resultFnBody.prependIndent()}

             val result = DbOperations.$dbOperation(connection, ::createFromGenericData,
                ${targetType}::class,
                ${"\"\"\""}
                    ${query.prependIndent()}
                ${"\"\"\""}, $parameterNames)
                        
        """)

        if (targetType == Unit::class.qualifiedName) {
            append("     return")
        } else {
            append("     return result")
        }
        append("\n         }")
    }

    private fun buildReturnTypeConstructor(returnType: KSTypeReference?): String {
        val returnKSType = returnType?.resolve()
        val entityType = if (returnKSType?.arguments?.isNotEmpty() == true) {
            returnKSType.arguments.first().type?.resolve()
        } else {
            returnKSType
        }

        val classDecl = entityType?.declaration as? KSClassDeclaration
        val primaryConstructor = classDecl?.primaryConstructor
        val constructorParams = primaryConstructor?.parameters
            ?.mapIndexed(::getSingleTypeDeclarationFactory)
            ?.joinToString(",\n                    ") ?: ""
        return constructorParams
    }

    private fun getSingleTypeDeclarationFactory(index: Int, param: KSValueParameter): String {
        val paramType = param.type.resolve()
        val isEnum = (paramType.declaration as? KSClassDeclaration)?.classKind == ClassKind.ENUM_CLASS
        val typeName = paramType.declaration.qualifiedName?.asString()
        val optionalMark = "?".takeIf { paramType.isMarkedNullable } ?: ""

        val cast = when (typeName) {
            String::class.qualifiedName -> "data[$index] as String$optionalMark"
            Boolean::class.qualifiedName -> "(data[$index] as Long$optionalMark)$optionalMark.let { if (it == 0L) false else true }"
            Long::class.qualifiedName -> "data[$index] as Long$optionalMark"
            Double::class.qualifiedName -> "data[$index] as Double$optionalMark"
            Int::class.qualifiedName -> "(data[$index] as Long$optionalMark)$optionalMark.toInt()"
            Float::class.qualifiedName -> "(data[$index] as Double$optionalMark)$optionalMark.toFloat()"

            "kotlinx.datetime.LocalDateTime" -> "(data[$index] as Long$optionalMark)$optionalMark.let (Instant::fromEpochSeconds)$optionalMark.toLocalDateTime(TimeZone.currentSystemDefault())"

            else -> {
                if (isEnum) {
                    "${typeName}.valueOf(data[$index] as String)"
                } else {
                    "data[$index] as ${typeName!!}$optionalMark"
                }
            }
        }

        return "    $cast"
    }

    private fun getFunctionSignature(functionDeclaration: KSFunctionDeclaration): CustomSignature {
        val modifiers = functionDeclaration.modifiers.joinToString(" ") { it.name.lowercase() }
        val functionName = functionDeclaration.simpleName.asString()

        val parameters = functionDeclaration.parameters.joinToString(", ") { parameter ->
            val parameterName = parameter.name?.asString()
            val ksType = parameter.type.resolve()
            val parameterType = ksType.declaration.qualifiedName?.asString()
            val typeParams = ksType.arguments
                .joinToString(", ") { arg ->
                    arg.type?.resolve()?.declaration?.qualifiedName?.asString() ?: arg.type.toString()
                }
                .takeIf { it.isNotEmpty() }
                ?.let { "<$it>" }
                ?: ""
            val optMark = "?".takeIf { ksType.isMarkedNullable } ?: ""
            "$parameterName: $parameterType$typeParams$optMark"
        }

        val returnType = functionDeclaration.returnType?.resolve()
        val returnStrClass = returnType?.declaration?.qualifiedName?.asString()
        val optionalMark = "?".takeIf { returnType?.isMarkedNullable == true } ?: ""
        val targetEntity = returnType?.arguments?.firstOrNull()?.type?.resolve()?.declaration?.qualifiedName?.asString()
            ?:returnStrClass ?: ""

        val resultSignature = buildString {
            append("$modifiers fun $functionName ($parameters)")

            if (returnStrClass != null && returnStrClass != "kotlin.Unit") {
                val returnTypeWithParam = returnStrClass.takeIf { it == targetEntity }?.plus(optionalMark)
                    ?: "$returnStrClass<$targetEntity>"

                append(": ")
                append(returnTypeWithParam)
            }
        }

        return CustomSignature(resultSignature, targetEntity)
    }

    private fun buildStandardMethods(targetEntity: KSType, entityKeyType: KSType): String {
        val entitySimpleName = targetEntity.declaration.simpleName.asString()
        val keyType = entityKeyType.declaration.simpleName.asString()

        return buildString {
            buildInsertFunction(entitySimpleName).also (::append)
            buildUpdateFunction(entitySimpleName).also (::append)
            buildDeleteFunction(keyType, entitySimpleName).also (::append)
            buildFindAllFn(entitySimpleName).also (::append)
            buildFindByIdFn(keyType, entitySimpleName).also (::append)
            buildExistsFn(keyType, entitySimpleName).also (::append)
        }
    }

    private fun buildInsertFunction(entitySimpleName: String): String = """
        override fun insert(data: ${entitySimpleName}): Long? {
            val metadata = MetadataRegistry.getMetadata(${entitySimpleName}::class)
                as EntityMetadata<${entitySimpleName}>
            return DbOperations.insert(connection, metadata, data)
        }
        """

    private fun buildUpdateFunction(entitySimpleName: String): String = """
        override fun update(data: ${entitySimpleName}): Int {
            val metadata = MetadataRegistry.getMetadata(${entitySimpleName}::class)
                as EntityMetadata<${entitySimpleName}>    
            return DbOperations.update(connection, metadata, data)
        }
        """

    private fun buildDeleteFunction(keyType: String, entitySimpleName: String): String = """            
        override fun deleteById(id: ${keyType}): Int {
            val metadata = MetadataRegistry.getMetadata(${entitySimpleName}::class)
                as EntityMetadata<${entitySimpleName}>
            return DbOperations.deleteById(connection, metadata, id)
        }
        """

    private fun buildFindByIdFn(keyType: String, entitySimpleName: String): String = """
        override fun findById(id: $keyType): ${entitySimpleName}? {
            val metadata = MetadataRegistry.getMetadata(${entitySimpleName}::class)
                as EntityMetadata<${entitySimpleName}>
            return DbOperations.findById(connection, metadata, ${entitySimpleName}::class, id)
        }
        """

    private fun buildFindAllFn(entitySimpleName: String): String = """
        override fun findAll(): List<${entitySimpleName}> {
            val metadata = MetadataRegistry.getMetadata(${entitySimpleName}::class)
                as EntityMetadata<${entitySimpleName}>
            return DbOperations.findAll(connection, metadata, ${entitySimpleName}::class)
        }
        """


    private fun buildExistsFn(keyType: String, entitySimpleName: String): String = """
        override fun exists(id: $keyType): Boolean {
            val metadata = MetadataRegistry.getMetadata(${entitySimpleName}::class)
                as EntityMetadata<${entitySimpleName}>
            return DbOperations.exists(connection, metadata, id)
        }
        """

    private fun fail(message: String) {
        environment.logger.error(message)
    }

    private data class CustomSignature (val signature: String, val targetType: String?, )

    private companion object {

        val SUPPORTED_PRIMITIVE_TYPES = setOf(
            String::class.qualifiedName,
            Boolean::class.qualifiedName,
            Long::class.qualifiedName,
            Double::class.qualifiedName,
            Int::class.qualifiedName,
            Float::class.qualifiedName
        )
    }
}