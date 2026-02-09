package io.github.kmupla.kist.ksp

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSValueArgument
import com.google.devtools.ksp.symbol.KSVisitor
import io.github.kmupla.kist.Column
import io.github.kmupla.kist.Entity
import io.github.kmupla.kist.PrimaryKeyColumn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class EntityClassVisitorTest {

    lateinit var underTest: EntityClassVisitor
    lateinit var environment: SymbolProcessorEnvironment
    lateinit var logger: KSPLogger
    lateinit var resultMap: MutableMap<KSName, String>

    @BeforeEach
    fun setUp() {
        environment = mock()
        logger = mock()
        resultMap = mutableMapOf()
        whenever(environment.logger).thenReturn(logger)

        val templateContent = this::class.java.classLoader.getResourceAsStream("codegen/MetadataTemplate.kt")
            ?.bufferedReader()
            ?.readText()
            ?: throw IllegalStateException("Template file not found")
        underTest = EntityClassVisitor(environment, resultMap, templateContent)
    }

    @Test
    fun `visitClassDeclaration when class is invalid then logs error and returns`() {
        val classDeclaration: KSClassDeclaration = mock()
        val ksName: KSName = mock()
        whenever(ksName.asString()).thenReturn("InvalidClass")
        whenever(classDeclaration.simpleName).thenReturn(ksName)
        whenever(classDeclaration.accept(any<KSVisitor<Any?, Boolean>>(), anyOrNull())).thenReturn(false)

        underTest.visitClassDeclaration(classDeclaration, Unit)

        verify(logger).error("Class is invalid. Aborting processing")
        assertTrue(resultMap.isEmpty())
    }

    @Test
    fun `visitClassDeclaration when tableName is missing then throws exception`() {
        val classDeclaration: KSClassDeclaration = mock()
        val ksName: KSName = mock()
        whenever(ksName.asString()).thenReturn("TestEntity")
        whenever(classDeclaration.simpleName).thenReturn(ksName)
        val entityAnnotation: KSAnnotation = mock()
        val annotationType: KSType = mock()
        val declaration: KSDeclaration = mock()
        val qualifiedNameMock: KSName = mock()

        whenever(classDeclaration.annotations).thenReturn(sequenceOf(entityAnnotation))
        whenever(entityAnnotation.annotationType).thenReturn(mock())
        whenever(entityAnnotation.annotationType.resolve()).thenReturn(annotationType)
        whenever(annotationType.declaration).thenReturn(declaration)
        whenever(declaration.qualifiedName).thenReturn(qualifiedNameMock)
        whenever(qualifiedNameMock.asString()).thenReturn(_root_ide_package_.io.github.kmupla.kist.Entity::class.qualifiedName)
        whenever(entityAnnotation.arguments).thenReturn(emptyList())
        whenever(classDeclaration.accept(any<KSVisitor<Any?, Boolean>>(), anyOrNull())).thenReturn(true)

        val exception = assertThrows(IllegalArgumentException::class.java) {
            underTest.visitClassDeclaration(classDeclaration, Unit)
        }
        assertEquals("Table name is required in an Entity", exception.message)
    }

    @Test
    fun `visitClassDeclaration with valid entity`() {
        val classDeclaration = mockClassDeclaration("TestEntity", "my_table")

        val pkProperty = mockProperty("id", "id_pk", isPrimary = true, String::class.qualifiedName!!)
        val nameProperty = mockProperty("name", "name_col", isPrimary = false, String::class.qualifiedName!!)

        whenever(classDeclaration.getAllProperties()).thenReturn(sequenceOf(pkProperty, nameProperty))

        underTest.visitClassDeclaration(classDeclaration, Unit)

        assertEquals(1, resultMap.size)
        val generatedCode = resultMap.values.first()
        assertTrue(generatedCode.contains("object TestEntityOrmMetadata: EntityMetadata<TestEntity>"))
        assertTrue(generatedCode.contains("""override val tableName: String = "my_table""""))
        assertTrue(generatedCode.contains("""override val keyField = "id""""))
        assertTrue(generatedCode.contains("""fieldName = "id","""))
        assertTrue(generatedCode.contains("""columnName = "id_pk","""))
        assertTrue(generatedCode.contains("""fieldName = "name","""))
        assertTrue(generatedCode.contains("""columnName = "name_col","""))
    }

    @Test
    fun `visitClassDeclaration when no primary key then throws exception`() {
        val classDeclaration = mockClassDeclaration("TestEntity", "my_table")
        val nameProperty = mockProperty("name", "name_col", isPrimary = false, String::class.qualifiedName!!)
        whenever(classDeclaration.getAllProperties()).thenReturn(sequenceOf(nameProperty))

        val exception = assertThrows(IllegalArgumentException::class.java) {
            underTest.visitClassDeclaration(classDeclaration, Unit)
        }
        assertEquals("Entity must have exactly one PrimaryKeyColumn", exception.message)
    }

    @Test
    fun `visitClassDeclaration when multiple primary keys then throws exception`() {
        val classDeclaration = mockClassDeclaration("TestEntity", "my_table")
        val pkProperty1 = mockProperty("id1", "id1_pk", isPrimary = true, String::class.qualifiedName!!)
        val pkProperty2 = mockProperty("id2", "id2_pk", isPrimary = true, String::class.qualifiedName!!)
        whenever(classDeclaration.getAllProperties()).thenReturn(sequenceOf(pkProperty1, pkProperty2))

        val exception = assertThrows(IllegalArgumentException::class.java) {
            underTest.visitClassDeclaration(classDeclaration, Unit)
        }
        assertEquals("Entity must have exactly one PrimaryKeyColumn", exception.message)
    }

    private fun mockClassDeclaration(className: String, tableName: String): KSClassDeclaration {
        val classDeclaration: KSClassDeclaration = mock(extraInterfaces = arrayOf(KSDeclaration::class))
        val ksName: KSName = mock()
        val qualifiedKsName: KSName = mock()
        val containingFile: KSFile = mock()
        val packageNameString = "io.knative.kist.ksp.generated"
        val packageName: KSName = mock()

        whenever(ksName.asString()).thenReturn(className)
        whenever(qualifiedKsName.asString()).thenReturn("$packageNameString.$className")
        whenever(classDeclaration.simpleName).thenReturn(ksName)
        whenever(classDeclaration.qualifiedName).thenReturn(qualifiedKsName)
        whenever(classDeclaration.containingFile).thenReturn(containingFile)
        whenever(containingFile.packageName).thenReturn(packageName)
        whenever(packageName.asString()).thenReturn(packageNameString)
        whenever(classDeclaration.accept(any<KSVisitor<Any?, Boolean>>(), anyOrNull())).thenReturn(true)

        val entityAnnotation = mockAnnotation("Entity", mapOf("tableName" to tableName), _root_ide_package_.io.github.kmupla.kist.Entity::class.qualifiedName!!)
        whenever(classDeclaration.annotations).thenReturn(sequenceOf(entityAnnotation))

        return classDeclaration
    }

    private fun mockProperty(name: String, columnName: String, isPrimary: Boolean, typeQualifiedName: String): KSPropertyDeclaration {
        val property: KSPropertyDeclaration = mock()
        val ksName: KSName = mock()
        val type: KSTypeReference = mock()
        val resolvedType: KSType = mock()
        val declaration: KSClassDeclaration = mock()
        val qualifiedName: KSName = mock()

        whenever(ksName.asString()).thenReturn(name)
        whenever(property.simpleName).thenReturn(ksName)
        whenever(property.type).thenReturn(type)
        whenever(type.resolve()).thenReturn(resolvedType)
        whenever(resolvedType.declaration).thenReturn(declaration)
        whenever(declaration.qualifiedName).thenReturn(qualifiedName)
        whenever(qualifiedName.asString()).thenReturn(typeQualifiedName)

        val annotationName = if (isPrimary) "PrimaryKeyColumn" else "Column"
        val annType = if (isPrimary) _root_ide_package_.io.github.kmupla.kist.PrimaryKeyColumn::class else _root_ide_package_.io.github.kmupla.kist.Column::class
        val columnAnnotation = mockAnnotation(annotationName, mapOf("name" to columnName), annType.qualifiedName!!)
        whenever(property.annotations).thenReturn(sequenceOf(columnAnnotation))

        return property
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