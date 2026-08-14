package com.divafinance.core.common

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.convert
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import platform.CoreCrypto.CCKeyDerivationPBKDF
import platform.CoreCrypto.kCCPBKDF2
import platform.CoreCrypto.kCCPRFHmacAlgSHA256
import platform.Security.SecRandomCopyBytes
import platform.Security.kSecRandomDefault

@OptIn(ExperimentalForeignApi::class)
actual object SecurityUtils {
    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH = 32
    private const val SALT_LENGTH = 32

    actual fun generateSalt(): String {
        val saltBytes = ByteArray(SALT_LENGTH)
        saltBytes.usePinned { pinned ->
            SecRandomCopyBytes(kSecRandomDefault, SALT_LENGTH.convert(), pinned.addressOf(0))
        }
        return saltBytes.joinToString("") { byte ->
            (byte.toInt() and 0xFF).toString(16).padStart(2, '0')
        }
    }

    actual fun hashPin(pin: String, salt: String): String {
        val saltBytes = salt.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        val pinBytes = pin.encodeToByteArray()
        val derivedKey = ByteArray(KEY_LENGTH)

        memScoped {
            pinBytes.usePinned { pinPinned ->
                saltBytes.usePinned { saltPinned ->
                    derivedKey.usePinned { keyPinned ->
                        CCKeyDerivationPBKDF(
                            algorithm = kCCPBKDF2,
                            password = pinPinned.addressOf(0).reinterpret(),
                            passwordLen = pinBytes.size.convert(),
                            salt = saltPinned.addressOf(0).reinterpret(),
                            saltLen = saltBytes.size.convert(),
                            prf = kCCPRFHmacAlgSHA256,
                            rounds = ITERATIONS.convert(),
                            derivedKey = keyPinned.addressOf(0).reinterpret(),
                            derivedKeyLen = KEY_LENGTH.convert(),
                        )
                    }
                }
            }
        }

        return derivedKey.joinToString("") { byte ->
            (byte.toInt() and 0xFF).toString(16).padStart(2, '0')
        }
    }
}
