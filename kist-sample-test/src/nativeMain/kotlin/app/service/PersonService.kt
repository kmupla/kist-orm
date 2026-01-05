package app.service

import com.company.local.example.PersonDao
import com.company.local.example.model.Person
import com.company.local.example.model.UserType
import io.knative.kist.injectDao
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
object PersonService {

    private val personDao by injectDao<PersonDao> {}

    fun maintainPerson() {
        println("Creating person data")
        repeat(10) {
            val newPerson = Person(it + 1, "Ricardo$it", 10000L * (it + 1),
                "Av. Brasil", true, 100, UserType.BASIC, complement = "No complement",
                creationDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()))
            personDao.insert(newPerson)
        }

        println("Creation done. Listing")
        val data = personDao.findAll()
        data.forEach { person ->
            println(person)
        }
    }
}