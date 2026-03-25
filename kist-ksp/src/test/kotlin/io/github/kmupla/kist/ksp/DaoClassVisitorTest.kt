package io.github.kmupla.kist.ksp

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.*
import io.github.kmupla.kist.KistDao
import io.github.kmupla.kist.ModifyingQuery
import io.github.kmupla.kist.Query
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*

class DaoClassVisitorTest {

    private lateinit var underTest: DaoClassVisitor
    private lateinit var environment: SymbolProcessorEnvironment
    private lateinit var logger: KSPLogger
    private lateinit var resultMap: MutableMap<KSName, String>

    @BeforeEach
    fun setUp() {
        environment = mock()
        logger = mock()
        resultMap = mutableMapOf()
        whenever(environment.logger).thenReturn(logger)

        val templateContent = this::class.java.classLoader.getResourceAsStream("codegen/DaoImplTemplate.kt")
            ?.bufferedReader()
            ?.readText()
            ?: throw IllegalStateException("Template file not found")
        underTest = DaoClassVisitor(environment, resultMap, templateContent)
    }

    @Test
    fun `visitClassDeclaration when class is invalid then logs error and returns`() {
        val classDeclaration: KSClassDeclaration = mock()
        val ksName: KSName = mock()
        whenever(ksName.asString()).thenReturn("InvalidDao")
        whenever(classDeclaration.simpleName).thenReturn(ksName)
        whenever(classDeclaration.accept(any<KSVisitor<Any?, Boolean>>(), anyOrNull())).thenReturn(false)

        underTest.visitClassDeclaration(classDeclaration, Unit)

        verify(logger).error("Class is invalid. Aborting processing")
        assertTrue(resultMap.isEmpty())
    }

    @Test
    fun `visitClassDeclaration when not a KistDao then fails`() {
        val daoDeclaration: KSClassDeclaration = mock()
        val ksName: KSName = mock()
        whenever(ksName.asString()).thenReturn("NotAKistDao")
        whenever(daoDeclaration.simpleName).thenReturn(ksName)
        whenever(daoDeclaration.qualifiedName).thenReturn(ksName)
        whenever(daoDeclaration.accept(any<KSVisitor<Any?, Boolean>>(), anyOrNull())).thenReturn(true)
        whenever(daoDeclaration.superTypes).thenReturn(emptySequence())
        whenever(environment.logger).thenReturn(logger)
        whenever(logger.error(any(), anyOrNull())).thenAnswer {
            throw IllegalStateException("Class ${daoDeclaration.qualifiedName} must implement KistDao")
        }

        val exception = assertThrows(IllegalStateException::class.java) {
            underTest.visitClassDeclaration(daoDeclaration, Unit)
        }
        assertTrue(exception.message!!.contains("must implement KistDao"))
    }

    @Test
    fun `visitClassDeclaration with valid dao`() {
        val entityType = mockType("com.example.TestEntity", "TestEntity")
        val keyType = mockType("kotlin.String", "String")
        val daoDeclaration = mockDaoClassDeclaration("TestDao", "com.example.TestDao", entityType, keyType)
        whenever(daoDeclaration.declarations).thenReturn(emptySequence())

        underTest.visitClassDeclaration(daoDeclaration, Unit)

        assertEquals(1, resultMap.size)
        val generatedCode = resultMap.values.first()
        println("Generated code:\n$generatedCode") // Debug output
        assertTrue(generatedCode.contains("TestDaoImpl"), "Should contain TestDaoImpl class")
        assertTrue(generatedCode.contains("TestDao"), "Should contain TestDao interface reference")
        assertTrue(generatedCode.contains("DatabaseConnection") || generatedCode.contains("connection"), "Should contain database connection")
    }

