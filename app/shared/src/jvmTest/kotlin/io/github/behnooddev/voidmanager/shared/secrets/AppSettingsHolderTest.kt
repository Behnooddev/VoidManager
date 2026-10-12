package io.github.behnooddev.voidmanager.shared.secrets

import io.github.behnooddev.voidmanager.shared.settings.AppSettings
import io.github.behnooddev.voidmanager.shared.settings.AppSettingsHolder
import io.github.behnooddev.voidmanager.shared.settings.SettingsStore
import kotlin.test.Test
import kotlin.test.assertEquals

private class FakeStore(
    var saved: Map<String, String> = emptyMap(),
    var failReads: Boolean = false,
    var failWrites: Boolean = false,
) : SettingsStore {
    override fun read(): Map<String, String> = if (failReads) error("unreadable") else saved

    override fun write(values: Map<String, String>) {
        if (failWrites) error("disk full")
        saved = values
    }
}

class AppSettingsHolderTest {
    @Test
    fun savedSettingsAreReadAtStart() {
        val store = FakeStore(AppSettings(revealSeconds = 60).toMap())
        assertEquals(60, AppSettingsHolder(store).current.revealSeconds)
    }

    @Test
    fun anUnreadableStoreFallsBackToTheDefaults() {
        assertEquals(AppSettings(), AppSettingsHolder(FakeStore(failReads = true)).current)
    }

    @Test
    fun aChangeIsAppliedAndSavedAtOnce() {
        val store = FakeStore()
        val holder = AppSettingsHolder(store)
        holder.update { it.copy(clipboardClearSeconds = 120) }
        assertEquals(120, holder.current.clipboardClearSeconds)
        assertEquals("120", store.saved["clipboard_clear_seconds"])
    }

    @Test
    fun aValueThatIsNotOneOfTheChoicesIsNotAccepted() {
        val holder = AppSettingsHolder(FakeStore())
        holder.update { it.copy(idleLockSeconds = 5) }
        assertEquals(AppSettings.DEFAULT_IDLE_LOCK, holder.current.idleLockSeconds)
    }

    @Test
    fun aFailedWriteStillKeepsTheChangeForThisRun() {
        val holder = AppSettingsHolder(FakeStore(failWrites = true))
        holder.update { it.copy(revealSeconds = 10) }
        assertEquals(10, holder.current.revealSeconds)
    }
}
