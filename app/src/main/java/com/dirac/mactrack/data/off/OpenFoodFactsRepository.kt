package com.dirac.mactrack.data.off

import com.dirac.mactrack.data.food.FoodDetail
import com.dirac.mactrack.data.food.Nutrients
import com.dirac.mactrack.data.food.PortionUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

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
