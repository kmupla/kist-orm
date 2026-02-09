package io.github.kmupla.kist.ksp

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.*
import io.github.kmupla.kist.KistDao
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
        assertTrue(generatedCode.contains("class TestDaoImpl(private val connection: DatabaseConnection): TestDao"))
        assertTrue(generatedCode.contains("import com.example.TestEntity"))
        assertTrue(generatedCode.contains("import com.example.TestDao"))
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
        assertTrue(generatedCode.contains("""SELECT * FROM test_entity WHERE name = :name"""))
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

        whenever(superTypeQualifiedName.asString()).thenReturn(_root_ide_package_.io.github.kmupla.kist.KistDao::class.qualifiedName)
        whenever(superTypeDecl.qualifiedName).thenReturn(superTypeQualifiedName)
        whenever(superType.declaration).thenReturn(superTypeDecl)
        val typeArgs = listOf(mockTypeArgument(entityType), mockTypeArgument(keyType))
        whenever(superType.arguments).thenReturn(typeArgs)
        whenever(superTypeRef.resolve()).thenReturn(superType)
        whenever(daoDeclaration.superTypes).thenReturn(sequenceOf(superTypeRef))

        return daoDeclaration
    }

    private fun mockFunctionDeclaration(name: String, query: String): KSFunctionDeclaration {
        val func: KSFunctionDeclaration = mock<KSFunctionDeclaration>()
        val ksName: KSName = mock<KSName>()
        whenever(ksName.getShortName()).thenReturn(name)
        whenever(ksName.asString()).thenReturn(name)
        whenever(func.simpleName).thenReturn(ksName)

        val queryAnnotation = mockAnnotation("Query", mapOf("value" to query), _root_ide_package_.io.github.kmupla.kist.Query::class.qualifiedName!!)
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