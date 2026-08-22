package com.dirac.mactrack.data.cnf

import android.content.Context
import android.database.sqlite.SQLiteDatabase

class CnfRepository(private val context: Context) {

    private val dbName = "cnf.db"
    @Volatile private var db: SQLiteDatabase? = null

    private fun open(): SQLiteDatabase {
        db?.let { return it }
        synchronized(this) {
            db?.let { return it }
            val outFile = context.getDatabasePath(dbName)
            if (!outFile.exists()) {
                outFile.parentFile?.mkdirs()
                context.assets.open(dbName).use { input ->
                    outFile.outputStream().use { output -> input.copyTo(output) }
                }
            }
            val opened = SQLiteDatabase.openDatabase(outFile.path, null, SQLiteDatabase.OPEN_READONLY)
            db = opened
            return opened
        }
    }

    private val cols = "code,name,kcal,protein,carb,fat,fiber,sugar,satfat,sodium,potassium,cholesterol"

    private fun readFood(c: android.database.Cursor) = CnfFood(
        code = c.getInt(0), name = c.getString(1),
        kcal = c.getDouble(2), protein = c.getDouble(3), carb = c.getDouble(4), fat = c.getDouble(5),
        fiber = c.getDouble(6), sugar = c.getDouble(7), satFat = c.getDouble(8),
        sodium = c.getDouble(9), potassium = c.getDouble(10), cholesterol = c.getDouble(11)
    )

    fun search(query: String, limit: Int = 50): List<CnfFood> {
        val q = query.trim()
        if (q.length < 2) return emptyList()
        val terms = q.split(Regex("\\s+"))
        val where = terms.joinToString(" AND ") { "name LIKE ?" }
        val args = terms.map { "%$it%" }.toTypedArray()
        val sql = "SELECT $cols FROM cnf_food WHERE $where ORDER BY length(name) LIMIT $limit"
        val out = mutableListOf<CnfFood>()
        open().rawQuery(sql, args).use { c -> while (c.moveToNext()) out.add(readFood(c)) }
        return out
    }

    fun getFood(code: Int): CnfFood? {
        open().rawQuery("SELECT $cols FROM cnf_food WHERE code = ? LIMIT 1", arrayOf(code.toString())).use { c ->
            if (c.moveToNext()) return readFood(c)
        }
        return null
    }

    fun measures(code: Int): List<CnfMeasure> {
        val out = mutableListOf<CnfMeasure>()
        open().rawQuery("SELECT description,grams FROM cnf_measure WHERE food_code = ? ORDER BY grams", arrayOf(code.toString())).use { c ->
            while (c.moveToNext()) out.add(CnfMeasure(c.getString(0), c.getDouble(1)))
        }
        return out
    }
}