package io.rss.knative.tools.kist.ksp

import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import io.rss.knative.tools.kist.KistDao
import io.rss.knative.tools.kist.Query

class DaoClassVisitor(private val environment: SymbolProcessorEnvironment,
                      private val resultMap: MutableMap<KSName, String>) : KSVisitorVoid() {

    private val templateContent: String
     init {
         val templatePath = "codegen/DaoImplTemplate.kt"
         templateContent = this::class.java.classLoader.getResourceAsStream(templatePath)?.bufferedReader()?.readText()
             ?: throw IllegalStateException("Template file $templatePath not found in resources")
     }

    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
        environment.logger.info("Processing DAO interface: ${classDeclaration.simpleName.asString()}")

        if (!classDeclaration.validate()) {
            environment.logger.error("Class is invalid. Aborting processing")
            return
        }

        val kistDaoDecl = classDeclaration.superTypes.find { it.resolve().declaration.qualifiedName?.asString() == KistDao::class.qualifiedName }
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

        val parameterNames = functionDeclaration.parameters
            .map { it.name?.getShortName() }
            .joinToString (",")

        val constructorParams = buildReturnTypeConstructor(functionDeclaration.returnType)

        return writeCustomQueryResult(signature, targetType, constructorParams, parameterNames, query)
    }

    private fun writeCustomQueryResult(
        signature: String, targetType: String?,
        constructorParams: String, parameterNames: String,
        query: String,
    ) = buildString {
        append("     ")
        append("override ")
        append(signature)

        append(
            """ {
                fun createFromGenericData(data: Array<Any?>) =
                     $targetType (
                        ${constructorParams.takeIf { it.isNotEmpty() } ?: ""}
                     )
    
                return DbOperations.listForGenericType(connection, ::createFromGenericData,
                    ${targetType}::class,
                    "$query", $parameterNames)
                }
            """)
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
        val constructorParams = primaryConstructor?.parameters?.mapIndexed(::getSingleTypeDeclarationFactory)?.joinToString(",\n                    ") ?: ""
        return constructorParams
    }

    private fun getSingleTypeDeclarationFactory(index: Int, param: KSValueParameter): String {
        val typeName = param.type.resolve().declaration.qualifiedName?.asString()
        val cast = when (typeName) {
            String::class.qualifiedName -> "data[$index] as String"
            Boolean::class.qualifiedName -> "data[$index] as Boolean"
            Long::class.qualifiedName -> "data[$index] as Long"
            Double::class.qualifiedName -> "data[$index] as Double"
            Int::class.qualifiedName -> "(data[$index] as Long).toInt()"
            Float::class.qualifiedName -> "(data[$index] as Double).toFloat()"
            else -> "data[$index] as ${typeName!!}"
        }

        return "    $cast"
    }

    private fun getFunctionSignature(functionDeclaration: KSFunctionDeclaration): CustomSignature {
        val modifiers = functionDeclaration.modifiers.joinToString(" ") { it.name.lowercase() }
        val functionName = functionDeclaration.simpleName.asString()

        val parameters = functionDeclaration.parameters.joinToString(", ") { parameter ->
            val parameterName = parameter.name?.asString()
            val parameterType = parameter.type.resolve().declaration.qualifiedName?.asString()
            "$parameterName: $parameterType"
        }

        val returnType = functionDeclaration.returnType?.resolve()
        val returnStrClass = returnType?.declaration?.qualifiedName?.asString()
        val targetEntity = returnType?.arguments?.firstOrNull()?.type?.resolve()?.declaration?.qualifiedName?.asString()
            ?:returnStrClass ?: ""

        val resultSignature = buildString {
            append("$modifiers fun $functionName ($parameters)")

            if (returnStrClass != null && returnStrClass != "kotlin.Unit") {
                val returnTypeWithParam = returnStrClass.takeIf { it == targetEntity }
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

    private fun fail(message: String) {
        environment.logger.error(message)
    }

    private data class CustomSignature (val signature: String, val targetType: String?, )
}