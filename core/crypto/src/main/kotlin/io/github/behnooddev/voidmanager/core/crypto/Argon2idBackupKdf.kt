package io.github.behnooddev.voidmanager.core.crypto

import io.github.behnooddev.voidmanager.core.backup.BackupKdf
import io.github.behnooddev.voidmanager.core.backup.BackupKdfParams

/** The backup key derivation: the same Argon2id as the vault key file, with the cost the backup header names. */
class Argon2idBackupKdf : BackupKdf {
    override fun derive(
        password: ByteArray,
        salt: ByteArray,
        params: BackupKdfParams,
    ): ByteArray =
        Argon2idKdf.derive(
            password,
            salt,
            KdfParams(memoryKiB = params.memoryKiB, iterations = params.iterations, parallelism = params.parallelism),
        )
}
