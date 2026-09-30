package io.github.behnooddev.voidmanager.core.data.logic

import io.github.behnooddev.voidmanager.core.model.FieldSection
import io.github.behnooddev.voidmanager.core.model.Sensitivity
import io.github.behnooddev.voidmanager.core.model.SharingPolicy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SensitivityPolicyTest {
    @Test
    fun requestedSensitivityCanRaiseTheDefault() {
        assertEquals(Sensitivity.Sensitive, SensitivityPolicy.effective(Sensitivity.Personal, Sensitivity.Sensitive))
    }

    @Test
    fun requestedSensitivityCannotLowerTheDefault() {
        assertNull(SensitivityPolicy.effective(Sensitivity.Sensitive, Sensitivity.Personal))
        assertNull(SensitivityPolicy.effective(Sensitivity.Secret, Sensitivity.Sensitive))
    }

    @Test
    fun missingRequestUsesTheDefault() {
        assertEquals(Sensitivity.Secret, SensitivityPolicy.effective(Sensitivity.Secret, null))
    }

    @Test
    fun secretValuesAreNeverShareableWhateverThePolicy() {
        for (policy in SharingPolicy.entries) {
            assertEquals(
                ShareDecision.Never,
                SensitivityPolicy.shareDecision(FieldSection.Accounts, Sensitivity.Secret, policy),
            )
        }
    }

    @Test
    fun defaultsFollowTheSpecification() {
        val inherit = SharingPolicy.Inherit

        assertEquals(
            ShareDecision.Allow,
            SensitivityPolicy.shareDecision(FieldSection.Identity, Sensitivity.Public, inherit),
        )
        assertEquals(
            ShareDecision.Allow,
            SensitivityPolicy.shareDecision(FieldSection.Contact, Sensitivity.Personal, inherit),
        )
        assertEquals(
            ShareDecision.Allow,
            SensitivityPolicy.shareDecision(FieldSection.Social, Sensitivity.Personal, inherit),
        )
        assertEquals(
            ShareDecision.Ask,
            SensitivityPolicy.shareDecision(FieldSection.Address, Sensitivity.Personal, inherit),
        )
        assertEquals(
            ShareDecision.Never,
            SensitivityPolicy.shareDecision(FieldSection.Financial, Sensitivity.Sensitive, inherit),
        )
        assertEquals(
            ShareDecision.Never,
            SensitivityPolicy.shareDecision(FieldSection.Identity, Sensitivity.Sensitive, inherit),
        )
    }

    @Test
    fun valueLevelPolicyOverridesTheSectionDefaultBelowSecret() {
        assertEquals(
            ShareDecision.Allow,
            SensitivityPolicy.shareDecision(FieldSection.Address, Sensitivity.Personal, SharingPolicy.Allow),
        )
        assertEquals(
            ShareDecision.Never,
            SensitivityPolicy.shareDecision(FieldSection.Contact, Sensitivity.Personal, SharingPolicy.Never),
        )
    }

    @Test
    fun secretValuesAreLeftOutOfDefaultExports() {
        assertFalse(SensitivityPolicy.inDefaultExport(Sensitivity.Secret))
        assertTrue(SensitivityPolicy.inDefaultExport(Sensitivity.Sensitive))
        assertTrue(SensitivityPolicy.inDefaultExport(Sensitivity.Personal))
    }
}
