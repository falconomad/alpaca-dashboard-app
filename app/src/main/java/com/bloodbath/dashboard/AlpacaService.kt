package com.bloodbath.dashboard

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.OkHttpClient
import okhttp3.Request

object AlpacaService {
    private val client = OkHttpClient()
    private val gson = Gson()

    fun getCredentials(context: Context): Pair<String, String> {
        return try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            val sharedPreferences = EncryptedSharedPreferences.create(
                "alpaca_keys",
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            val key = sharedPreferences.getString("api_key", "") ?: ""
            val secret = sharedPreferences.getString("api_secret", "") ?: ""
            Pair(key, secret)
        } catch (e: Exception) {
            Pair("", "")
        }
    }

    fun saveCredentials(context: Context, key: String, secret: String) {
        try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            val sharedPreferences = EncryptedSharedPreferences.create(
                "alpaca_keys",
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            sharedPreferences.edit().apply {
                putString("api_key", key)
                putString("api_secret", secret)
                apply()
            }
        } catch (e: Exception) {
            // Log/ignore security initialization issues on early emulation
        }
    }

    fun fetchAccount(context: Context): Map<String, Any>? {
        val (key, secret) = getCredentials(context)
        if (key.isEmpty() || secret.isEmpty()) return null

        val request = Request.Builder()
            .url("https://paper-api.alpaca.markets/v2/account")
            .addHeader("APCA-API-KEY-ID", key)
            .addHeader("APCA-API-SECRET-KEY", secret)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                val type = object : TypeToken<Map<String, Any>>() {}.type
                gson.fromJson(body, type)
            }
        } catch (e: Exception) {
            null
        }
    }

    fun fetchTransactions(context: Context): List<Map<String, Any>>? {
        val (key, secret) = getCredentials(context)
        if (key.isEmpty() || secret.isEmpty()) return null

        val request = Request.Builder()
            .url("https://paper-api.alpaca.markets/v2/account/activities?activity_types=FILL")
            .addHeader("APCA-API-KEY-ID", key)
            .addHeader("APCA-API-SECRET-KEY", secret)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                val type = object : TypeToken<List<Map<String, Any>>>() {}.type
                gson.fromJson(body, type)
            }
        } catch (e: Exception) {
            null
        }
    }
}
