package com.company.local.example.model

import io.github.kmupla.kist.Column
import io.github.kmupla.kist.Entity
import io.github.kmupla.kist.PrimaryKeyColumn
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

@io.github.kmupla.kist.Entity(tableName = "person_table")
data class Person (
    @io.github.kmupla.kist.PrimaryKeyColumn("id")
    var id: Int = 0,

    @io.github.kmupla.kist.Column(name = "name")
    var name: String,

    @io.github.kmupla.kist.Column(name = "birthday_timestamp")
    var birthday: Long,

    @io.github.kmupla.kist.Column(name = "street")
    var street: String = "",

    @io.github.kmupla.kist.Column(name = "is_active")
    var active: Boolean = true,

    @io.github.kmupla.kist.Column(name = "st_number")
    var number: Int = 0,

    @io.github.kmupla.kist.Column(name = "utype")
    var userType: UserType,

    @io.github.kmupla.kist.Column(name = "dt_creation")
    var creationDate: LocalDateTime?,

    @io.github.kmupla.kist.Column(name = "phone_number")
    var phoneNumber: Int? = null,

    @io.github.kmupla.kist.Column(name = "complement")
    var complement: String? = null,
) {
}

enum class UserType {
    BASIC, MANAGER;
}