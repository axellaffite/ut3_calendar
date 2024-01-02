package com.edt.ut3.backend.credentials

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.edt.ut3.backend.requests.authentication_services.Credentials
import java.util.Dictionary
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.jsonObject
import com.edt.ut3.misc.extensions.toJsonOject
import com.edt.ut3.misc.extensions.toStringMap

/**
 * This class is used to store the user credentials.
 *
 * @property context
 */
class CredentialsManager private constructor(val context: Context) {

    companion object {

        fun getInstance(applicationContext: Context): CredentialsManager {
            return CredentialsManager(applicationContext)
        }
    }

    /**
     * Returns the master key.
     * Used to encrypt the shared preferences.
     */
    private fun getMasterKey() = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)


    private fun getCredentialsPreferenceFile(): SharedPreferences {
        return EncryptedSharedPreferences.create(
            "credentials",
            getMasterKey(),
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }


    /**
     * Save the credentials into an encrypted file.
     *
     * @param credentials The user credentials
     */
    fun saveCredentials(credentials: Credentials) = synchronized(this) {
        getCredentialsPreferenceFile().edit {
            putString("username", credentials.username)
            putString("password", credentials.password)
            putString("disambiguationIdentity", credentials.disambiguationIdentity)
        }
    }

    /**
     * Return the credentials if they exist.
     * Otherwise returns null.
     *
     * @return The credentials or null
     */
    fun getCredentials(): Credentials? = synchronized(this) {
        getCredentialsPreferenceFile().run {
            val username = getString("username", null)
            val password = getString("password", null)
            val disambiguationIdentity = getString("disambiguationIdentity", null)

            Credentials.from(username, password, disambiguationIdentity)
        }
    }

    /**
     * Clear the credentials stored in memory.
     */
    fun clearCredentials(): Unit = synchronized(this){
        getCredentialsPreferenceFile().edit {
            putString("username", null)
            putString("password", null)
            putString("disambiguationIdentity", null)
        }
    }

    fun getLocalVariables(): Map<String, String>? = synchronized(this) {
        getCredentialsPreferenceFile().run{
            val localStorageStr = getString("localStorage", null) ?: return null
            Json.parseToJsonElement(localStorageStr).jsonObject.toStringMap()
        }
    }

    fun clearLocalVariables(): Unit = synchronized(this) {
        getCredentialsPreferenceFile().edit{
            remove("localStorage")
        }
    }

    fun saveLocalVariables(localVariables: Map<String, String>): Unit = synchronized(this) {
        getCredentialsPreferenceFile().edit {
            putString("localStorage", localVariables.toJsonOject().toString())
        }
    }

}