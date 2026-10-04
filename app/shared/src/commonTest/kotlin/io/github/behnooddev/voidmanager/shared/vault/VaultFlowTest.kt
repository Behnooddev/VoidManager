package io.github.behnooddev.voidmanager.shared.vault

import io.github.behnooddev.voidmanager.core.data.OpenOutcome
import io.github.behnooddev.voidmanager.core.data.OpenedVault
import io.github.behnooddev.voidmanager.core.data.VaultGateway
import io.github.behnooddev.voidmanager.core.data.VaultRepositories
import io.github.behnooddev.voidmanager.core.security.AutoLockController
import io.github.behnooddev.voidmanager.core.security.AutoLockPolicy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

private class FakeVault : OpenedVault {
    var lockCalls = 0
    var enableResult = true
    var enabledWith: ByteArray? = null

    override fun enableDeviceUnlock(deviceKey: ByteArray): Boolean {
        enabledWith = deviceKey.copyOf()
        return enableResult
    }

    override val repositories: VaultRepositories
        get() = error("The repositories are not used by these tests")

    override fun lock() {
        lockCalls++
    }
}

private class FakeGateway(
    var vaultExists: Boolean,
) : VaultGateway {
    val vault = FakeVault()
    var createResult: OpenOutcome = OpenOutcome.Opened(vault)
    var unlockResult: OpenOutcome = OpenOutcome.Opened(vault)
    var unlockCalls = 0
    var deviceEnabled = false
    var deviceResult: OpenOutcome = OpenOutcome.Opened(vault)
    var disableCalls = 0

    override fun exists() = vaultExists

    override fun deviceUnlockEnabled() = deviceEnabled

    override fun unlockWithDeviceKey(deviceKey: ByteArray): OpenOutcome = deviceResult

    override fun disableDeviceUnlock() {
        disableCalls++
        deviceEnabled = false
    }

    override fun create(password: CharArray): OpenOutcome = createResult

    override fun unlock(password: CharArray): OpenOutcome {
        unlockCalls++
        return unlockResult
    }
}

class VaultFlowTest {
    private var now = 0L
    private var policy =
        AutoLockPolicy(idleTimeoutMillis = 60_000L, lockOnBackground = true, backgroundGraceMillis = 0L)
    private val password = "synthetic".toCharArray()

    private fun flow(gateway: FakeGateway): VaultFlow =
        VaultFlow(gateway, AutoLockController({ policy }, { now }), AttemptThrottle { now })

    @Test
    fun theStageFollowsWhetherAVaultExists() {
        assertEquals(Stage.NeedsSetup, flow(FakeGateway(vaultExists = false)).stage)
        assertEquals(Stage.Locked, flow(FakeGateway(vaultExists = true)).stage)
    }

    @Test
    fun creatingAVaultLeavesItUnlocked() {
        val gateway = FakeGateway(vaultExists = false)
        val flow = flow(gateway)
        flow.create(password)
        val stage = assertIs<Stage.Unlocked>(flow.stage)
        assertSame(gateway.vault, stage.vault)
        assertNull(flow.failure)
    }

    @Test
    fun createDoesNothingWhenAVaultAlreadyExists() {
        val flow = flow(FakeGateway(vaultExists = true))
        flow.create(password)
        assertEquals(Stage.Locked, flow.stage)
    }

    @Test
    fun aWrongPasswordKeepsTheVaultLockedAndNamesTheFailure() {
        val gateway = FakeGateway(vaultExists = true).apply { unlockResult = OpenOutcome.WrongPasswordOrCorrupt }
        val flow = flow(gateway)
        flow.unlock(password)
        assertEquals(Stage.Locked, flow.stage)
        assertEquals(Failure.WrongPassword, flow.failure)
    }

    @Test
    fun aSuccessfulUnlockClearsAnEarlierFailure() {
        val gateway = FakeGateway(vaultExists = true).apply { unlockResult = OpenOutcome.WrongPasswordOrCorrupt }
        val flow = flow(gateway)
        flow.unlock(password)
        gateway.unlockResult = OpenOutcome.Opened(gateway.vault)
        flow.unlock(password)
        assertIs<Stage.Unlocked>(flow.stage)
        assertNull(flow.failure)
    }

    @Test
    fun otherOutcomesMapToTheirFailures() {
        val cases =
            mapOf(
                OpenOutcome.InvalidKeyFile to Failure.InvalidKeyFile,
                OpenOutcome.UnsupportedVersion(9) to Failure.UnsupportedVersion,
                OpenOutcome.NewerData to Failure.NewerData,
                OpenOutcome.StorageFailure to Failure.StorageFailure,
            )
        for ((outcome, expected) in cases) {
            val flow = flow(FakeGateway(vaultExists = true).apply { unlockResult = outcome })
            flow.unlock(password)
            assertEquals(Stage.Locked, flow.stage)
            assertEquals(expected, flow.failure)
        }
    }

    @Test
    fun aMissingKeyFileSendsTheUserBackToSetup() {
        val flow = flow(FakeGateway(vaultExists = true).apply { unlockResult = OpenOutcome.NoVault })
        flow.unlock(password)
        assertEquals(Stage.NeedsSetup, flow.stage)
    }

