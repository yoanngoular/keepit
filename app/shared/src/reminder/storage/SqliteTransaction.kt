package com.ygoular.keepit.reminder.storage

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.async.executeSQL

internal suspend inline fun <T> SQLiteConnection.writeTransaction(block: () -> T): T {
    executeSQL("BEGIN IMMEDIATE")
    try {
        val result = block()
        executeSQL("COMMIT")
        return result
    } catch (failure: Throwable) {
        if (inTransaction()) executeSQL("ROLLBACK")
        throw failure
    }
}
