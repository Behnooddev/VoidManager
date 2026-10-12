package io.github.behnooddev.voidmanager.core.backup

/** A backup file or payload that does not follow the format. The message never contains stored text. */
class BackupFormatException(
    message: String,
) : Exception(message)

/** A backup that is larger than the configured limits, before or after decompression. */
class BackupTooLargeException(
    message: String,
) : Exception(message)
