package io.github.behnooddev.voidmanager.core.data.logic

import io.github.behnooddev.voidmanager.core.model.FieldDataType

/** Chooses the search form for a value of a given data type. */
object FieldNormalizer {
    fun normalizedFor(
        type: FieldDataType,
        text: String,
    ): String? {
        val result =
            when (type) {
                FieldDataType.Phone -> PhoneNormalizer.digits(text)
                FieldDataType.Number, FieldDataType.Boolean -> return null
                else -> TextNormalizer.normalize(text)
            }
        return result.ifEmpty { null }
    }
}
