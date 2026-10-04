package io.github.behnooddev.voidmanager.shared.vault

import io.github.behnooddev.voidmanager.core.security.PasswordAssessment
import io.github.behnooddev.voidmanager.core.security.PasswordPolicy

/** What the setup screen shows and whether the Create button may be pressed. */
data class SetupCheck(
    val assessment: PasswordAssessment,
    val passwordEntered: Boolean,
    val confirmationEntered: Boolean,
    val matches: Boolean,
    val acknowledged: Boolean,
) {
    val canCreate: Boolean
        get() = passwordEntered && assessment.isAcceptable && matches && acknowledged

    /** A mismatch is reported only once something has been typed into the confirmation field. */
    val showMismatch: Boolean
        get() = confirmationEntered && !matches
}

object SetupForm {
    fun check(
        password: CharSequence,
        confirmation: CharSequence,
        acknowledged: Boolean,
        policy: PasswordPolicy,
    ): SetupCheck =
        SetupCheck(
            assessment = policy.assess(password),
            passwordEntered = password.isNotEmpty(),
            confirmationEntered = confirmation.isNotEmpty(),
            matches = password.isNotEmpty() && password.toString() == confirmation.toString(),
            acknowledged = acknowledged,
        )
}
