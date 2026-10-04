package io.github.behnooddev.voidmanager.shared.ui

import androidx.compose.runtime.Composable
import io.github.behnooddev.voidmanager.shared.resources.Res
import io.github.behnooddev.voidmanager.shared.resources.failure_invalid_key
import io.github.behnooddev.voidmanager.shared.resources.failure_newer
import io.github.behnooddev.voidmanager.shared.resources.failure_storage
import io.github.behnooddev.voidmanager.shared.resources.failure_unsupported
import io.github.behnooddev.voidmanager.shared.resources.failure_wrong
import io.github.behnooddev.voidmanager.shared.vault.Failure
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun failureText(failure: Failure): String =
    stringResource(
        when (failure) {
            Failure.WrongPassword -> Res.string.failure_wrong
            Failure.InvalidKeyFile -> Res.string.failure_invalid_key
            Failure.UnsupportedVersion -> Res.string.failure_unsupported
            Failure.NewerData -> Res.string.failure_newer
            Failure.StorageFailure -> Res.string.failure_storage
        },
    )
