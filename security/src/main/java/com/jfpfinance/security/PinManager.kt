package com.jfpfinance.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec

/**
 * PIN de acesso ao aplicativo (seção 21).
 *
 * O PIN nunca é guardado. Guardamos apenas um derivado (PBKDF2 com sal aleatório),
 * e esse derivado ainda é cifrado com uma chave AES que vive no Android Keystore
 * e não pode ser extraída do aparelho.
 *
 * Após [MAX_TENTATIVAS] erros seguidos, novas tentativas ficam bloqueadas por um
 * tempo que dobra a cada novo erro.
 */
class PinManager(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("jfp_seguranca", Context.MODE_PRIVATE)

    fun temPinDefinido(): Boolean = prefs.contains(CHAVE_HASH)

    fun definirPin(pin: String) {
        require(pinValido(pin)) { "O PIN deve ter de $TAMANHO_MINIMO a $TAMANHO_MAXIMO dígitos." }
        val sal = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val derivado = derivar(pin, sal)
        val cipher = Cipher.getInstance(TRANSFORMACAO)
        cipher.init(Cipher.ENCRYPT_MODE, obterOuCriarChave())
        val cifrado = cipher.doFinal(derivado)
        prefs.edit()
            .putString(CHAVE_SAL, codificar(sal))
            .putString(CHAVE_IV, codificar(cipher.iv))
            .putString(CHAVE_HASH, codificar(cifrado))
            .putInt(CHAVE_ERROS, 0)
            .putLong(CHAVE_BLOQUEADO_ATE, 0L)
            .apply()
    }

    fun verificarPin(pin: String): ResultadoPin {
        val restante = segundosDeBloqueioRestantes()
        if (restante > 0) return ResultadoPin.Bloqueado(restante)

        val sal = prefs.getString(CHAVE_SAL, null)?.let(::decodificar)
        val iv = prefs.getString(CHAVE_IV, null)?.let(::decodificar)
        val cifrado = prefs.getString(CHAVE_HASH, null)?.let(::decodificar)
        if (sal == null || iv == null || cifrado == null) return ResultadoPin.SemPin

        val esperado = try {
            val cipher = Cipher.getInstance(TRANSFORMACAO)
            cipher.init(Cipher.DECRYPT_MODE, obterOuCriarChave(), GCMParameterSpec(128, iv))
            cipher.doFinal(cifrado)
        } catch (e: Exception) {
            // Chave do Keystore perdida (ex.: restauração em outro aparelho).
            return ResultadoPin.SemPin
        }

        return if (MessageDigest.isEqual(esperado, derivar(pin, sal))) {
            prefs.edit().putInt(CHAVE_ERROS, 0).putLong(CHAVE_BLOQUEADO_ATE, 0L).apply()
            ResultadoPin.Correto
        } else {
            registrarErro()
        }
    }

    fun segundosDeBloqueioRestantes(): Long {
        val ate = prefs.getLong(CHAVE_BLOQUEADO_ATE, 0L)
        val falta = ate - System.currentTimeMillis()
        return if (falta > 0) (falta + 999) / 1000 else 0
    }

    private fun registrarErro(): ResultadoPin {
        val erros = prefs.getInt(CHAVE_ERROS, 0) + 1
        val editor = prefs.edit().putInt(CHAVE_ERROS, erros)
        if (erros >= MAX_TENTATIVAS) {
            val excedente = (erros - MAX_TENTATIVAS).coerceAtMost(6)
            val segundos = 30L shl excedente // 30s, 60s, 120s ... até 32 min
            editor.putLong(CHAVE_BLOQUEADO_ATE, System.currentTimeMillis() + segundos * 1000)
            editor.apply()
            return ResultadoPin.Bloqueado(segundos)
        }
        editor.apply()
        return ResultadoPin.Incorreto(tentativasRestantes = MAX_TENTATIVAS - erros)
    }

    private fun derivar(pin: String, sal: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), sal, ITERACOES, 256)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun obterOuCriarChave(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val gerador = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        gerador.init(
            KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return gerador.generateKey()
    }

    private fun codificar(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)
    private fun decodificar(texto: String): ByteArray = Base64.decode(texto, Base64.NO_WRAP)

    companion object {
        const val TAMANHO_MINIMO = 4
        const val TAMANHO_MAXIMO = 8
        const val MAX_TENTATIVAS = 5

        fun pinValido(pin: String): Boolean =
            pin.length in TAMANHO_MINIMO..TAMANHO_MAXIMO && pin.all { it.isDigit() }

        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val ALIAS = "jfp_finance_pin"
        private const val TRANSFORMACAO = "AES/GCM/NoPadding"
        private const val ITERACOES = 120_000
        private const val CHAVE_SAL = "pin_sal"
        private const val CHAVE_IV = "pin_iv"
        private const val CHAVE_HASH = "pin_hash"
        private const val CHAVE_ERROS = "pin_erros"
        private const val CHAVE_BLOQUEADO_ATE = "pin_bloqueado_ate"
    }
}

sealed interface ResultadoPin {
    data object Correto : ResultadoPin
    data class Incorreto(val tentativasRestantes: Int) : ResultadoPin
    data class Bloqueado(val segundos: Long) : ResultadoPin
    data object SemPin : ResultadoPin
}
