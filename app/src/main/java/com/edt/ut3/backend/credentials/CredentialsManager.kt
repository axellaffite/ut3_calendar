package com.edt.ut3.backend.credentials

import android.content.Context
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.edt.ut3.backend.requests.authentication_services.Authenticator


/**
 * This class is used to store the user credentials.
 *
 * @property context
 */
class CredentialsManager private constructor(val context: Context) {

    companion object {
        var instance: CredentialsManager? = null

        fun getInstance(applicationContext: Context): CredentialsManager {
            synchronized(this) {
                if (instance == null) {
                    instance = CredentialsManager(applicationContext)
                }

                return instance!!
            }
        }
    }

    /**
     * Returns the master key.
     * Used to encrypt the shared preferences.
     */
    private fun getMasterKey() = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

    /**
     * Returns the credentials preference file
     * which is an encrypted file.
     *
     * Creates it if it doesn't exist.
     */
    private fun getCredentialsPreferenceFile() = EncryptedSharedPreferences.create(
        "credentials",
        getMasterKey(),
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    /**
     * Save the credentials into an
     * encrypted file.
     *
     * @param credentials The user credentials
     */
    fun saveCredentials(credentials: Authenticator.Credentials) {
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
    fun getCredentials(): Authenticator.Credentials? {
        getCredentialsPreferenceFile().run {
            val username = getString("username", null)
            val password = getString("password", null)

            return Authenticator.Credentials.from(username, password)
        }
    }

    /**
     * Clear the credentials stored in memory.
     */
    fun clearCredentials() {
        getCredentialsPreferenceFile().edit {
            putString("username", null)
            putString("password", null)
        }
    }

}