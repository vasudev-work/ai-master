package com.vasudev.ai.master.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.vasudev.ai.master.core.Config
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Encrypted storage for user-entered secrets. Nothing here ships inside the APK. */
@Singleton
class SecurePrefs @Inject constructor(@ApplicationContext context: Context) {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            Config.SECURE_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    var llmApiKey: String
        get() = prefs.getString(KEY_API, "").orEmpty()
        set(value) { prefs.edit().putString(KEY_API, value).apply() }

    var baseUrl: String
        get() = prefs.getString(KEY_BASE, Config.DEFAULT_BASE_URL).orEmpty()
        set(value) { prefs.edit().putString(KEY_BASE, value).apply() }

    var model: String
        get() = prefs.getString(KEY_MODEL, Config.DEFAULT_MODEL).orEmpty()
        set(value) { prefs.edit().putString(KEY_MODEL, value).apply() }

    var searchApiKey: String
        get() = prefs.getString(KEY_SEARCH, "").orEmpty()
        set(value) { prefs.edit().putString(KEY_SEARCH, value).apply() }

    private companion object {
        const val KEY_API = "llm_api_key"
        const val KEY_BASE = "llm_base_url"
        const val KEY_MODEL = "model_name"
        const val KEY_SEARCH = "search_api_key"
    }
}
