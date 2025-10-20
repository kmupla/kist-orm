package io.rss.knative.tools.kist

@Dao
interface KistDao<T, P> {

    fun insert(data: T): Long?
    fun update(data: T): Int
    fun deleteById(id: P): Int

    fun findAll(): List<T>
    fun findById(id: P): T?

}