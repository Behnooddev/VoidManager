package io.github.behnooddev.voidmanager.core.database

sealed interface OpenPlan {
    /** No schema exists yet. */
    data object Create : OpenPlan

    data object UpToDate : OpenPlan

    /** The stored schema is older. A snapshot is taken before [from] is migrated to [to]. */
    data class Migrate(
        val from: Long,
        val to: Long,
    ) : OpenPlan

    /** The stored schema is newer than this build understands. The vault must not be opened. */
    data class RefuseNewer(
        val stored: Long,
        val supported: Long,
    ) : OpenPlan
}

object MigrationPlanner {
    fun plan(
        stored: Long?,
        supported: Long,
    ): OpenPlan =
        when {
            stored == null -> OpenPlan.Create
            stored == supported -> OpenPlan.UpToDate
            stored < supported -> OpenPlan.Migrate(stored, supported)
            else -> OpenPlan.RefuseNewer(stored, supported)
        }
}
