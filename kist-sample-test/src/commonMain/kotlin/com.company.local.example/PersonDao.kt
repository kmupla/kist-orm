package com.company.local.example

import com.company.local.example.model.Person
import com.company.local.example.model.PersonMinDto
import io.github.kmupla.kist.Dao
import io.github.kmupla.kist.KistDao
import io.github.kmupla.kist.Query

@Dao
interface PersonDao: KistDao<Person, Int> {

    // Positional placeholders — backward-compatible
    @Query("SELECT * FROM person_table where street like ?")
    fun findByStreet(prefix: String): List<Person>

    @Query("SELECT * FROM person_table WHERE name like ? AND street like ?")
    fun findByNameStreet(name: String, streetPart: String): List<Person>

    @Query("SELECT id, name FROM person_table")
    fun listMinimalReference(): List<PersonMinDto>

    // Named placeholders — parameter order in the query no longer has to match the method signature
    @Query("SELECT * FROM person_table WHERE street like :streetPart AND name like :name")
    fun findByNameStreetNamed(name: String, streetPart: String): List<Person>
}