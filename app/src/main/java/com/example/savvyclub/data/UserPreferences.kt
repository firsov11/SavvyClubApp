// UserPreferences.kt
package com.example.savvyclub.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.savvyclub.data.model.Comrade
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore("user_prefs")

class UserPreferences(private val context: Context) {

    companion object {
        val UID_KEY = stringPreferencesKey("uid")
        val NAME_KEY = stringPreferencesKey("name")
        val EMAIL_KEY = stringPreferencesKey("email")
        val AVATAR_KEY = stringPreferencesKey("avatar")
    }

    suspend fun saveUser(comrade: Comrade) {
        context.dataStore.edit { prefs ->
            prefs[UID_KEY] = comrade.uid ?: ""
            prefs[NAME_KEY] = comrade.name
            prefs[EMAIL_KEY] = comrade.email
            prefs[AVATAR_KEY] = comrade.avatarUrl
        }
    }

    /** 🔹 Очистка при выходе */
    suspend fun clearUser() {
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
    }

    val userFlow: Flow<Comrade?> = context.dataStore.data.map { prefs ->
        val uid = prefs[UID_KEY]
        if (uid.isNullOrEmpty()) {
            null
        } else {
            Comrade(
                uid = uid,
                name = prefs[NAME_KEY] ?: "",
                email = prefs[EMAIL_KEY] ?: "",
                avatarUrl = prefs[AVATAR_KEY] ?: "",
                isOnline = false // локально off
            )
        }
    }
}
