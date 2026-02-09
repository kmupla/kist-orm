package com.company.local.example

import com.company.local.example.model.Person
import com.company.local.example.model.PersonMinDto
import io.github.kmupla.kist.Dao
import io.github.kmupla.kist.KistDao
import io.github.kmupla.kist.Query

@io.github.kmupla.kist.Dao
interface PersonDao: io.github.kmupla.kist.KistDao<Person, Int> {

    @io.github.kmupla.kist.Query("SELECT * FROM person_table where street like ?")
    fun findByStreet(prefix: String): List<Person>

    @io.github.kmupla.kist.Query("SELECT * FROM person_table WHERE name like ? AND street like ?")
    fun findByNameStreet(name: String, streetPart: String): List<Person>

    @io.github.kmupla.kist.Query("SELECT id, name FROM person_table")
    fun listMinimalReference(): List<PersonMinDto>
}