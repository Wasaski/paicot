package com.paicot.core

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import java.util.UUID

private val Context.prefsDataStore by preferencesDataStore(name = "paicot_prefs")

/** Persistent settings: API token, server bind, privilege mode, SAF tree. */
class Prefs(private val context: Context) {

    private val KEY_TOKEN = stringPreferencesKey("api_token")
    private val KEY_PORT = intPreferencesKey("server_port")
    private val KEY_HOST = stringPreferencesKey("server_host")
    private val KEY_MODE = stringPreferencesKey("privilege_mode")
    private val KEY_SAF_TREE = stringPreferencesKey("saf_tree_uri")

    suspend fun token(): String {
        val existing = context.prefsDataStore.data.first()[KEY_TOKEN]
        if (existing != null && existing.isNotBlank()) return existing
        val generated = UUID.randomUUID().toString().replace("-", "")
        context.prefsDataStore.edit { it[KEY_TOKEN] = generated }
        return generated
    }

    suspend fun regenerateToken(): String {
        val generated = UUID.randomUUID().toString().replace("-", "")
        context.prefsDataStore.edit { it[KEY_TOKEN] = generated }
        return generated
    }

    suspend fun port(): Int = context.prefsDataStore.data.first()[KEY_PORT] ?: DEFAULT_PORT
    suspend fun setPort(port: Int) = context.prefsDataStore.edit { it[KEY_PORT] = port }

    /** Loopback by default; user must explicitly enable 0.0.0.0 in Settings. */
    suspend fun host(): String = context.prefsDataStore.data.first()[KEY_HOST] ?: DEFAULT_HOST
    suspend fun setHost(host: String) = context.prefsDataStore.edit { it[KEY_HOST] = host }

    suspend fun mode(): PrivilegeMode {
        val raw = context.prefsDataStore.data.first()[KEY_MODE]
        return try { PrivilegeMode.valueOf(raw ?: "STANDARD") } catch (_: Exception) { PrivilegeMode.STANDARD }
    }
    suspend fun setMode(mode: PrivilegeMode) = context.prefsDataStore.edit { it[KEY_MODE] = mode.name }

    suspend fun safTreeUri(): String? = context.prefsDataStore.data.first()[KEY_SAF_TREE]
    suspend fun setSafTreeUri(uri: String?) = context.prefsDataStore.edit {
        if (uri == null) it.remove(KEY_SAF_TREE) else it[KEY_SAF_TREE] = uri
    }

    companion object {
        const val DEFAULT_PORT = 7777
        const val DEFAULT_HOST = "127.0.0.1"
    }
}
