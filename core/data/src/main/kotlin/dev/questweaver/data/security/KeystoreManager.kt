package dev.questweaver.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * Manages encryption keys for database using Android Keystore.
 * 
 * Provides secure generation and retrieval of AES-256-GCM encryption keys
 * with hardware-backed security where available.
 */
class KeystoreManager(@Suppress("UNUSED_PARAMETER") context: Context) {
    
    private val keyStore: KeyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply {
        load(null)
    }
    
    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "questweaver_db_key"
        private const val KEY_SIZE = 256
    }
    
    /**
     * Gets the database encryption key, creating it if it doesn't exist.
     * 
     * @return ByteArray containing the 256-bit AES encryption key
     */
    fun getOrCreateDatabaseKey(): ByteArray {
        return if (keyStore.containsAlias(KEY_ALIAS)) {
            retrieveKey()
        } else {
            generateAndStoreKey()
        }
    }
    
    /**
     * Generates a new AES-256-GCM key and stores it in Android Keystore.
     * 
     * Configures the key with:
     * - Hardware-backed security where available
     * - No user authentication required (allows background access)
     * - AES-256-GCM encryption algorithm
     * 
     * @return ByteArray containing the generated key
     */
    private fun generateAndStoreKey(): ByteArray {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            KEYSTORE_PROVIDER
        )
        
        val keyGenSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_SIZE)
            .setUserAuthenticationRequired(false) // Allow background access
            .build()
        
        keyGenerator.init(keyGenSpec)
        val secretKey: SecretKey = keyGenerator.generateKey()
        
        return secretKey.encoded
    }
    
    /**
     * Retrieves an existing encryption key from Android Keystore.
     * 
     * @return ByteArray containing the stored key
     * @throws IllegalStateException if the key doesn't exist
     */
    private fun retrieveKey(): ByteArray {
        val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        checkNotNull(entry) { "Key not found in keystore: $KEY_ALIAS" }
        
        return entry.secretKey.encoded
    }
}
