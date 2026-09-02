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
        // Match on a singular stem so a plural query ("eggs") also finds singular CNF names ("Egg, ...").
        val terms = q.split(Regex("\\s+")).map { stem(it) }
        val where = terms.joinToString(" AND ") { "name LIKE ?" }
        // Rank a name that STARTS WITH the query's first word above ones that merely contain it, then by
        // length (shorter = closer). So "eggs" prefers "Egg, chicken, ..." over "Fish, salmon, ..., eggs".
        val sql = "SELECT $cols FROM cnf_food WHERE $where " +
            "ORDER BY CASE WHEN name LIKE ? THEN 0 ELSE 1 END, length(name) LIMIT $limit"
        val args = (terms.map { "%$it%" } + "${terms.first()}%").toTypedArray()
        val out = mutableListOf<CnfFood>()
        open().rawQuery(sql, args).use { c -> while (c.moveToNext()) out.add(readFood(c)) }
        return out
    }

    // A tiny singulariser: drop a trailing "s" (not "ss") from a longer word so "eggs" -> "egg",
    // "oats" -> "oat". Deliberately naive -- it just widens the LIKE match; ranking still picks the best.
    private fun stem(term: String): String =
        if (term.length > 3 && term.endsWith("s") && !term.endsWith("ss")) term.dropLast(1) else term

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