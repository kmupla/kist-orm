package com.company.local.example.model

import io.knative.kist.Column
import io.knative.kist.Entity
import io.knative.kist.PrimaryKeyColumn
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

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

    @Column(name = "is_active")
    var active: Boolean = true,

    @Column(name = "st_number")
    var number: Int = 0,

    @Column(name = "utype")
    var userType: UserType,

    @Column(name = "dt_creation")
    var creationDate: LocalDateTime?,

    @Column(name = "phone_number")
    var phoneNumber: Int? = null,

    @Column(name = "complement")
    var complement: String? = null,
) {
}

enum class UserType {
    BASIC, MANAGER;
}