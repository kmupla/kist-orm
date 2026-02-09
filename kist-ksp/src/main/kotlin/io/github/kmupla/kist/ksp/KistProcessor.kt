package io.github.kmupla.kist.ksp

import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.validate
import io.github.kmupla.kist.Dao
import io.github.kmupla.kist.Entity

private const val BASE_PKG = "io.github.kmupla.kist"

class KistProcessor(private val environment: SymbolProcessorEnvironment) : SymbolProcessor {

    private var alreadyInvoked = false

    private val resultFiles = mutableMapOf<KSName, String>()
    private var registerResult: String? = null

    private val entityClassVisitor = EntityClassVisitor(environment, resultFiles)
    private val daoClassVisitor = DaoClassVisitor(environment, resultFiles)

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (alreadyInvoked) {
            return emptyList()
        }

        val processedAnnotations = resolveEntities(resolver)
            .plus(resolveDaos(resolver))

        generateRegister()

        alreadyInvoked = true
        return processedAnnotations
    }

    private fun generateRegister() {
        registerResult = RegisterFileProcessor.generateRegister(resultFiles)
    }

    private fun resolveEntities(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation(_root_ide_package_.io.github.kmupla.kist.Entity::class.qualifiedName!!)
        val ret = symbols.toList()
        symbols
            .filter { it is KSClassDeclaration }
            .filter { it.validate() }
            .forEach { it.accept(entityClassVisitor, Unit) }
        return ret
    }

    private fun resolveDaos(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation(_root_ide_package_.io.github.kmupla.kist.Dao::class.qualifiedName!!)
        val ret = symbols.toList()
        symbols
            .filter { it is KSClassDeclaration }
            .filter { it.validate() }
            .forEach { it.accept(daoClassVisitor, Unit) }
        return ret
    }

    override fun finish() {
        if (resultFiles.isEmpty()) {
            return
        }

        val codeGenerator = environment.codeGenerator

        resultFiles.forEach { (ksName, source) ->
            println("Creating for file: ${ksName.getShortName()}")
            val file = when {
                "Dao" in ksName.getShortName() -> {
                    val fileName = "${ksName.getShortName()}Impl"
                    codeGenerator.createNewFile(Dependencies(false), "$BASE_PKG.daos", fileName)
                }
                else -> {
                    val fileName = "${ksName.getShortName()}OrmMetadata"
                    codeGenerator.createNewFile(Dependencies(false), "$BASE_PKG.entities", fileName)
                }
            }
            file.use { it.write(source.toByteArray()) }
        }

        registerResult?.let { content ->
            val fileName = "KistRegister"
            val file = codeGenerator.createNewFile(Dependencies(false), "$BASE_PKG.processed", fileName)
            file.use { it.write(content.toByteArray()) }
        }
    }
}