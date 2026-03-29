package com.github.com.chenjia404.meshchat.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.github.com.chenjia404.meshchat.core.dispatchers.ApplicationScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import com.github.com.chenjia404.meshchat.core.util.normalizeMeshChatServerBaseUrl
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
        /** meshchat-server 当前登录用户 id（`POST /groups/.../join` 返回的 member.user.id） */
        val MESHCHAT_SERVER_USER_ID = longPreferencesKey("meshchat_server_user_id")
        /** 直连 meshchat-server 的 JWT（与 [MESHCHAT_SERVER_JWT_API_BASE] 成对） */
        val MESHCHAT_SERVER_JWT = stringPreferencesKey("meshchat_server_jwt")
        /** 该 JWT 对应的 API 根（如 `https://chat.example.com/`） */
        val MESHCHAT_SERVER_JWT_API_BASE = stringPreferencesKey("meshchat_server_jwt_api_base")
        /** 曾加入过超级群的 meshchat-server 根地址集合（`https://host/`） */
        val MESHCHAT_JOINED_SERVER_BASES = stringSetPreferencesKey("meshchat_joined_server_bases")
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

    val meshChatServerUserIdFlow: StateFlow<Long?> = context.meshChatDataStore.data
        .map { preferences -> preferences[Keys.MESHCHAT_SERVER_USER_ID] }
        .stateIn(
            scope = applicationScope,
            started = SharingStarted.Eagerly,
            initialValue = null,
        )

    /** 已记录、曾加入超级群的 meshchat-server 根地址（规范化带 `/`） */
    val meshChatJoinedServerBasesFlow: StateFlow<Set<String>> = context.meshChatDataStore.data
        .map { preferences -> preferences[Keys.MESHCHAT_JOINED_SERVER_BASES] ?: emptySet() }
        .stateIn(
            scope = applicationScope,
            started = SharingStarted.Eagerly,
            initialValue = emptySet(),
        )

    /** meshchat-server JWT 与对应 API 根（与 [getMeshChatServerJwtPair] 一致），供超级群 WebSocket 驱动连接。 */
    val meshChatServerJwtPairFlow: StateFlow<Pair<String?, String?>> = context.meshChatDataStore.data
        .map { preferences ->
            Pair(preferences[Keys.MESHCHAT_SERVER_JWT], preferences[Keys.MESHCHAT_SERVER_JWT_API_BASE])
        }
        .stateIn(
            scope = applicationScope,
            started = SharingStarted.Eagerly,
            initialValue = Pair(null, null),
        )

    suspend fun getMeshChatJoinedServerBases(): Set<String> {
        val prefs = context.meshChatDataStore.data.first()
        return prefs[Keys.MESHCHAT_JOINED_SERVER_BASES] ?: emptySet()
    }

    suspend fun addMeshChatJoinedServerBase(baseUrl: String) {
        val n = normalizeMeshChatServerBaseUrl(baseUrl)
        context.meshChatDataStore.edit { preferences ->
            val cur = preferences[Keys.MESHCHAT_JOINED_SERVER_BASES] ?: emptySet()
            preferences[Keys.MESHCHAT_JOINED_SERVER_BASES] = cur + n
        }
    }

    suspend fun setBaseUrl(value: String) {
        context.meshChatDataStore.edit { preferences: MutablePreferences ->
            preferences[Keys.BASE_URL] = value.trim().ifBlank { DEFAULT_BASE_URL }
        }
    }

    fun currentBaseUrl(): String = baseUrlFlow.value.ensureTrailingSlash()

    fun currentBaseHttpUrl(): HttpUrl = currentBaseUrl().toHttpUrl()

    suspend fun setMeshChatServerUserId(id: Long?) {
        context.meshChatDataStore.edit { preferences ->
            if (id == null) {
                preferences.remove(Keys.MESHCHAT_SERVER_USER_ID)
            } else {
                preferences[Keys.MESHCHAT_SERVER_USER_ID] = id
            }
        }
    }

    suspend fun getMeshChatServerJwtPair(): Pair<String?, String?> {
        val prefs = context.meshChatDataStore.data.first()
        return Pair(prefs[Keys.MESHCHAT_SERVER_JWT], prefs[Keys.MESHCHAT_SERVER_JWT_API_BASE])
    }

    suspend fun setMeshChatServerJwt(token: String?, apiBase: String?) {
        context.meshChatDataStore.edit { preferences ->
            if (token.isNullOrBlank() || apiBase.isNullOrBlank()) {
                preferences.remove(Keys.MESHCHAT_SERVER_JWT)
                preferences.remove(Keys.MESHCHAT_SERVER_JWT_API_BASE)
            } else {
                preferences[Keys.MESHCHAT_SERVER_JWT] = token
                preferences[Keys.MESHCHAT_SERVER_JWT_API_BASE] = apiBase.trimEnd('/') + "/"
            }
        }
    }

    suspend fun clearMeshChatServerJwt() {
        setMeshChatServerJwt(null, null)
    }

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
