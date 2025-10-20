package app

import app.service.PersonService
import co.touchlab.sqliter.bindLong
import co.touchlab.sqliter.bindString
import co.touchlab.sqliter.interop.SQLiteExceptionErrorCode
import co.touchlab.sqliter.withStatement
import io.rss.knative.tools.kist.InMemoryConfig
import io.rss.knative.tools.kist.PersistenceContext
import io.rss.knative.tools.kist.processed.KistRegister.processAnnotations

fun main() {
    PersistenceContext.createConnection(InMemoryConfig (
        dbName = "test.db",

        createStatements = listOf(
            """
               CREATE TABLE person_table(
               id INTEGER PRIMARY KEY, 
               name TEXT,
               birthday_timestamp INTEGER,
               street TEXT,
               st_number INTEGER,
               complement TEXT
               ) 
            """,
        )
    ))

    PersistenceContext.processAnnotations()

    println("executing INSERT")
//    PersistenceContext.connection
//        .withStatement("INSERT INTO person_table (id,name,birthday_timestamp,street,st_number) VALUES (?,?,?,?,?)") {
//
//        try {
//            bindLong(1, 123L)
//            bindString(2, "Ricardo")
//            bindLong(3, 10000)
//            bindString(4, "Av. Brasil")
//            bindLong(5, 100)
////            bindString(6, null)
//            execute()
//        } catch (e: SQLiteExceptionErrorCode) {
//            println("SQLiteExceptionErrorCode: ${e.message}")
//            e.printStackTrace()
//            throw e
//        } catch (e: Exception) {
//            println("Exception cought: ${e.message}")
//            e.printStackTrace()
//            throw e
//        } finally {
////            finalizeStatement()
//        }
//    }


    try {
        PersonService.maintainPerson()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}