package com.vasudev.ai.master.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.vasudev.ai.master.core.Config
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = Config.DATASTORE_NAME)

data class UiSettings(
    val languageTag: String = Config.DEFAULT_LANGUAGE,
    val grounded: Boolean = false,
    val privateMode: Boolean = false,
    /** User-saved example stories / style notes, kept on-device. */
    val styleExamples: String = "",
    /** When true, story generation runs on-device (offline) instead of the cloud. */
    val onDeviceMode: Boolean = false
)

@Singleton
class SettingsStore @Inject constructor(@ApplicationContext private val context: Context) {

    private object Keys {
        val LANGUAGE = stringPreferencesKey("language")
        val GROUNDED = booleanPreferencesKey("grounded")
        val PRIVATE = booleanPreferencesKey("private_mode")
        val STYLE = stringPreferencesKey("style_examples")
        val ON_DEVICE = booleanPreferencesKey("on_device_mode")
    }

    val flow: Flow<UiSettings> = context.dataStore.data.map { p ->
        UiSettings(
            languageTag = p[Keys.LANGUAGE] ?: Config.DEFAULT_LANGUAGE,
            grounded = p[Keys.GROUNDED] ?: false,
            privateMode = p[Keys.PRIVATE] ?: false,
            styleExamples = p[Keys.STYLE] ?: "",
            onDeviceMode = p[Keys.ON_DEVICE] ?: false
        )
    }

    suspend fun setLanguage(tag: String) {
        context.dataStore.edit { it[Keys.LANGUAGE] = tag }
    }

    suspend fun setGrounded(value: Boolean) {
        context.dataStore.edit { it[Keys.GROUNDED] = value }
    }

    suspend fun setPrivate(value: Boolean) {
        context.dataStore.edit { it[Keys.PRIVATE] = value }
    }

    suspend fun setStyleExamples(text: String) {
        context.dataStore.edit { it[Keys.STYLE] = text }
    }

    suspend fun setOnDevice(value: Boolean) {
        context.dataStore.edit { it[Keys.ON_DEVICE] = value }
    }
}