    @Test
    fun `visitClassDeclaration with positional query emits vararg call site`() {
        val entityType = mockType("com.example.TestEntity", "TestEntity")
        val keyType = mockType("kotlin.String", "String")
        val daoDeclaration = mockDaoClassDeclaration("TestDao", "com.example.TestDao", entityType, keyType)

        val function = mockFunctionDeclarationWithParams(
            name = "findByStreet",
            query = "SELECT * FROM test_entity WHERE street LIKE ?",
            params = listOf("prefix" to "kotlin.String"),
        )
        val declarations: Sequence<KSDeclaration> = sequenceOf(function as KSDeclaration)
        whenever(daoDeclaration.declarations).thenReturn(declarations)

        underTest.visitClassDeclaration(daoDeclaration, Unit)

        assertEquals(1, resultMap.size)
        val generatedCode = resultMap.values.first()
        assertTrue(generatedCode.contains("fun findByStreet"), "Should contain function name")
        // positional mode: params passed as plain identifiers, not as mapOf(...)
        assertTrue(generatedCode.contains("prefix"), "Should reference the param name directly")
        assertFalse(generatedCode.contains("mapOf("), "Should NOT use mapOf in positional mode")
    }

    @Test
    fun `visitClassDeclaration with named query emits mapOf call site`() {
        val entityType = mockType("com.example.TestEntity", "TestEntity")
        val keyType = mockType("kotlin.String", "String")
        val daoDeclaration = mockDaoClassDeclaration("TestDao", "com.example.TestDao", entityType, keyType)

        val function = mockFunctionDeclarationWithParams(
            name = "findByName",
            query = "SELECT * FROM test_entity WHERE name = :name",
            params = listOf("name" to "kotlin.String"),
        )
        val declarations: Sequence<KSDeclaration> = sequenceOf(function as KSDeclaration)
        whenever(daoDeclaration.declarations).thenReturn(declarations)

        underTest.visitClassDeclaration(daoDeclaration, Unit)

        assertEquals(1, resultMap.size)
        val generatedCode = resultMap.values.first()
        assertTrue(generatedCode.contains("fun findByName"), "Should contain function name")
        assertTrue(generatedCode.contains("""mapOf("name" to name)"""), "Should emit mapOf with named param")
    }

    @Test
    fun `visitClassDeclaration with named query and multiple params emits full mapOf`() {
        val entityType = mockType("com.example.TestEntity", "TestEntity")
        val keyType = mockType("kotlin.String", "String")
        val daoDeclaration = mockDaoClassDeclaration("TestDao", "com.example.TestDao", entityType, keyType)

        val function = mockFunctionDeclarationWithParams(
            name = "findByNameAndStreet",
            query = "SELECT * FROM test_entity WHERE name = :name AND street = :street",
            params = listOf("name" to "kotlin.String", "street" to "kotlin.String"),
        )
        val declarations: Sequence<KSDeclaration> = sequenceOf(function as KSDeclaration)
        whenever(daoDeclaration.declarations).thenReturn(declarations)

        underTest.visitClassDeclaration(daoDeclaration, Unit)

        assertEquals(1, resultMap.size)
        val generatedCode = resultMap.values.first()
        assertTrue(generatedCode.contains("""mapOf("name" to name, "street" to street)"""), "Should emit mapOf with both params")
    }

    @Test
    fun `visitClassDeclaration with mixed placeholder query logs error and omits method`() {
        val entityType = mockType("com.example.TestEntity", "TestEntity")
        val keyType = mockType("kotlin.String", "String")
        val daoDeclaration = mockDaoClassDeclaration("TestDao", "com.example.TestDao", entityType, keyType)

        val function = mockFunctionDeclarationWithParams(
            name = "badQuery",
            query = "SELECT * FROM t WHERE a = ? AND b = :b",
            params = listOf("a" to "kotlin.String", "b" to "kotlin.String"),
        )
        val declarations: Sequence<KSDeclaration> = sequenceOf(function as KSDeclaration)
        whenever(daoDeclaration.declarations).thenReturn(declarations)

        underTest.visitClassDeclaration(daoDeclaration, Unit)

        verify(logger).error(
            "Query in 'badQuery' mixes positional '?' and named ':param' placeholders, which is not allowed."
        )
        val generatedCode = resultMap.values.first()
        assertFalse(generatedCode.contains("fun badQuery"), "Mixed-placeholder method should not be generated")
    }

