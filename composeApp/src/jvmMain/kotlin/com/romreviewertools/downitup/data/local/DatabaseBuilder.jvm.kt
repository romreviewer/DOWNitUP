package com.romreviewertools.downitup.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        val dbFile = File(System.getProperty("user.home"), ".downitup/downitup.db")
        dbFile.parentFile?.mkdirs()

        val driver = JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}")
        AppDatabase.Schema.create(driver)
        return driver
    }
}
