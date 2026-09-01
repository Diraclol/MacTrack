package com.dirac.mactrack.data.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

// Retry a rate-limited / temporarily-unavailable request a few times with exponential backoff
// (1s, 2s, 4s) before giving up, so a transient 429 doesn't fail the chat outright.
private const val MAX_RETRIES = 3
private fun backoffMs(attempt: Int): Long = 1000L * (1L shl (attempt - 1))

// One chat turn. `imageDataUrl` (a data:image/...;base64,... string) is sent as an OpenAI vision
// content part; null means a plain text turn.
data class ChatMessage(
    val role: String,          // "system" | "user" | "assistant"
    val content: String,
    val imageDataUrl: String? = null
)

class AiException(message: String) : Exception(message)

// A minimal OpenAI-compatible chat client over HttpURLConnection (no third-party dependency). Talks
// to Gemini's OpenAI endpoint by default, or any OpenAI-compatible server (Ollama/Open WebUI) if the
// base URL is changed. Streams tokens via SSE.
class AiClient {

    // Streams assistant content deltas. Throws AiException on a non-200 or transport error, which the
    // caller collects inside a try/catch.
    fun stream(baseUrl: String, apiKey: String, model: String, messages: List<ChatMessage>): Flow<String> = flow {
        val body = requestBody(model, messages, stream = true).toByteArray(Charsets.UTF_8)
        var attempt = 0
        while (true) {
            val conn = (URL(endpoint(baseUrl)).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 15_000
                readTimeout = 120_000
                setRequestProperty("Content-Type", "application/json")
                if (apiKey.isNotBlank()) setRequestProperty("Authorization", "Bearer $apiKey")
                setRequestProperty("Accept", "text/event-stream")
            }
            try {
                conn.outputStream.use { it.write(body) }
                val code = conn.responseCode
                if (code == 200) {
                    conn.inputStream.bufferedReader().useLines { lines ->
                        for (raw in lines) {
                            val line = raw.trim()
                            if (!line.startsWith("data:")) continue
                            val data = line.removePrefix("data:").trim()
                            if (data == "[DONE]") break
                            val delta = runCatching {
                                JSONObject(data).getJSONArray("choices").getJSONObject(0)
                                    .optJSONObject("delta")?.optString("content").orEmpty()
                            }.getOrDefault("")
                            if (delta.isNotEmpty()) emit(delta)
                        }
                    }
                    return@flow
                }
                val err = conn.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                // Retry only transient rate-limit / unavailable codes; everything else fails now.
                if (!(code == 429 || code == 503) || attempt >= MAX_RETRIES) {
                    throw AiException(parseError(code, err))
                }
            } finally {
                conn.disconnect()
            }
            attempt++
            delay(backoffMs(attempt))
        }
    }.flowOn(Dispatchers.IO)

    // A cheap non-streaming call to verify the base URL, key, and model resolve.
    suspend fun testConnection(baseUrl: String, apiKey: String, model: String): Result<Unit> = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            conn = (URL(endpoint(baseUrl)).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 15_000
                readTimeout = 30_000
                setRequestProperty("Content-Type", "application/json")
                if (apiKey.isNotBlank()) setRequestProperty("Authorization", "Bearer $apiKey")
            }
            val body = requestBody(model, listOf(ChatMessage("user", "ping")), stream = false)
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            if (conn.responseCode == 200) {
                Result.success(Unit)
            } else {
                Result.failure(AiException(parseError(conn.responseCode, conn.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty())))
            }
        } catch (e: Exception) {
            Result.failure(AiException(e.message ?: "Connection failed"))
        } finally {
            conn?.disconnect()
        }
    }

    private fun endpoint(baseUrl: String): String = baseUrl.trim().trimEnd('/') + "/chat/completions"

    private fun requestBody(model: String, messages: List<ChatMessage>, stream: Boolean): String {
        val arr = JSONArray()
        messages.forEach { m ->
            val obj = JSONObject().put("role", m.role)
            if (m.imageDataUrl != null) {
                // Vision turn: content is an array of text + image_url parts.
                val parts = JSONArray()
                if (m.content.isNotBlank()) {
                    parts.put(JSONObject().put("type", "text").put("text", m.content))
                }
                parts.put(
                    JSONObject().put("type", "image_url")
                        .put("image_url", JSONObject().put("url", m.imageDataUrl))
                )
                obj.put("content", parts)
            } else {
                obj.put("content", m.content)
            }
            arr.put(obj)
        }
        return JSONObject().put("model", model).put("stream", stream).put("messages", arr).toString()
    }

    private fun parseError(code: Int, body: String): String {
        val msg = runCatching { JSONObject(body).optJSONObject("error")?.optString("message") }.getOrNull()
        return when {
            code == 401 -> "Unauthorized — check your API key."
            code == 403 -> "Forbidden — the key may not have access to this model."
            code == 404 -> "Not found — check the base URL and model name."
            code == 429 -> "Rate limit or quota exceeded. Try again later."
            !msg.isNullOrBlank() -> msg
            else -> "Request failed (HTTP $code)."
        }
    }
}
