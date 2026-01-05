package io.knative.kist.processed

import io.knative.kist.*
import io.knative.kist.config.*

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