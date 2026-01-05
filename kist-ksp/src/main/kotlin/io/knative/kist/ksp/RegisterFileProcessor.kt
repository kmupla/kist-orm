package io.knative.kist.ksp

import com.google.devtools.ksp.symbol.KSName

object RegisterFileProcessor {

    private val templateContent: String

    init {
        val templatePath = "codegen/KistRegisterTemplate.kt"
        templateContent = this::class.java.classLoader.getResourceAsStream(templatePath)?.bufferedReader()?.readText()
            ?: throw IllegalStateException("Template file $templatePath not found in resources")
    }

    fun generateRegister(resultFiles: Map<KSName, String>): String {
        val entitiesBlock = mutableListOf<String>()
        val daosBlock = mutableListOf<String>()

        resultFiles.forEach { (source, resultFile) ->
            when {
                isEntityMetadata(resultFile) -> registerEntity(source).let(entitiesBlock::add)
                else -> registerDao(source).let(daosBlock::add)
            }
        }

        return templateContent
            .replace("// <plugin-entities />", entitiesBlock.joinToString(System.lineSeparator()))
            .replace("// <plugin-daos />", daosBlock.joinToString(System.lineSeparator()))
    }

    // TODO: have a decent (structured) way of checking
    private fun isEntityMetadata(resultFile: String): Boolean = ": EntityMetadata" in resultFile

    private fun registerEntity(entityName: KSName): String {
        return """
           MetadataRegistry.addEntityMetadata(${entityName.asString()}::class, io.knative.kist.entities.${entityName.getShortName()}OrmMetadata) 
        """
    }

    private fun registerDao(source: KSName): String {
        return """
           MetadataRegistry.addDaoImplementation(${source.asString()}::class, 
               io.knative.kist.daos.${source.getShortName()}Impl(PersistenceContext.connection)) 
        """
    }
}