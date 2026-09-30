package io.github.behnooddev.voidmanager.core.data.logic

import io.github.behnooddev.voidmanager.core.model.FieldSection
import io.github.behnooddev.voidmanager.core.model.Sensitivity
import io.github.behnooddev.voidmanager.core.model.SharingPolicy

enum class ShareDecision { Allow, Ask, Never }

/** Rules that depend on a value's sensitivity. They are enforced in the data layer, not in screens. */
object SensitivityPolicy {
    /** The sensitivity a value is stored with, or null when [requested] would lower the definition default. */
    fun effective(
        definitionDefault: Sensitivity,
        requested: Sensitivity?,
    ): Sensitivity? =
        when {
            requested == null -> definitionDefault
            requested.level < definitionDefault.level -> null
            else -> requested
        }

    /** Whether a value may appear in an ordinary export without an explicit opt-in. */
    fun inDefaultExport(sensitivity: Sensitivity): Boolean = sensitivity != Sensitivity.Secret

    /**
     * Whether a value may be put into a share package.
     * Secret values are never shareable. A value-level policy overrides the section default for
     * everything below Secret.
     */
    fun shareDecision(
        section: FieldSection,
        sensitivity: Sensitivity,
        policy: SharingPolicy,
    ): ShareDecision {
        if (sensitivity == Sensitivity.Secret) return ShareDecision.Never
        return when (policy) {
            SharingPolicy.Allow -> ShareDecision.Allow
            SharingPolicy.Ask -> ShareDecision.Ask
            SharingPolicy.Never -> ShareDecision.Never
            SharingPolicy.Inherit -> sectionDefault(section, sensitivity)
        }
    }

    private fun sectionDefault(
        section: FieldSection,
        sensitivity: Sensitivity,
    ): ShareDecision =
        when {
            sensitivity == Sensitivity.Sensitive -> ShareDecision.Never
            section == FieldSection.Financial -> ShareDecision.Never
            section == FieldSection.Address -> ShareDecision.Ask
            else -> ShareDecision.Allow
        }
}
