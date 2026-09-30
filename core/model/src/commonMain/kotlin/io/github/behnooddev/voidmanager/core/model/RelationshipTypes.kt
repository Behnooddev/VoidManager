package io.github.behnooddev.voidmanager.core.model

data class RelationshipType(
    val key: String,
    val label: String,
    /** Key of the type that describes the same relationship from the other person's side. */
    val reciprocalKey: String,
) {
    val id: String get() = idForKey(key)
    val isSymmetric: Boolean get() = reciprocalKey == key

    companion object {
        fun idForKey(key: String): String = "sys.$key"
    }
}

/**
 * Built-in relationship types. "A is a Brother of B" means B is, by default, a Sibling of A.
 * Gendered types fall back to a neutral reciprocal because the application does not store gender.
 * When creating a relationship, the caller may choose a different reciprocal type.
 */
object RelationshipTypes {
    val all: List<RelationshipType> =
        listOf(
            RelationshipType("sibling", "Sibling", "sibling"),
            RelationshipType("brother", "Brother", "sibling"),
            RelationshipType("sister", "Sister", "sibling"),
            RelationshipType("parent", "Parent", "child"),
            RelationshipType("father", "Father", "child"),
            RelationshipType("mother", "Mother", "child"),
            RelationshipType("child", "Child", "parent"),
            RelationshipType("son", "Son", "parent"),
            RelationshipType("daughter", "Daughter", "parent"),
            RelationshipType("grandparent", "Grandparent", "grandchild"),
            RelationshipType("grandchild", "Grandchild", "grandparent"),
            RelationshipType("spouse", "Spouse", "spouse"),
            RelationshipType("partner", "Partner", "partner"),
            RelationshipType("friend", "Friend", "friend"),
            RelationshipType("colleague", "Colleague", "colleague"),
            RelationshipType("cousin", "Cousin", "cousin"),
            RelationshipType("relative", "Relative", "relative"),
            RelationshipType("manager", "Manager", "report"),
            RelationshipType("report", "Direct report", "manager"),
        )

    private val byKey = all.associateBy { it.key }

    fun byKey(key: String): RelationshipType? = byKey[key]

    /** The default reciprocal type of [key], or null when the key is unknown. */
    fun reciprocalOf(key: String): RelationshipType? = byKey[key]?.let { byKey[it.reciprocalKey] }
}
