package com.dirac.mactrack.data.off

import com.dirac.mactrack.data.food.FoodDetail
import com.dirac.mactrack.data.food.Nutrients
import com.dirac.mactrack.data.food.PortionUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

// A lightweight branded product from an Open Food Facts NAME search: enough to render a search row and
// then open the full detail (by barcode) the same way a barcode scan does.
data class OffProduct(
    val code: String,
    val name: String,
    val kcalPer100: Double,
    val proteinPer100: Double,
    val carbPer100: Double,
    val fatPer100: Double
)

// Looks up a barcode against Open Food Facts -- the "branded" food source. Online-only: on no
// network, a non-200, or a missing product it returns null, and the caller falls back to the
// offline CNF + saved foods. No new dependency: JVM HttpURLConnection + Android's org.json.
class OpenFoodFactsRepository {

    suspend fun lookup(barcode: String): FoodDetail? = withContext(Dispatchers.IO) {
        val json = fetch(barcode) ?: return@withContext null
        if (json.optInt("status", 0) != 1) return@withContext null
        val product = json.optJSONObject("product") ?: return@withContext null
        toFoodDetail(barcode, product)
    }

    // Search Open Food Facts by NAME (so branded/day-to-day items appear when typed, not only by
    // barcode). Returns [] on no network / error / a too-short query, so search stays offline-safe.
    // Each hit opens via the same "branded" barcode path as a scan.
    suspend fun searchByName(query: String, limit: Int = 20): List<OffProduct> = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (q.length < 3) return@withContext emptyList()
        val json = fetchSearch(q, limit) ?: return@withContext emptyList()
        val arr = json.optJSONArray("products") ?: return@withContext emptyList()
        val out = mutableListOf<OffProduct>()
        for (i in 0 until arr.length()) {
            val p = arr.optJSONObject(i) ?: continue
            val code = p.optString("code").filter { it.isDigit() }
            if (code.isEmpty()) continue
            val base = p.optString("product_name").ifBlank { p.optString("product_name_en") }
            if (base.isBlank()) continue
            val brand = p.optString("brands").split(",").firstOrNull()?.trim().orEmpty()
            val name = if (brand.isNotBlank()) "$base ($brand)" else base
            val n = p.optJSONObject("nutriments")
            out.add(
                OffProduct(
                    code = code,
                    name = name,
                    kcalPer100 = n?.optDouble("energy-kcal_100g", 0.0) ?: 0.0,
                    proteinPer100 = n?.optDouble("proteins_100g", 0.0) ?: 0.0,
                    carbPer100 = n?.optDouble("carbohydrates_100g", 0.0) ?: 0.0,
                    fatPer100 = n?.optDouble("fat_100g", 0.0) ?: 0.0
                )
            )
        }
        out
    }

    private fun fetchSearch(query: String, limit: Int): JSONObject? {
        var conn: HttpURLConnection? = null
        return try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = URL(
                "https://world.openfoodfacts.org/cgi/search.pl?search_terms=$encoded" +
                    "&search_simple=1&action=process&json=1&page_size=$limit" +
                    "&fields=code,product_name,product_name_en,brands,nutriments"
            )
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 10000
                setRequestProperty("User-Agent", "MacTrack/0.1 (Android)")
            }
            if (conn.responseCode != 200) return null
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            JSONObject(body)
        } catch (e: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    private fun fetch(barcode: String): JSONObject? {
        val digits = barcode.filter { it.isDigit() }
        if (digits.isEmpty()) return null
        var conn: HttpURLConnection? = null
        return try {
            val url = URL("https://world.openfoodfacts.org/api/v2/product/$digits.json")
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("User-Agent", "MacTrack/0.1 (Android)")
            }
            if (conn.responseCode != 200) return null
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            JSONObject(body)
        } catch (e: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    private fun toFoodDetail(barcode: String, product: JSONObject): FoodDetail? {
        val n = product.optJSONObject("nutriments") ?: return null
        // Per-100g values. Macros/fiber/sugar/satfat are grams; OFF sodium_100g is grams -> mg.
        // OFF's potassium/cholesterol coverage and units are inconsistent, so leave them 0 for
        // now rather than risk order-of-magnitude errors.
        val per100 = Nutrients(
            kcal = n.optDouble("energy-kcal_100g", 0.0),
            protein = n.optDouble("proteins_100g", 0.0),
            carb = n.optDouble("carbohydrates_100g", 0.0),
            fat = n.optDouble("fat_100g", 0.0),
            fiber = n.optDouble("fiber_100g", 0.0),
            sugar = n.optDouble("sugars_100g", 0.0),
            satFat = n.optDouble("saturated-fat_100g", 0.0),
            sodium = n.optDouble("sodium_100g", 0.0) * 1000.0,
            potassium = 0.0,
            cholesterol = 0.0
        )
        val perGram = per100 * (1.0 / 100.0)

        val brand = product.optString("brands").split(",").firstOrNull()?.trim().orEmpty()
        val baseName = product.optString("product_name").ifBlank {
            product.optString("product_name_en").ifBlank { "Barcode $barcode" }
        }
        val name = if (brand.isNotBlank()) "$baseName ($brand)" else baseName

        val servingG = product.optDouble("serving_quantity", 0.0)
        val units = buildList {
            add(PortionUnit("g", perGram, 1.0))
            add(PortionUnit("oz", perGram * 28.3495, 28.3495))
            if (servingG > 0.0) add(PortionUnit("serving", perGram * servingG, servingG))
        }
        return if (servingG > 0.0) {
            FoodDetail(name, units, defaultUnitLabel = "serving", defaultAmount = 1.0)
        } else {
            FoodDetail(name, units, defaultUnitLabel = "g", defaultAmount = 100.0)
        }
    }
}
