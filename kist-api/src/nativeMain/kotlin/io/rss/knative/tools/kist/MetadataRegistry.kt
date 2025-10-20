package io.rss.knative.tools.kist

import kotlin.reflect.KClass

object MetadataRegistry {

    private val entityToMetadataMap = mutableMapOf<KClass<*>, EntityMetadata<*>>()
    private val daoImplMap = mutableMapOf<KClass<*>, KistDao<*,*>>()
    private var initialized = false

    fun addEntityMetadata (entity: KClass<*>, meta: EntityMetadata<*>) {
        entityToMetadataMap[entity] = meta
    }

    fun getMetadata(entity: KClass<*>): EntityMetadata<*> {
        assertIsInitialized()
        return entityToMetadataMap[entity] ?: throw IllegalArgumentException(
            "No metadata for found for class [$entity]. Check the configuration and whether this is really an Entity")
    }

    fun addDaoImplementation(source: KClass<*>, generatedImpl: KistDao<*,*>) {
        daoImplMap[source] = generatedImpl
    }

    fun getDaoImplementation(source: KClass<*>): KistDao<*,*> {
        assertIsInitialized()
        return daoImplMap[source] ?: throw IllegalArgumentException(
            "No DAO implementation found for class [$source]. Check the configuration and whether this is a valid KistDao")
    }

    fun markInitialized() {
        initialized = true
    }

    fun assertIsInitialized() {
        // TODO:
    }

}