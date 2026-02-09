package io.github.kmupla.kist

import co.touchlab.sqliter.Cursor
import co.touchlab.sqliter.Statement
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class MetadataRegistryTest {

    val mockMetadata = object : io.github.kmupla.kist.EntityMetadata<String> {
        override val tableName: String
            get() = "person"
        override val keyField: String
            get() = "pId"
        override val fieldMetadata: List<io.github.kmupla.kist.FieldMetadata>
            get() = listOf()

        override fun create(cursor: Cursor): String {
            return "created"
        }

        override fun bindFields(
            source: String,
            statement: Statement,
            fieldIndexMap: Map<String, Int>
        ) {
            println("bound")
        }

        override fun getId(source: String) = 1
    }

    @BeforeTest
    fun setUp() {
        _root_ide_package_.io.github.kmupla.kist.MetadataRegistry.reset()
    }

    @Test
    fun `addEntityMetadata and getMetadata should store and retrieve metadata`() {
        val entityClass = String::class

        val metadata = mockMetadata
        _root_ide_package_.io.github.kmupla.kist.MetadataRegistry.markInitialized()
        _root_ide_package_.io.github.kmupla.kist.MetadataRegistry.addEntityMetadata(entityClass, metadata)
        val retrievedMetadata = _root_ide_package_.io.github.kmupla.kist.MetadataRegistry.getMetadata(entityClass)
        assertSame(metadata, retrievedMetadata)
    }

    @Test
    fun `getMetadata should throw an exception if not initialized`() {
        val entityClass = String::class
        assertFailsWith<IllegalStateException> {
            _root_ide_package_.io.github.kmupla.kist.MetadataRegistry.getMetadata(entityClass)
        }
    }

    @Test
    fun `getMetadata should throw an exception if metadata is not found`() {
        val entityClass = String::class
        _root_ide_package_.io.github.kmupla.kist.MetadataRegistry.markInitialized()
        assertFailsWith<IllegalArgumentException> {
            _root_ide_package_.io.github.kmupla.kist.MetadataRegistry.getMetadata(entityClass)
        }
    }

    @Test
    fun `addDaoImplementation and getDaoImplementation should store and retrieve DAO implementations`() {
        val daoInterface = _root_ide_package_.io.github.kmupla.kist.KistDao::class
        val daoImpl = object : io.github.kmupla.kist.KistDao<String, Long> {
            override fun insert(data: String): Long? = null
            override fun update(data: String): Int = 0
            override fun deleteById(id: Long): Int = 0
            override fun findAll(): List<String> = emptyList()
            override fun findById(id: Long): String? = null
            override fun exists(id: Long): Boolean  = false
        }
        _root_ide_package_.io.github.kmupla.kist.MetadataRegistry.markInitialized()
        _root_ide_package_.io.github.kmupla.kist.MetadataRegistry.addDaoImplementation(daoInterface, daoImpl)
        val retrievedDao = _root_ide_package_.io.github.kmupla.kist.MetadataRegistry.getDaoImplementation(daoInterface)
        assertSame(daoImpl, retrievedDao)
    }

    @Test
    fun `getDaoImplementation should throw an exception if not initialized`() {
        val daoInterface = _root_ide_package_.io.github.kmupla.kist.KistDao::class
        assertFailsWith<IllegalStateException> {
            _root_ide_package_.io.github.kmupla.kist.MetadataRegistry.getDaoImplementation(daoInterface)
        }
    }

    @Test
    fun `getDaoImplementation should throw an exception if the DAO implementation is not found`() {
        val daoInterface = _root_ide_package_.io.github.kmupla.kist.KistDao::class
        _root_ide_package_.io.github.kmupla.kist.MetadataRegistry.markInitialized()
        assertFailsWith<IllegalArgumentException> {
            _root_ide_package_.io.github.kmupla.kist.MetadataRegistry.getDaoImplementation(daoInterface)
        }
    }

    @Test
    fun `markInitialized should mark the registry as initialized`() {
        _root_ide_package_.io.github.kmupla.kist.MetadataRegistry.markInitialized()
        // Should not throw an exception
        _root_ide_package_.io.github.kmupla.kist.MetadataRegistry.assertIsInitialized()
    }
}