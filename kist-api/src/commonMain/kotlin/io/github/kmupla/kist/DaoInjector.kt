package io.github.kmupla.kist

inline fun <reified T : io.github.kmupla.kist.KistDao<*, *>> injectDao(noinline configure: () -> Unit = {}): Lazy<T> {
    return lazy {
        configure()
        _root_ide_package_.io.github.kmupla.kist.MetadataRegistry.getDaoImplementation(T::class) as T
    }
}