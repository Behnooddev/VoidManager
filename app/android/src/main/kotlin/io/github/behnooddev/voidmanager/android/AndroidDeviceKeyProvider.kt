package io.github.behnooddev.voidmanager.android

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import io.github.behnooddev.voidmanager.shared.vault.DeviceKeyProvider
import io.github.behnooddev.voidmanager.shared.vault.DeviceKeyResult
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlin.coroutines.resume

/**
 * Holds the device key behind the Android Keystore and strong biometrics.
 *
 * A 256-bit AES key lives in the Keystore (in StrongBox when the device has it) and can be used only
 * after a biometric prompt for that one operation. It is invalidated when biometrics are enrolled
 * or removed. The vault's device key is 32 random bytes that this class encrypts with the Keystore key
 * and stores in `device.key`; the bytes are readable only after a successful prompt.
 */
class AndroidDeviceKeyProvider(
    private val activity: FragmentActivity,
) : DeviceKeyProvider {
    private val file = File(activity.filesDir, FILE_NAME)

    override fun isAvailable(): Boolean =
        BiometricManager.from(activity).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
            BiometricManager.BIOMETRIC_SUCCESS

    override fun hasKey(): Boolean = file.exists() && keyStore().containsAlias(ALIAS)

    override suspend fun createKey(): DeviceKeyResult {
        if (!isAvailable()) return DeviceKeyResult.Unavailable
        val cipher =
            try {
                deleteKey()
                createStoreKey()
                Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, storeKey()) }
            } catch (e: GeneralSecurityException) {
                deleteKey()
                return DeviceKeyResult.Failed
            }
        val authenticated =
            when (val outcome = authenticate(cipher, R.string.biometric_subtitle_enable)) {
                is PromptOutcome.Authenticated -> outcome.cipher
                PromptOutcome.Cancelled -> {
                    deleteKey()
                    return DeviceKeyResult.Cancelled
                }
                PromptOutcome.Failed -> {
                    deleteKey()
                    return DeviceKeyResult.Failed
                }
            }
        val key = ByteArray(KEY_BYTES).also { SecureRandom().nextBytes(it) }
        return try {
            val encrypted = authenticated.doFinal(key)
            writeAtomically(authenticated.iv + encrypted)
            DeviceKeyResult.Key(key.copyOf())
        } catch (e: GeneralSecurityException) {
            deleteKey()
            DeviceKeyResult.Failed
        } catch (e: java.io.IOException) {
            deleteKey()
            DeviceKeyResult.Failed
        } finally {
            key.fill(0)
        }
    }

    override suspend fun releaseKey(): DeviceKeyResult {
        if (!isAvailable()) return DeviceKeyResult.Unavailable
        val stored = readStored() ?: return DeviceKeyResult.Unavailable
        val cipher =
            try {
                Cipher.getInstance(TRANSFORMATION).apply {
                    init(Cipher.DECRYPT_MODE, storeKey(), GCMParameterSpec(TAG_BITS, stored.copyOfRange(0, IV_BYTES)))
                }
            } catch (e: KeyPermanentlyInvalidatedException) {
                return DeviceKeyResult.Invalidated
            } catch (e: GeneralSecurityException) {
                return DeviceKeyResult.Failed
            }
        val authenticated =
            when (val outcome = authenticate(cipher, R.string.biometric_subtitle_unlock)) {
                is PromptOutcome.Authenticated -> outcome.cipher
                PromptOutcome.Cancelled -> return DeviceKeyResult.Cancelled
                PromptOutcome.Failed -> return DeviceKeyResult.Failed
            }
        return try {
            DeviceKeyResult.Key(authenticated.doFinal(stored, IV_BYTES, stored.size - IV_BYTES))
        } catch (e: GeneralSecurityException) {
            DeviceKeyResult.Failed
        }
    }

    override fun deleteKey() {
        try {
            val store = keyStore()
            if (store.containsAlias(ALIAS)) store.deleteEntry(ALIAS)
        } catch (e: java.security.KeyStoreException) {
            // Nothing more can be done; the file is removed below and is useless without the key.
        }
        file.delete()
    }

    private sealed interface PromptOutcome {
        class Authenticated(
            val cipher: Cipher,
        ) : PromptOutcome

        data object Cancelled : PromptOutcome

        data object Failed : PromptOutcome
    }

    /** Shows the system prompt for [cipher]. The prompt is created and shown on the main thread. */
    private suspend fun authenticate(
        cipher: Cipher,
        subtitle: Int,
    ): PromptOutcome =
        suspendCancellableCoroutine { continuation ->
            val callback =
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        val used = result.cryptoObject?.cipher
                        if (continuation.isActive) {
                            continuation.resume(
                                if (used !=
                                    null
                                ) {
                                    PromptOutcome.Authenticated(used)
                                } else {
                                    PromptOutcome.Failed
                                },
                            )
                        }
                    }

                    override fun onAuthenticationError(
                        errorCode: Int,
                        errString: CharSequence,
                    ) {
                        val cancelled =
                            errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                                errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                                errorCode == BiometricPrompt.ERROR_CANCELED
                        if (continuation.isActive) {
                            continuation.resume(if (cancelled) PromptOutcome.Cancelled else PromptOutcome.Failed)
                        }
                    }
                }
            val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), callback)
            val info =
                BiometricPrompt.PromptInfo
                    .Builder()
                    .setTitle(activity.getString(R.string.biometric_title))
                    .setSubtitle(activity.getString(subtitle))
                    .setNegativeButtonText(activity.getString(R.string.biometric_use_password))
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                    .setConfirmationRequired(false)
                    .build()
            continuation.invokeOnCancellation { prompt.cancelAuthentication() }
            prompt.authenticate(info, BiometricPrompt.CryptoObject(cipher))
        }

    private fun keyStore(): KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    private fun storeKey(): SecretKey = keyStore().getKey(ALIAS, null) as SecretKey

    private fun createStoreKey() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                generateStoreKey(strongBox = true)
                return
            } catch (e: StrongBoxUnavailableException) {
                // The device has no StrongBox; the key goes to the regular hardware-backed Keystore instead.
            }
        }
        generateStoreKey(strongBox = false)
    }

    private fun generateStoreKey(strongBox: Boolean) {
        val spec =
            KeyGenParameterSpec
                .Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setKeySize(KEY_BYTES * BITS_PER_BYTE)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setUserAuthenticationRequired(true)
                .setInvalidatedByBiometricEnrollment(true)
                .apply {
                    // Authentication for every single use, and only strong biometrics (not the screen lock).
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
                    }
                    if (strongBox && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) applyStrongBox(this)
                }.build()
        KeyGenerator
            .getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            .apply { init(spec) }
            .generateKey()
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun applyStrongBox(builder: KeyGenParameterSpec.Builder) {
        builder.setIsStrongBoxBacked(true)
    }

    private fun readStored(): ByteArray? = if (file.exists()) file.readBytes().takeIf { it.size > IV_BYTES } else null

    private fun writeAtomically(bytes: ByteArray) {
        val temporary = File(file.parentFile, "$FILE_NAME.tmp")
        temporary.writeBytes(bytes)
        if (!temporary.renameTo(file)) {
            temporary.delete()
            throw java.io.IOException("Could not store the device key")
        }
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val ALIAS = "voidmanager_device_key"
        const val FILE_NAME = "device.key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val KEY_BYTES = 32
        const val BITS_PER_BYTE = 8
        const val IV_BYTES = 12
        const val TAG_BITS = 128
    }
}
