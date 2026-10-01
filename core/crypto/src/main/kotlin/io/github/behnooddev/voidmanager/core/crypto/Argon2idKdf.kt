package io.github.behnooddev.voidmanager.core.crypto

import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters

/**
 * Argon2id cost parameters. They are stored with every derived key, so they can be raised later.
 * The bounds protect against weak settings and against a tampered key file that asks for
 * an amount of memory large enough to crash the application.
 */
data class KdfParams(
    val memoryKiB: Int,
    val iterations: Int,
    val parallelism: Int,
) {
    init {
        require(memoryKiB in MIN_MEMORY_KIB..MAX_MEMORY_KIB) { "Memory cost out of range" }
        require(iterations in MIN_ITERATIONS..MAX_ITERATIONS) { "Iteration count out of range" }
        require(parallelism in 1..MAX_PARALLELISM) { "Parallelism out of range" }
    }

    companion object {
        /** 19 MiB and 2 iterations is the lowest setting the OWASP password storage guidance accepts for Argon2id. */
        const val MIN_MEMORY_KIB = 19 * 1024
        const val MAX_MEMORY_KIB = 512 * 1024
        const val MIN_ITERATIONS = 2
        const val MAX_ITERATIONS = 10
        const val MAX_PARALLELISM = 8

        val FLOOR = KdfParams(MIN_MEMORY_KIB, MIN_ITERATIONS, 1)

        val DEFAULT = KdfParams(memoryKiB = 64 * 1024, iterations = 3, parallelism = 1)
    }
}

/** Argon2id (RFC 9106) from Bouncy Castle. */
object Argon2idKdf {
    const val SALT_BYTES = 16
    const val OUTPUT_BYTES = 32

    fun derive(
        password: ByteArray,
        salt: ByteArray,
        params: KdfParams,
    ): ByteArray {
        require(salt.size == SALT_BYTES) { "The salt must be $SALT_BYTES bytes" }
        return run(password, salt, params.memoryKiB, params.iterations, params.parallelism, null, null, OUTPUT_BYTES)
    }

    /** Exposed for the RFC 9106 test vector, which uses parameters below the production floor. */
    internal fun deriveUnchecked(
        password: ByteArray,
        salt: ByteArray,
        memoryKiB: Int,
        iterations: Int,
        parallelism: Int,
        secret: ByteArray?,
        additional: ByteArray?,
        outputBytes: Int,
    ): ByteArray = run(password, salt, memoryKiB, iterations, parallelism, secret, additional, outputBytes)

    @Suppress("LongParameterList")
    private fun run(
        password: ByteArray,
        salt: ByteArray,
        memoryKiB: Int,
        iterations: Int,
        parallelism: Int,
        secret: ByteArray?,
        additional: ByteArray?,
        outputBytes: Int,
    ): ByteArray {
        val builder =
            Argon2Parameters
                .Builder(Argon2Parameters.ARGON2_id)
                .withVersion(Argon2Parameters.ARGON2_VERSION_13)
                .withIterations(iterations)
                .withMemoryAsKB(memoryKiB)
                .withParallelism(parallelism)
                .withSalt(salt)
        if (secret != null) builder.withSecret(secret)
        if (additional != null) builder.withAdditional(additional)
        val generator = Argon2BytesGenerator()
        generator.init(builder.build())
        val out = ByteArray(outputBytes)
        generator.generateBytes(password, out)
        return out
    }
}