    @Test
    fun repeatedWrongPasswordsAreThrottledAndTheGatewayIsNotCalledDuringTheWait() {
        val gateway = FakeGateway(vaultExists = true).apply { unlockResult = OpenOutcome.WrongPasswordOrCorrupt }
        val flow = flow(gateway)
        repeat(5) { flow.unlock(password) }
        assertEquals(5, gateway.unlockCalls)
        assertEquals(5_000L, flow.throttleRemainingMillis())

        flow.unlock(password)
        assertEquals(5, gateway.unlockCalls)

        now += 5_000
        flow.unlock(password)
        assertEquals(6, gateway.unlockCalls)
    }

    @Test
    fun lockingClosesTheVaultOnceAndReturnsToLocked() {
        val gateway = FakeGateway(vaultExists = false)
        val flow = flow(gateway)
        flow.create(password)
        flow.lock()
        flow.lock()
        assertEquals(Stage.Locked, flow.stage)
        assertEquals(1, gateway.vault.lockCalls)
    }

    @Test
    fun theVaultLocksAtOnceWhenTheAppGoesToTheBackground() {
        val gateway = FakeGateway(vaultExists = false)
        val flow = flow(gateway)
        flow.create(password)
        flow.onBackgrounded()
        assertEquals(Stage.Locked, flow.stage)
        assertEquals(1, gateway.vault.lockCalls)
    }

    @Test
    fun aBackgroundGraceKeepsTheVaultOpenUntilTheGraceEnds() {
        policy = policy.copy(backgroundGraceMillis = 30_000L)
        val flow = flow(FakeGateway(vaultExists = false))
        flow.create(password)
        flow.onBackgrounded()
        assertIs<Stage.Unlocked>(flow.stage)

        now += 10_000
        flow.onForegrounded()
        assertIs<Stage.Unlocked>(flow.stage)

        flow.onBackgrounded()
        now += 31_000
        flow.onForegrounded()
        assertEquals(Stage.Locked, flow.stage)
    }

    @Test
    fun theIdleTimeoutLocksAndInteractionPostponesIt() {
        val flow = flow(FakeGateway(vaultExists = false))
        flow.create(password)

        now += 50_000
        flow.onInteraction()
        now += 50_000
        flow.checkAutoLock()
        assertIs<Stage.Unlocked>(flow.stage)

        now += 10_000
        flow.checkAutoLock()
        assertEquals(Stage.Locked, flow.stage)
    }

    @Test
    fun lifecycleEventsDoNothingWhileLocked() {
        val gateway = FakeGateway(vaultExists = true)
        val flow = flow(gateway)
        flow.onBackgrounded()
        flow.onForegrounded()
        flow.checkAutoLock()
        flow.onInteraction()
        assertEquals(Stage.Locked, flow.stage)
        assertEquals(0, gateway.vault.lockCalls)
    }

    @Test
    fun theIdleTimerStartsFreshAfterEveryUnlock() {
        val flow = flow(FakeGateway(vaultExists = true))
        now += 500_000
        flow.unlock(password)
        flow.checkAutoLock()
        assertTrue(flow.stage is Stage.Unlocked)
    }

    private val deviceKey = ByteArray(32) { 5 }

    @Test
    fun deviceUnlockOpensTheVaultAndIsNotThrottled() {
        val gateway = FakeGateway(vaultExists = true).apply { deviceResult = OpenOutcome.Opened(vault) }
        val flow = flow(gateway)
        flow.unlockWithDeviceKey(deviceKey)
        assertIs<Stage.Unlocked>(flow.stage)
        assertNull(flow.failure)
    }

    @Test
    fun aRejectedDeviceKeyTurnsDeviceUnlockOffWithoutThrottlingThePassword() {
        val gateway =
            FakeGateway(vaultExists = true).apply {
                deviceEnabled = true
                deviceResult = OpenOutcome.DeviceKeyRejected
            }
        val flow = flow(gateway)
        repeat(6) { flow.unlockWithDeviceKey(deviceKey) }
        assertEquals(Stage.Locked, flow.stage)
        assertEquals(Failure.DeviceUnlockFailed, flow.failure)
        assertEquals(0L, flow.throttleRemainingMillis())
        assertTrue(!flow.deviceUnlockEnabled())
    }

    @Test
    fun anInvalidatedKeyRemovesTheSlotAndNamesTheReason() {
        val gateway = FakeGateway(vaultExists = true).apply { deviceEnabled = true }
        val flow = flow(gateway)
        flow.deviceKeyInvalidated()
        assertEquals(Failure.DeviceUnlockInvalidated, flow.failure)
        assertEquals(1, gateway.disableCalls)
    }

    @Test
    fun deviceUnlockIsIgnoredUnlessTheVaultIsLocked() {
        val gateway = FakeGateway(vaultExists = false)
        val flow = flow(gateway)
        flow.unlockWithDeviceKey(deviceKey)
        assertEquals(Stage.NeedsSetup, flow.stage)
    }

    @Test
    fun enablingDeviceUnlockNeedsAnUnlockedVaultAndPassesTheKeyOn() {
        val gateway = FakeGateway(vaultExists = false)
        val flow = flow(gateway)
        assertTrue(!flow.enableDeviceUnlock(deviceKey))

        flow.create(password)
        assertTrue(flow.enableDeviceUnlock(deviceKey))
        assertEquals(deviceKey.toList(), gateway.vault.enabledWith?.toList())

        gateway.vault.enableResult = false
        assertTrue(!flow.enableDeviceUnlock(deviceKey))
    }
}
