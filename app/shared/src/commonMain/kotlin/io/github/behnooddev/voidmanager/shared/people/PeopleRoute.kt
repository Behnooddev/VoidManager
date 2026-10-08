package io.github.behnooddev.voidmanager.shared.people

/**
 * Where the People and Personal tabs are. A route is saved across screen rotations as a string,
 * so it holds identifiers only, never personal data.
 */
sealed interface PeopleRoute {
    data object Home : PeopleRoute

    data object QuickAdd : PeopleRoute

    data object Trash : PeopleRoute

    data class Profile(
        val entityId: String,
    ) : PeopleRoute

    data class Rename(
        val entityId: String,
    ) : PeopleRoute

    /** [valueId] is null when a new value is being added. */
    data class FieldEditor(
        val entityId: String,
        val valueId: String?,
    ) : PeopleRoute

    fun encode(): String =
        when (this) {
            Home -> HOME
            QuickAdd -> QUICK_ADD
            Trash -> TRASH
            is Profile -> "$PROFILE$SEP$entityId"
            is Rename -> "$RENAME$SEP$entityId"
            is FieldEditor -> "$FIELD$SEP$entityId$SEP${valueId ?: NEW}"
        }

    companion object {
        private const val SEP = "/"
        private const val HOME = "home"
        private const val QUICK_ADD = "quickadd"
        private const val TRASH = "trash"
        private const val PROFILE = "profile"
        private const val RENAME = "rename"
        private const val FIELD = "field"
        private const val NEW = "new"

        /** Returns null for text that is not a route, so the caller can fall back to its default. */
        fun parse(text: String): PeopleRoute? {
            val parts = text.split(SEP)
            return when {
                text == HOME -> Home
                text == QUICK_ADD -> QuickAdd
                text == TRASH -> Trash
                parts.size == 2 && parts[0] == PROFILE && parts[1].isNotEmpty() -> Profile(parts[1])
                parts.size == 2 && parts[0] == RENAME && parts[1].isNotEmpty() -> Rename(parts[1])
                parts.size == 3 && parts[0] == FIELD && parts[1].isNotEmpty() && parts[2].isNotEmpty() ->
                    FieldEditor(parts[1], parts[2].takeUnless { it == NEW })
                else -> null
            }
        }
    }
}
