package io.github.behnooddev.voidmanager.core.crypto

import io.github.behnooddev.voidmanager.core.backup.BackupKdfParams
import io.github.behnooddev.voidmanager.core.backup.BackupService
import io.github.behnooddev.voidmanager.core.backup.BackupSnapshot
import io.github.behnooddev.voidmanager.core.backup.OpenResult
import io.github.behnooddev.voidmanager.core.backup.SnapshotEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/** One backup through the real Argon2id, so the wiring between the two modules is exercised. */
class BackupWithArgon2idTest {
    private val service = BackupService(Argon2idBackupKdf())
    private val snapshot =
        BackupSnapshot(
            schemaVersion = 1,
            definitions = emptyList(),
            entities = listOf(SnapshotEntity("e1", 0, "Synthetic Person", false, null, 1L, 1L, null)),
            values = emptyList(),
            relationships = emptyList(),
        )

    @Test
    fun aBackupRoundTripsThroughTheRealKeyDerivation() {
        val file = service.create(snapshot, "synthetic backup password".toCharArray(), BackupKdfParams.FLOOR, "test")
        assertEquals(
            snapshot,
            assertIs<OpenResult.Opened>(service.open(file, "synthetic backup password".toCharArray())).snapshot,
        )
    }

    @Test
    fun aWrongPasswordIsRejectedByTheRealKeyDerivation() {
        val file = service.create(snapshot, "synthetic backup password".toCharArray(), BackupKdfParams.FLOOR, "test")
        assertEquals(OpenResult.WrongPasswordOrChanged, service.open(file, "wrong password".toCharArray()))
    }
}
