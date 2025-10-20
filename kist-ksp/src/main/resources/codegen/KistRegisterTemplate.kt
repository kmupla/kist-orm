package io.rss.knative.tools.kist.processed

import io.rss.knative.tools.kist.*

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