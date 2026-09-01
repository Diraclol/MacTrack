package com.dirac.mactrack.data.ai

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

// Defaults: Gemini via its OpenAI-compatible endpoint. Base URL and model are plain config; the API
// key is the only secret. Everything is user-editable so a "local server" (homelab) base URL can be
// dropped in later with no code change.
const val DEFAULT_AI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/openai/"
const val DEFAULT_AI_MODEL = "gemini-3.5-flash-lite"

// Stores the AI connection settings. Base URL and model live in plain SharedPreferences; the API key
// is encrypted with a hardware-backed Android Keystore AES-GCM key and only the ciphertext is
// persisted, so the raw key never sits in prefs. No third-party dependency.
class AiSettingsStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("ai_settings", Context.MODE_PRIVATE)

    private val _baseUrl = MutableStateFlow(prefs.getString(KEY_BASE_URL, DEFAULT_AI_BASE_URL) ?: DEFAULT_AI_BASE_URL)
    val baseUrl: StateFlow<String> = _baseUrl.asStateFlow()

    private val _model = MutableStateFlow(prefs.getString(KEY_MODEL, DEFAULT_AI_MODEL) ?: DEFAULT_AI_MODEL)
    val model: StateFlow<String> = _model.asStateFlow()

    private val _hasKey = MutableStateFlow(prefs.contains(KEY_API_KEY))
    val hasKey: StateFlow<Boolean> = _hasKey.asStateFlow()

    fun setBaseUrl(url: String) {
        val v = url.trim().ifBlank { DEFAULT_AI_BASE_URL }
        prefs.edit().putString(KEY_BASE_URL, v).apply()
        _baseUrl.value = v
    }

    fun setModel(model: String) {
        val v = model.trim().ifBlank { DEFAULT_AI_MODEL }
        prefs.edit().putString(KEY_MODEL, v).apply()
        _model.value = v
    }

    fun setApiKey(key: String) {
        val trimmed = key.trim()
        if (trimmed.isEmpty()) {
            clearApiKey()
            return
        }
        prefs.edit().putString(KEY_API_KEY, encrypt(trimmed)).apply()
        _hasKey.value = true
    }

    fun clearApiKey() {
        prefs.edit().remove(KEY_API_KEY).apply()
        _hasKey.value = false
    }

    // Decrypted key for making a request. Returns null if unset or if decryption fails (e.g. the
    // Keystore entry was invalidated), in which case the caller should prompt for the key again.
    fun apiKey(): String? {
        val stored = prefs.getString(KEY_API_KEY, null) ?: return null
        return try {
            decrypt(stored)
        } catch (e: Exception) {
            null
        }
    }

    // --- Keystore AES-GCM ---

    private fun secretKey(): SecretKey {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (ks.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
        )
        return generator.generateKey()
    }

    // Serialized as base64(iv):base64(ciphertext).
    private fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val iv = cipher.iv
        val ct = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(iv, Base64.NO_WRAP) + ":" + Base64.encodeToString(ct, Base64.NO_WRAP)
    }

    private fun decrypt(stored: String): String {
        val parts = stored.split(":")
        require(parts.size == 2) { "malformed ciphertext" }
        val iv = Base64.decode(parts[0], Base64.NO_WRAP)
        val ct = Base64.decode(parts[1], Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, iv))
        return String(cipher.doFinal(ct), Charsets.UTF_8)
    }

    private companion object {
        const val KEY_BASE_URL = "base_url"
        const val KEY_MODEL = "model"
        const val KEY_API_KEY = "api_key_enc"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "mactrack_ai_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
