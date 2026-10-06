package io.github.behnooddev.voidmanager.shared.vault

import io.github.behnooddev.voidmanager.core.data.OpenOutcome
import io.github.behnooddev.voidmanager.core.data.OpenedVault
import io.github.behnooddev.voidmanager.core.data.VaultGateway
import io.github.behnooddev.voidmanager.core.data.VaultRepositories
import io.github.behnooddev.voidmanager.core.security.AutoLockController
import io.github.behnooddev.voidmanager.core.security.AutoLockPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class ControllerFakeVault : OpenedVault {
    var enableResult = true
    var lockCalls = 0

    override val repositories: VaultRepositories
        get() = error("The repositories are not used by these tests")

    override fun enableDeviceUnlock(deviceKey: ByteArray) = enableResult

    override fun lock() {
        lockCalls++
    }
}

private class ControllerFakeGateway(
    private val vaultExists: Boolean,
) : VaultGateway {
    val vault = ControllerFakeVault()
    var unlockResult: OpenOutcome = OpenOutcome.Opened(vault)
    var deviceResult: OpenOutcome = OpenOutcome.Opened(vault)
    var deviceEnabled = true
    var disableCalls = 0
    var lastDeviceKey: ByteArray? = null

    override fun exists() = vaultExists

    override fun create(password: CharArray): OpenOutcome = OpenOutcome.Opened(vault)

    override fun unlock(password: CharArray): OpenOutcome = unlockResult

    override fun deviceUnlockEnabled() = deviceEnabled

    override fun unlockWithDeviceKey(deviceKey: ByteArray): OpenOutcome {
        lastDeviceKey = deviceKey
        return deviceResult
    }

    override fun disableDeviceUnlock() {
        disableCalls++
        deviceEnabled = false
    }
}

private class FakeKeys(
    var available: Boolean = true,
    var stored: Boolean = true,
    var result: DeviceKeyResult = DeviceKeyResult.Cancelled,
) : DeviceKeyProvider {
    var lastKey: ByteArray? = null
    var deleteCalls = 0

    override fun isAvailable() = available

    override fun hasKey() = stored

    override suspend fun createKey(): DeviceKeyResult = result

    override suspend fun releaseKey(): DeviceKeyResult = result

    override fun deleteKey() {
        deleteCalls++
        stored = false
    }
}

class VaultControllerTest {
    private val gateway = ControllerFakeGateway(vaultExists = true)
    private val controller =
        VaultController(
            flow =
                VaultFlow(
                    gateway,
                    AutoLockController({ AutoLockPolicy() }, { 0L }),
                    AttemptThrottle { 0L },
                ),
            worker = Dispatchers.Unconfined,
        )

    private fun newKey() = ByteArray(32) { 7 }

    @Test
    fun unlockOpensTheVaultAndWipesThePassword() =
        runBlocking {
            val password = "synthetic".toCharArray()
            controller.unlock(password)
            assertIs<Stage.Unlocked>(controller.stage)
            assertTrue(password.all { it == '\u0000' })
            assertFalse(controller.busy)
        }

    @Test
    fun aWrongPasswordPublishesTheFailure() =
        runBlocking {
            gateway.unlockResult = OpenOutcome.WrongPasswordOrCorrupt
            controller.unlock("synthetic".toCharArray())
            assertEquals(Stage.Locked, controller.stage)
            assertEquals(Failure.WrongPassword, controller.failure)
        }

    @Test
    fun deviceUnlockOpensTheVaultAndWipesTheReleasedKey() =
        runBlocking {
            val key = newKey()
            val keys = FakeKeys(result = DeviceKeyResult.Key(key))
            controller.unlockWithDevice(keys)
            assertIs<Stage.Unlocked>(controller.stage)
            assertTrue(key.all { it == 0.toByte() }, "the released key must be overwritten")
            assertFalse(controller.busy)
        }

    @Test
    fun aCancelledPromptIsNotAnError() =
        runBlocking {
            controller.unlockWithDevice(FakeKeys(result = DeviceKeyResult.Cancelled))
            assertEquals(Stage.Locked, controller.stage)
            assertNull(controller.failure)
            assertFalse(controller.busy)
        }

    @Test
    fun anInvalidatedKeyTurnsDeviceUnlockOffAndSaysWhy() =
        runBlocking {
            val keys = FakeKeys(result = DeviceKeyResult.Invalidated)
            controller.unlockWithDevice(keys)
            assertEquals(Failure.DeviceUnlockInvalidated, controller.failure)
            assertEquals(1, gateway.disableCalls)
            assertEquals(1, keys.deleteCalls)
        }

    @Test
    fun anUnavailableOrFailedPromptReportsAFailure() =
        runBlocking {
            controller.unlockWithDevice(FakeKeys(result = DeviceKeyResult.Failed))
            assertEquals(Failure.DeviceUnlockFailed, controller.failure)
        }

    @Test
    fun theUnlockButtonIsOfferedOnlyWhenEverythingIsInPlace() {
        assertTrue(controller.deviceUnlockReady(FakeKeys()))
        assertFalse(controller.deviceUnlockReady(FakeKeys(available = false)))
        assertFalse(controller.deviceUnlockReady(FakeKeys(stored = false)))
        gateway.deviceEnabled = false
        assertFalse(controller.deviceUnlockReady(FakeKeys()))
    }

    @Test
    fun enablingNeedsAnUnlockedVault() =
        runBlocking {
            val keys = FakeKeys(result = DeviceKeyResult.Key(newKey()))
            assertEquals(DeviceUnlockChange.Failed, controller.enableDeviceUnlock(keys))
        }

    @Test
    fun enablingStoresTheKeyAndWipesTheCopy() =
        runBlocking {
            controller.unlock("synthetic".toCharArray())
            val key = newKey()
            val change = controller.enableDeviceUnlock(FakeKeys(result = DeviceKeyResult.Key(key)))
            assertEquals(DeviceUnlockChange.Enabled, change)
            assertTrue(key.all { it == 0.toByte() }, "the key copy must be overwritten")
        }

    @Test
    fun aFailedKeyFileWriteRemovesThePlatformKey() =
        runBlocking {
            controller.unlock("synthetic".toCharArray())
            gateway.vault.enableResult = false
            val keys = FakeKeys(result = DeviceKeyResult.Key(newKey()))
            assertEquals(DeviceUnlockChange.Failed, controller.enableDeviceUnlock(keys))
            assertEquals(1, keys.deleteCalls)
        }

    @Test
    fun aCancelledEnablePromptChangesNothing() =
        runBlocking {
            controller.unlock("synthetic".toCharArray())
            val keys = FakeKeys(result = DeviceKeyResult.Cancelled)
            assertEquals(DeviceUnlockChange.Cancelled, controller.enableDeviceUnlock(keys))
            assertEquals(0, keys.deleteCalls)
        }

    @Test
    fun lockReturnsToTheUnlockScreenAndClosesTheVault() =
        runBlocking {
            controller.unlock("synthetic".toCharArray())
            controller.lock()
            assertEquals(Stage.Locked, controller.stage)
            assertEquals(1, gateway.vault.lockCalls)
        }
}