    @Test
    fun `visitClassDeclaration with unknown named param logs error`() {
        val entityType = mockType("com.example.TestEntity", "TestEntity")
        val keyType = mockType("kotlin.String", "String")
        val daoDeclaration = mockDaoClassDeclaration("TestDao", "com.example.TestDao", entityType, keyType)

        val function = mockFunctionDeclarationWithParams(
            name = "findByName",
            query = "SELECT * FROM t WHERE name = :typo",
            params = listOf("name" to "kotlin.String"),
        )
        val declarations: Sequence<KSDeclaration> = sequenceOf(function as KSDeclaration)
        whenever(daoDeclaration.declarations).thenReturn(declarations)

        underTest.visitClassDeclaration(daoDeclaration, Unit)

        verify(logger).error(
            "Named parameter ':typo' in query of 'findByName' does not match any method parameter. Available: [name]"
        )
    }

    @Test
    fun `visitClassDeclaration with custom query`() {
        val entityType = mockType("com.example.TestEntity", "TestEntity")
        val keyType = mockType("kotlin.String", "String")
        val daoDeclaration = mockDaoClassDeclaration("TestDao", "com.example.TestDao", entityType, keyType)

        val function: KSFunctionDeclaration = mockFunctionDeclaration("findByName", "SELECT * FROM test_entity WHERE name = :name")
        val declarations: Sequence<KSDeclaration> = sequenceOf(function as KSDeclaration)
        whenever(daoDeclaration.declarations).thenReturn(declarations)

        underTest.visitClassDeclaration(daoDeclaration, Unit)

        assertEquals(1, resultMap.size)
        val generatedCode = resultMap.values.first()
        assertTrue(generatedCode.contains("fun findByName ()"))
        // named mode: original SQL is still embedded verbatim in the generated source
        assertTrue(generatedCode.contains("""SELECT * FROM test_entity WHERE name = :name"""))
    }

    @Test
    fun `visitClassDeclaration with modifying query unit return and named params emits executeModifyingQuery without return value`() {
        val entityType = mockType("com.example.TestEntity", "TestEntity")
        val keyType = mockType("kotlin.String", "String")
        val daoDeclaration = mockDaoClassDeclaration("TestDao", "com.example.TestDao", entityType, keyType)

        val function = mockModifyingFunctionWithParams(
            name = "deleteByName",
            query = "DELETE FROM test_entity WHERE name = :name",
            params = listOf("name" to "kotlin.String"),
            returnQualifiedName = "kotlin.Unit",
            returnSimpleName = "Unit",
        )
        val declarations: Sequence<KSDeclaration> = sequenceOf(function as KSDeclaration)
        whenever(daoDeclaration.declarations).thenReturn(declarations)

        underTest.visitClassDeclaration(daoDeclaration, Unit)

        assertEquals(1, resultMap.size)
        val generatedCode = resultMap.values.first()
        assertTrue(generatedCode.contains("fun deleteByName"), "Should contain function name")
        assertTrue(generatedCode.contains("executeModifyingQuery"), "Should call executeModifyingQuery")
        assertTrue(generatedCode.contains("""mapOf("name" to name)"""), "Should use named params mapOf")
        assertFalse(generatedCode.contains("return result"), "Unit return should not return a result value")
        assertTrue(generatedCode.contains("return"), "Unit return should still emit a bare return")
    }

    @Test
    fun `visitClassDeclaration with modifying query long return and positional params emits executeModifyingQuery with return result`() {
        val entityType = mockType("com.example.TestEntity", "TestEntity")
        val keyType = mockType("kotlin.String", "String")
        val daoDeclaration = mockDaoClassDeclaration("TestDao", "com.example.TestDao", entityType, keyType)

        val function = mockModifyingFunctionWithParams(
            name = "archiveOlderThan",
            query = "UPDATE test_entity SET archived = 1 WHERE age > ?",
            params = listOf("age" to "kotlin.Long"),
            returnQualifiedName = "kotlin.Long",
            returnSimpleName = "Long",
        )
        val declarations: Sequence<KSDeclaration> = sequenceOf(function as KSDeclaration)
        whenever(daoDeclaration.declarations).thenReturn(declarations)

        underTest.visitClassDeclaration(daoDeclaration, Unit)

        assertEquals(1, resultMap.size)
        val generatedCode = resultMap.values.first()
        assertTrue(generatedCode.contains("fun archiveOlderThan"), "Should contain function name")
        assertTrue(generatedCode.contains("executeModifyingQuery"), "Should call executeModifyingQuery")
        assertFalse(generatedCode.contains("mapOf("), "Positional mode should not use mapOf")
        assertTrue(generatedCode.contains("return result"), "Long return should return result")
    }

