package io.github.kmupla.kist.processed

import io.github.kmupla.kist.*
import io.github.kmupla.kist.config.*

object KistRegister {

    fun PersistenceContext.processAnnotations() {
        processEntities()
        processDaos()
        MetadataRegistry.markInitialized()
    }

    private fun processEntities() {
        // <plugin-entities />
    }

    private fun processDaos() {
        // <plugin-daos />
    }
}