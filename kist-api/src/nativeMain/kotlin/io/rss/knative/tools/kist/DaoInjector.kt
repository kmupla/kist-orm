package io.rss.knative.tools.kist

inline fun <reified T : KistDao<*, *>> injectDao(noinline configure: () -> Unit = {}): Lazy<T> {
    return lazy {
        configure()
        MetadataRegistry.getDaoImplementation(T::class) as T
    }
}