    @Test
    fun `visitClassDeclaration with modifying query invalid return type logs error and omits method`() {
        val entityType = mockType("com.example.TestEntity", "TestEntity")
        val keyType = mockType("kotlin.String", "String")
        val daoDeclaration = mockDaoClassDeclaration("TestDao", "com.example.TestDao", entityType, keyType)

        val function = mockModifyingFunctionWithParams(
            name = "badModifyingReturn",
            query = "DELETE FROM test_entity WHERE name = :name",
            params = listOf("name" to "kotlin.String"),
            returnQualifiedName = "kotlin.String",
            returnSimpleName = "String",
        )
        val declarations: Sequence<KSDeclaration> = sequenceOf(function as KSDeclaration)
        whenever(daoDeclaration.declarations).thenReturn(declarations)

        underTest.visitClassDeclaration(daoDeclaration, Unit)

        verify(logger).error(
            "ModifyingQuery 'badModifyingReturn' must return Unit or Long, but found 'kotlin.String'."
        )
        val generatedCode = resultMap.values.first()
        assertFalse(generatedCode.contains("fun badModifyingReturn"), "Invalid return type method should not be generated")
    }

    @Test
    fun `visitClassDeclaration with modifying query mixed placeholders logs error and omits method`() {
        val entityType = mockType("com.example.TestEntity", "TestEntity")
        val keyType = mockType("kotlin.String", "String")
        val daoDeclaration = mockDaoClassDeclaration("TestDao", "com.example.TestDao", entityType, keyType)

        val function = mockModifyingFunctionWithParams(
            name = "badMixedModifying",
            query = "DELETE FROM test_entity WHERE a = ? AND b = :b",
            params = listOf("a" to "kotlin.String", "b" to "kotlin.String"),
            returnQualifiedName = "kotlin.Unit",
            returnSimpleName = "Unit",
        )
        val declarations: Sequence<KSDeclaration> = sequenceOf(function as KSDeclaration)
        whenever(daoDeclaration.declarations).thenReturn(declarations)

        underTest.visitClassDeclaration(daoDeclaration, Unit)

        verify(logger).error(
            "Query in 'badMixedModifying' mixes positional '?' and named ':param' placeholders, which is not allowed."
        )
        val generatedCode = resultMap.values.first()
        assertFalse(generatedCode.contains("fun badMixedModifying"), "Mixed-placeholder modifying method should not be generated")
    }

    private fun mockDaoClassDeclaration(daoName: String, qualifiedDaoName: String, entityType: KSType, keyType: KSType): KSClassDeclaration {
        val daoDeclaration: KSClassDeclaration = mock()
        val ksName: KSName = mock()
        val qualifiedKsName: KSName = mock()
        whenever(ksName.asString()).thenReturn(daoName)
        whenever(qualifiedKsName.asString()).thenReturn(qualifiedDaoName)
        whenever(daoDeclaration.simpleName).thenReturn(ksName)
        whenever(daoDeclaration.qualifiedName).thenReturn(qualifiedKsName)
        whenever(daoDeclaration.accept(any<KSVisitor<Any?, Boolean>>(), anyOrNull())).thenReturn(true)

        val superTypeRef: KSTypeReference = mock()
        val superType: KSType = mock()
        val superTypeDecl: KSClassDeclaration = mock()
        val superTypeQualifiedName: KSName = mock()

        whenever(superTypeQualifiedName.asString()).thenReturn(KistDao::class.qualifiedName)
        whenever(superTypeDecl.qualifiedName).thenReturn(superTypeQualifiedName)
        whenever(superType.declaration).thenReturn(superTypeDecl)
        val typeArgs = listOf(mockTypeArgument(entityType), mockTypeArgument(keyType))
        whenever(superType.arguments).thenReturn(typeArgs)
        whenever(superTypeRef.resolve()).thenReturn(superType)
        whenever(daoDeclaration.superTypes).thenReturn(sequenceOf(superTypeRef))

        return daoDeclaration
    }

