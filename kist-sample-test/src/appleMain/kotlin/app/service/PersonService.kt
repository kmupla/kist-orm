package app.service

import com.company.local.example.PersonDao
import com.company.local.example.model.Person
import io.rss.knative.tools.kist.injectDao

object PersonService {

    private val personDao by injectDao<PersonDao> {}

    fun maintainPerson() {
        println("Creating person data")
        repeat(10) {
            val newPerson = Person(it + 1, "Ricardo$it", 10000L * (it + 1),
                "Av. Brasil", 100, "No complement")
            personDao.insert(newPerson)
        }

        println("Creation done. Listing")
        val data = personDao.findAll()
        data.forEach { person ->
            println(person)
        }
    }
}