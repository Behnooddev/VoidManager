package io.github.behnooddev.voidmanager.core.data.repository

import io.github.behnooddev.voidmanager.core.data.logic.LikeEscaper
import io.github.behnooddev.voidmanager.core.data.logic.PhoneNormalizer
import io.github.behnooddev.voidmanager.core.data.logic.TextNormalizer
import io.github.behnooddev.voidmanager.core.database.VoidManagerDatabase
import io.github.behnooddev.voidmanager.core.model.Entity

internal class SqlSearchRepository(
    private val database: VoidManagerDatabase,
) : SearchRepository {
    override fun search(
        query: String,
        limit: Int,
    ): List<Entity> {
        val text = TextNormalizer.normalize(query)
        if (text.isEmpty()) return emptyList()
        // Leading zeros are trunk prefixes: "0912..." must find a number stored as "+98 912...".
        val allDigits = PhoneNormalizer.digits(query)
        val digits = allDigits.trimStart('0').takeIf { it.length >= MIN_DIGITS } ?: allDigits
        val digitPattern = if (digits.length >= MIN_DIGITS) LikeEscaper.contains(digits) else NEVER_MATCHES
        val ids =
            database.fieldValueQueries
                .searchEntities(
                    pattern = LikeEscaper.contains(text),
                    digitPattern = digitPattern,
                    limit = limit.toLong(),
                ).executeAsList()
        return ids.mapNotNull { database.entityQueries.selectById(it, ::mapEntity).executeAsOneOrNull() }
    }

    private companion object {
        const val MIN_DIGITS = 3

        /** A pattern no stored search form can match, because stored forms are never empty. */
        const val NEVER_MATCHES = ""
    }
}
