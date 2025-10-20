package com.company.local.example.model

import io.rss.knative.tools.kist.Column
import io.rss.knative.tools.kist.Entity
import io.rss.knative.tools.kist.PrimaryKeyColumn

@Entity(tableName = "person_table")
data class Person (
    @PrimaryKeyColumn("id")
    var id: Int = 0,

    @Column(name = "name")
    var name: String,

    @Column(name = "birthday_timestamp")
    var birthday: Long,

    @Column(name = "street")
    var street: String = "",

    @Column(name = "st_number")
    var number: Int = 0,

    @Column(name = "complement")
    var complement: String? = null,
) {
}