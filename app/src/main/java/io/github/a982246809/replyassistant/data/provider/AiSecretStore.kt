package io.github.a982246809.replyassistant.data.provider

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.nio.ByteBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface AiSecretStore {
    suspend fun readApiKey(): String?
    suspend fun writeApiKey(value: String)
    suspend fun clearApiKey()
}

@Singleton
class AndroidKeystoreAiSecretStore @Inject constructor(
    @ApplicationContext context: Context
) : AiSecretStore {
    private val secretFile = File(context.noBackupFilesDir, FILE_NAME)

    override suspend fun readApiKey(): String? = withContext(Dispatchers.IO) {
        readApiKeyBlocking()
    }

    override suspend fun writeApiKey(value: String) = withContext(Dispatchers.IO) {
        writeApiKeyBlocking(value)
    }

    override suspend fun clearApiKey() = withContext(Dispatchers.IO) {
        clearApiKeyBlocking()
    }

    @Synchronized
    private fun readApiKeyBlocking(): String? {
        if (!secretFile.exists()) return null
        return runCatching {
            val payload = secretFile.readBytes()
            require(payload.size > VERSION_BYTES + IV_BYTES)
            val buffer = ByteBuffer.wrap(payload)
            val version = buffer.get()
            require(version == VERSION)
            val ivLength = buffer.get().toInt() and 0xFF
            require(ivLength in 12..32)
            val iv = ByteArray(ivLength).also(buffer::get)
            val encrypted = ByteArray(buffer.remaining()).also(buffer::get)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateKey(),
                GCMParameterSpec(128, iv)
            )
            cipher.doFinal(encrypted).toString(Charsets.UTF_8)
        }.getOrElse {
            clearApiKeyBlocking()
            null
        }
    }

    @Synchronized
    private fun writeApiKeyBlocking(value: String) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        val iv = cipher.iv
        val payload = ByteBuffer.allocate(VERSION_BYTES + 1 + iv.size + encrypted.size)
            .put(VERSION)
            .put(iv.size.toByte())
            .put(iv)
            .put(encrypted)
            .array()
        secretFile.writeBytes(payload)
    }

    @Synchronized
    private fun clearApiKeyBlocking() {
        if (secretFile.exists()) secretFile.delete()
        runCatching {
            val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
            if (keyStore.containsAlias(KEY_ALIAS)) keyStore.deleteEntry(KEY_ALIAS)
        }
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE).run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
            )
            generateKey()
        }
    }

    private companion object {
        const val FILE_NAME = "ai-provider.secret"
        const val KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "replyassistant_ai_provider_key_v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val VERSION: Byte = 1
        const val VERSION_BYTES = 1
        const val IV_BYTES = 1
    }
}
