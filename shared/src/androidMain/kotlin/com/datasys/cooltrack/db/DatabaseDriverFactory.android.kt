package com.datasys.cooltrack.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.datasys.cooltrack.core.SecureStorageInitializer

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver =
        AndroidSqliteDriver(
            CooltrackDatabase.Schema,
            SecureStorageInitializer.appContext,
            "cooltrack.db",
        )
}
