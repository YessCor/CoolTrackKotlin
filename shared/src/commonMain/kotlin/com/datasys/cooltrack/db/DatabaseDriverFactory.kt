package com.datasys.cooltrack.db

import app.cash.sqldelight.db.SqlDriver

/** Cada plataforma crea el driver de SQLite con su propio motor nativo. */
expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}
