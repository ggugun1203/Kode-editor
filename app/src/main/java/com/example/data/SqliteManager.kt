package com.example.data

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class SqlQueryResult(
    val isSuccess: Boolean,
    val message: String,
    val columns: List<String> = emptyList(),
    val rows: List<List<String>> = emptyList(),
    val affectedRows: Int = 0
)

data class TableSchema(
    val tableName: String,
    val columnNames: List<String>
)

class SqliteManager {

    suspend fun executeQuery(dbFile: File, sql: String): SqlQueryResult = withContext(Dispatchers.IO) {
        val cleanSql = sql.trim()
        if (cleanSql.isEmpty()) {
            return@withContext SqlQueryResult(false, "Query is empty")
        }

        var db: SQLiteDatabase? = null
        try {
            dbFile.parentFile?.mkdirs()
            db = SQLiteDatabase.openOrCreateDatabase(dbFile, null)

            val isSelect = cleanSql.startsWith("SELECT", ignoreCase = true) ||
                    cleanSql.startsWith("PRAGMA", ignoreCase = true) ||
                    cleanSql.startsWith("EXPLAIN", ignoreCase = true)

            if (isSelect) {
                val cursor = db.rawQuery(cleanSql, null)
                cursor.use { c ->
                    val colNames = c.columnNames.toList()
                    val rows = mutableListOf<List<String>>()
                    while (c.moveToNext()) {
                        val row = mutableListOf<String>()
                        for (i in 0 until c.columnCount) {
                            row.add(c.getString(i) ?: "NULL")
                        }
                        rows.add(row)
                    }
                    return@withContext SqlQueryResult(
                        isSuccess = true,
                        message = "Returned ${rows.size} rows",
                        columns = colNames,
                        rows = rows
                    )
                }
            } else {
                db.execSQL(cleanSql)
                return@withContext SqlQueryResult(
                    isSuccess = true,
                    message = "Query executed successfully"
                )
            }
        } catch (e: Exception) {
            SqlQueryResult(isSuccess = false, message = "SQL Error: ${e.message}")
        } finally {
            try { db?.close() } catch (e: Exception) {}
        }
    }

    suspend fun listTables(dbFile: File): List<TableSchema> = withContext(Dispatchers.IO) {
        if (!dbFile.exists()) return@withContext emptyList()
        val result = mutableListOf<TableSchema>()
        var db: SQLiteDatabase? = null
        try {
            db = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
            val cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'", null)
            val tableNames = mutableListOf<String>()
            cursor.use { c ->
                while (c.moveToNext()) {
                    tableNames.add(c.getString(0))
                }
            }

            for (tbl in tableNames) {
                val colCursor = db.rawQuery("PRAGMA table_info($tbl)", null)
                val cols = mutableListOf<String>()
                colCursor.use { cc ->
                    while (cc.moveToNext()) {
                        val colName = cc.getString(1)
                        val colType = cc.getString(2)
                        cols.add("$colName ($colType)")
                    }
                }
                result.add(TableSchema(tbl, cols))
            }
        } catch (e: Exception) {
            // ignore
        } finally {
            try { db?.close() } catch (e: Exception) {}
        }
        result
    }
}