    /**
     * Creates a mock [KSFunctionDeclaration] with an explicit parameter list.
     * Each entry in [params] is `paramName to qualifiedTypeName`.
     */
    private fun mockFunctionDeclarationWithParams(
        name: String,
        query: String,
        params: List<Pair<String, String>>,
    ): KSFunctionDeclaration {
        val func: KSFunctionDeclaration = mock()
        val ksName: KSName = mock()
        whenever(ksName.getShortName()).thenReturn(name)
        whenever(ksName.asString()).thenReturn(name)
        whenever(func.simpleName).thenReturn(ksName)

        val queryAnnotation = mockAnnotation("Query", mapOf("value" to query), Query::class.qualifiedName!!)
        whenever(func.annotations).thenReturn(sequenceOf(queryAnnotation))

        val returnTypeRef: KSTypeReference = mock()
        val returnType = mockType("com.example.TestEntity", "TestEntity")
        whenever(returnTypeRef.resolve()).thenReturn(returnType)
        whenever(func.returnType).thenReturn(returnTypeRef)
        whenever(func.modifiers).thenReturn(emptySet())

        val ksParams = params.map { (paramName, qualifiedType) ->
            val param: KSValueParameter = mock()
            val paramKsName: KSName = mock()
            whenever(paramKsName.getShortName()).thenReturn(paramName)
            whenever(paramKsName.asString()).thenReturn(paramName)
            whenever(param.name).thenReturn(paramKsName)

            val paramType = mockType(qualifiedType, qualifiedType.substringAfterLast('.'))
            whenever(paramType.arguments).thenReturn(emptyList())
            whenever(paramType.isMarkedNullable).thenReturn(false)
            val paramTypeRef: KSTypeReference = mock()
            whenever(paramTypeRef.resolve()).thenReturn(paramType)
            whenever(param.type).thenReturn(paramTypeRef)
            param
        }
        whenever(func.parameters).thenReturn(ksParams)
        whenever(func.toString()).thenReturn("fun $name(...): TestEntity")

        return func
    }

    private fun mockFunctionDeclaration(name: String, query: String): KSFunctionDeclaration {
        val func: KSFunctionDeclaration = mock<KSFunctionDeclaration>()
        val ksName: KSName = mock<KSName>()
        whenever(ksName.getShortName()).thenReturn(name)
        whenever(ksName.asString()).thenReturn(name)
        whenever(func.simpleName).thenReturn(ksName)

        val queryAnnotation = mockAnnotation("Query", mapOf("value" to query), Query::class.qualifiedName!!)
        whenever(func.annotations).thenReturn(sequenceOf(queryAnnotation))

        val returnTypeRef: KSTypeReference = mock()
        val returnType = mockType("com.example.TestEntity", "TestEntity")
        whenever(returnTypeRef.resolve()).thenReturn(returnType)
        whenever(func.returnType).thenReturn(returnTypeRef)
        whenever(func.parameters).thenReturn(emptyList())
        whenever(func.modifiers).thenReturn(emptySet())
        whenever(func.toString()).thenReturn("fun $name(): TestEntity")


        return func
    }

