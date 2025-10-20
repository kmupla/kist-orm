package io.rss.knative.tools.kist

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Entity (val tableName: String)

@Target(AnnotationTarget.FIELD, AnnotationTarget.TYPE_PARAMETER)
@Retention(AnnotationRetention.SOURCE)
annotation class PrimaryKeyColumn(val name: String)

@Target(AnnotationTarget.FIELD, AnnotationTarget.TYPE_PARAMETER)
@Retention(AnnotationRetention.SOURCE)
annotation class Column(val name: String)

@Target(AnnotationTarget.FIELD, AnnotationTarget.TYPE_PARAMETER)
@Retention(AnnotationRetention.SOURCE)
annotation class Transient(val name: String, )



@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Dao()

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class Query(val value: String)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class ModifyingQuery(val value: String)

/*
@Retention(AnnotationRetention.SOURCE)
annotation class QueryLimit(val value: Int)*/