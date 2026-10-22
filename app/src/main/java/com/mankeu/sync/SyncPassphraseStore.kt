package com.mankeu.sync

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * The sync passphrase, typed once per phone (DEC-024) and kept encrypted by a non-exportable
 * Android Keystore key, so automatic sync never has to ask again. Losing the phone loses only this
 * copy; the passphrase itself is never sent anywhere and cannot be recovered by the app.
 */
class SyncPassphraseStore(context: Context, name: String = PREFS) {
    private val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)

    fun exists(): Boolean = prefs.contains(KEY_VALUE)

    fun save(passphrase: CharArray) {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, key()) }
        // Encoded straight from the char array: a String copy could not be wiped.
        val buffer = Charsets.UTF_8.encode(CharBuffer.wrap(passphrase))
        val bytes = ByteArray(buffer.remaining()).also { buffer.get(it) }
        buffer.array().fill(0)
        val sealed = cipher.iv + cipher.doFinal(bytes)
        bytes.fill(0)
        prefs.edit().putString(KEY_VALUE, Base64.encodeToString(sealed, Base64.NO_WRAP)).apply()
    }

    fun load(): CharArray? {
        val sealed = prefs.getString(KEY_VALUE, null)?.let { Base64.decode(it, Base64.NO_WRAP) } ?: return null
        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, sealed, 0, IV_BYTES))
            val bytes = cipher.doFinal(sealed, IV_BYTES, sealed.size - IV_BYTES)
            val chars = Charsets.UTF_8.decode(ByteBuffer.wrap(bytes))
            CharArray(chars.remaining()).also { chars.get(it); chars.array().fill(' '); bytes.fill(0) }
        }.getOrNull() // a reset Keystore (e.g. restored to a new phone) means: ask again
    }

    fun clear() {
        prefs.edit().remove(KEY_VALUE).apply()
    }

    private fun key(): SecretKey {
        val store = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (store.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE).apply {
            init(
                KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build(),
            )
        }.generateKey()
    }

    private companion object {
        const val PREFS = "mankeu_sync" // tests pass their own name to play two phones in one process
        const val KEY_VALUE = "passphrase"
        const val KEYSTORE = "AndroidKeyStore"
        const val ALIAS = "mankeu_sync_passphrase"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_BYTES = 12
    }
}
