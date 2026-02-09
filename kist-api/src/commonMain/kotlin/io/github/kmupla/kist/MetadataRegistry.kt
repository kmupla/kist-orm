package io.github.kmupla.kist

import kotlin.reflect.KClass

object MetadataRegistry {

    private val entityToMetadataMap = mutableMapOf<KClass<*>, io.github.kmupla.kist.EntityMetadata<*>>()
    private val daoImplMap = mutableMapOf<KClass<*>, io.github.kmupla.kist.KistDao<*, *>>()
    private var initialized = false

    // For testing purposes
    internal fun reset() {
        entityToMetadataMap.clear()
        daoImplMap.clear()
        initialized = false
    }

    fun addEntityMetadata (entity: KClass<*>, meta: io.github.kmupla.kist.EntityMetadata<*>) {
        entityToMetadataMap[entity] = meta
    }

    fun getMetadata(entity: KClass<*>): io.github.kmupla.kist.EntityMetadata<*> {
        assertIsInitialized()
        return entityToMetadataMap[entity] ?: throw IllegalArgumentException(
            "No metadata for found for class [$entity]. Check the configuration and whether this is really an Entity")
    }

    fun addDaoImplementation(source: KClass<*>, generatedImpl: io.github.kmupla.kist.KistDao<*, *>) {
        daoImplMap[source] = generatedImpl
    }

    fun getDaoImplementation(source: KClass<*>): io.github.kmupla.kist.KistDao<*, *> {
        assertIsInitialized()
        return daoImplMap[source] ?: throw IllegalArgumentException(
            "No DAO implementation found for class [$source]. Check the configuration and whether this is a valid KistDao")
    }

    fun markInitialized() {
        initialized = true
    }

    fun assertIsInitialized() {
        if (!initialized) {
            throw IllegalStateException("Kist is not initialized. Please ensure Kist.init() is called before using any Kist features.")
        }
    }

}