package com.edt.ut3.refactored.models.repositories

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.edt.ut3.refactored.models.domain.Credentials
import com.edt.ut3.refactored.models.domain.from


/**
 * This class is used to store the user credentials.
 *
 * @property context
 */
class CredentialsRepository(val context: Context) {

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

            Credentials.from(username, password)
        }
    }

    /**
     * Clear the credentials stored in memory.
     */
    fun clearCredentials(): Unit = synchronized(this){
        getCredentialsPreferenceFile().edit {
            putString("username", null)
            putString("password", null)
        }
    }

}