    /**
     * Creates a mock [KSFunctionDeclaration] annotated with [ModifyingQuery].
     * [returnQualifiedName] / [returnSimpleName] control the declared return type.
     */
    private fun mockModifyingFunctionWithParams(
        name: String,
        query: String,
        params: List<Pair<String, String>>,
        returnQualifiedName: String,
        returnSimpleName: String,
    ): KSFunctionDeclaration {
        val func: KSFunctionDeclaration = mock()
        val ksName: KSName = mock()
        whenever(ksName.getShortName()).thenReturn(name)
        whenever(ksName.asString()).thenReturn(name)
        whenever(func.simpleName).thenReturn(ksName)

        val modifyingAnnotation = mockAnnotation("ModifyingQuery", mapOf("value" to query), ModifyingQuery::class.qualifiedName!!)
        whenever(func.annotations).thenReturn(sequenceOf(modifyingAnnotation))

        val returnTypeRef: KSTypeReference = mock()
        val returnType = mockType(returnQualifiedName, returnSimpleName)
        whenever(returnTypeRef.resolve()).thenReturn(returnType)
        whenever(func.returnType).thenReturn(returnTypeRef)
        whenever(func.modifiers).thenReturn(emptySet())

        val ksParams = params.map { (paramName, qualifiedType) ->
            val param: KSValueParameter = mock()
            val paramKsName: KSName = mock()
            whenever(paramKsName.getShortName()).thenReturn(paramName)
            whenever(paramKsName.asString()).thenReturn(paramName)
            whenever(param.name).thenReturn(paramKsName)

            val paramType = mockType(qualifiedType, qualifiedType.substringAfterLast('.'))
            whenever(paramType.arguments).thenReturn(emptyList())
            whenever(paramType.isMarkedNullable).thenReturn(false)
            val paramTypeRef: KSTypeReference = mock()
            whenever(paramTypeRef.resolve()).thenReturn(paramType)
            whenever(param.type).thenReturn(paramTypeRef)
            param
        }
        whenever(func.parameters).thenReturn(ksParams)
        whenever(func.toString()).thenReturn("fun $name(...): $returnSimpleName")

        return func
    }

    private fun mockType(qualifiedNameStr: String, simpleNameStr: String): KSType {
        val type: KSType = mock()
        val typeDeclaration: KSClassDeclaration = mock()
        val qualifiedName: KSName = mock()
        val simpleName: KSName = mock()

        whenever(qualifiedName.asString()).thenReturn(qualifiedNameStr)
        whenever(simpleName.asString()).thenReturn(simpleNameStr)
        whenever(typeDeclaration.qualifiedName).thenReturn(qualifiedName)
        whenever(typeDeclaration.simpleName).thenReturn(simpleName)
        whenever(type.declaration).thenReturn(typeDeclaration)
        whenever(type.toString()).thenReturn(simpleNameStr)

        return type
    }

    private fun mockTypeArgument(type: KSType): KSTypeArgument {
        val typeArg: KSTypeArgument = mock()
        val typeRef: KSTypeReference = mock()
        whenever(typeRef.resolve()).thenReturn(type)
        whenever(typeArg.type).thenReturn(typeRef)
        return typeArg
    }

    private fun mockAnnotation(name: String, arguments: Map<String, Any>, qualifiedNameStr: String): KSAnnotation {
        val annotation: KSAnnotation = mock()
        val ksName: KSName = mock()
        val type: KSType = mock()
        val declaration: KSDeclaration = mock()
        val qualifiedName: KSName = mock()

        whenever(ksName.asString()).thenReturn(name)
        whenever(annotation.shortName).thenReturn(ksName)
        whenever(annotation.annotationType).thenReturn(mock())
        whenever(annotation.annotationType.resolve()).thenReturn(type)
        whenever(type.declaration).thenReturn(declaration)
        whenever(declaration.simpleName).thenReturn(ksName)
        whenever(declaration.qualifiedName).thenReturn(qualifiedName)
        whenever(qualifiedName.asString()).thenReturn(qualifiedNameStr)

        val valueArguments = arguments.map { (key, value) ->
            val ksValueArgument: KSValueArgument = mock()
            val argName: KSName = mock()
            whenever(argName.asString()).thenReturn(key)
            whenever(ksValueArgument.name).thenReturn(argName)
            whenever(ksValueArgument.value).thenReturn(value)
            ksValueArgument
        }
        whenever(annotation.arguments).thenReturn(valueArguments)

        return annotation
    }
}