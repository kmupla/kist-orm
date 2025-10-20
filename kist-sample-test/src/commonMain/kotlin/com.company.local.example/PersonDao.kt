package com.company.local.example

import com.company.local.example.model.Person
import com.company.local.example.model.PersonMinDto
import io.rss.knative.tools.kist.Dao
import io.rss.knative.tools.kist.KistDao
import io.rss.knative.tools.kist.Query

@Dao
interface PersonDao: KistDao<Person, Int> {

    @Query("SELECT * FROM person_table where street like ?")
    fun findByStreet(prefix: String): List<Person>

    @Query("SELECT * FROM person_table WHERE name like ? AND street like ?")
    fun findByNameStreet(name: String, streetPart: String): List<Person>

    @Query("SELECT id, name FROM person_table")
    fun listMinimalReference(): List<PersonMinDto>
}