package com.github.com.chenjia404.meshchat.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.github.com.chenjia404.meshchat.core.dispatchers.ApplicationScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import javax.inject.Inject
import javax.inject.Singleton

private val Context.meshChatDataStore by preferencesDataStore(name = "meshchat_settings")

@Singleton
class SettingsStore @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope applicationScope: CoroutineScope,
) {
    private object Keys {
        val BASE_URL = stringPreferencesKey("base_url")
    }

    val baseUrlFlow: StateFlow<String> = context.meshChatDataStore.data
        .map { preferences ->
            preferences[Keys.BASE_URL]?.trim().orEmpty().ifBlank { DEFAULT_BASE_URL }
        }
        .stateIn(
            scope = applicationScope,
            started = SharingStarted.Eagerly,
            initialValue = DEFAULT_BASE_URL,
        )

    suspend fun setBaseUrl(value: String) {
        context.meshChatDataStore.edit { preferences: MutablePreferences ->
            preferences[Keys.BASE_URL] = value.trim().ifBlank { DEFAULT_BASE_URL }
        }
    }

    fun currentBaseUrl(): String = baseUrlFlow.value.ensureTrailingSlash()

    fun currentBaseHttpUrl(): HttpUrl = currentBaseUrl().toHttpUrl()

    fun currentWebSocketUrl(): String {
        val base = currentBaseUrl().removeSuffix("/")
        val wsBase = when {
            base.startsWith("https://") -> base.replaceFirst("https://", "wss://")
            base.startsWith("http://") -> base.replaceFirst("http://", "ws://")
            else -> "ws://$base"
        }
        return "$wsBase/api/v1/chat/ws"
    }

    companion object {
        const val DEFAULT_BASE_URL = "http://127.0.0.1:19080/"
    }
}

private fun String.ensureTrailingSlash(): String = if (endsWith("/")) this else "$this/"
