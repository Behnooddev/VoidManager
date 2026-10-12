package io.github.behnooddev.voidmanager.shared.people

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.behnooddev.voidmanager.core.data.repository.EntityRepository
import io.github.behnooddev.voidmanager.core.data.repository.FieldDefinitionRepository
import io.github.behnooddev.voidmanager.core.data.repository.FieldValueRepository
import io.github.behnooddev.voidmanager.core.data.repository.SearchRepository
import io.github.behnooddev.voidmanager.core.model.Entity
import io.github.behnooddev.voidmanager.core.model.EntityKind
import io.github.behnooddev.voidmanager.core.model.FailureReason
import io.github.behnooddev.voidmanager.core.model.FieldDataType
import io.github.behnooddev.voidmanager.core.model.FieldDefinitionInfo
import io.github.behnooddev.voidmanager.core.model.FieldValueInput
import io.github.behnooddev.voidmanager.core.model.Outcome
import io.github.behnooddev.voidmanager.core.model.Sensitivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.cancellation.CancellationException

/** Something the screen should tell the user about. */
enum class Problem {
    NotFound,
    InvalidInput,
    Conflict,
    NotAllowed,

    /** The database could not be read or written, for example because the vault was locked meanwhile. */
    Storage,

    /** The person was saved but at least one of the values next to the name was not. */
    PartiallySaved,

    /** Another field already uses this label. */
    LabelTaken,

    /** A custom field still has values; they have to be moved or deleted first. */
    FieldInUse,
}

sealed interface QuickAddResult {
    data class Saved(
        val entityId: String,
    ) : QuickAddResult

    /** The person exists; see [Problem.PartiallySaved]. */
    data class Partial(
        val entityId: String,
    ) : QuickAddResult

    data object Invalid : QuickAddResult

    data object Failed : QuickAddResult
}

/**
 * The state behind the People and Personal screens. Repository calls block, so every one runs on
 * [worker]. A call that fails because the vault was locked in the meantime ends in [Problem.Storage]
 * instead of an exception. The model belongs to one unlocked vault and is discarded when the vault locks.
 */
class PeopleModel(
    private val entities: EntityRepository,
    private val fields: FieldValueRepository,
    private val fieldDefinitions: FieldDefinitionRepository,
    private val search: SearchRepository,
    private val worker: CoroutineContext = Dispatchers.Default,
) {
    var people: List<Entity> by mutableStateOf(emptyList())
        private set

    var query: String by mutableStateOf("")
        private set

    var trashed: List<Entity> by mutableStateOf(emptyList())
        private set

    var profile: ProfileView? by mutableStateOf(null)
        private set

    var selfId: String? by mutableStateOf(null)
        private set

    /** Every field definition of this vault, hidden ones included. */
    var definitions: List<FieldDefinitionInfo> by mutableStateOf(emptyList())
        private set

    var problem: Problem? by mutableStateOf(null)
        private set

    fun clearProblem() {
        problem = null
    }

    suspend fun setQuery(text: String) {
        query = text
        loadPeople()
    }

    suspend fun loadPeople() {
        val text = query.trim()
        val found = blocking { if (text.isEmpty()) entities.listActive() else search.search(text) } ?: return
        // A slower, older search must not overwrite the results of what was typed since.
        if (text != query.trim()) return
        people = found.filter { it.kind == EntityKind.Person }
    }

    suspend fun loadSelf() {
        selfId = blocking { entities.ensureSelf("").id }
    }

    suspend fun loadProfile(entityId: String) {
        val loaded =
            blocking {
                val entity = entities.get(entityId)
                if (entity == null ||
                    entity.isTrashed
                ) {
                    null
                } else {
                    // Definitions are read together with the values so the profile never shows a stale label.
                    val current = fieldDefinitions.list()
                    definitions = current
                    ProfileBuilder.build(entity, fields.listForEntity(entityId), current)
                }
            }
        profile = loaded
        if (loaded == null && problem == null) problem = Problem.NotFound
    }

    suspend fun loadDefinitions() {
        definitions = blocking { fieldDefinitions.list() } ?: return
    }

    suspend fun createCustomField(
        label: String,
        dataType: FieldDataType,
        level: Sensitivity,
        allowsMultiple: Boolean,
    ): Boolean {
        problem = null
        outcome(Problem.LabelTaken) { fieldDefinitions.createCustom(label, dataType, level, allowsMultiple) }
            ?: return false
        loadDefinitions()
        return true
    }

    suspend fun setFieldHidden(
        id: String,
        hidden: Boolean,
    ) {
        problem = null
        outcome { fieldDefinitions.setHidden(id, hidden) } ?: return
        loadDefinitions()
    }

    /** A blank [label] restores the default label of a built-in field. */
    suspend fun renameField(
        id: String,
        label: String,
    ): Boolean {
        problem = null
        outcome(Problem.LabelTaken) { fieldDefinitions.setLabel(id, label) } ?: return false
        loadDefinitions()
        return true
    }

    suspend fun deleteCustomField(id: String): Boolean {
        problem = null
        outcome(Problem.FieldInUse) { fieldDefinitions.deleteCustom(id) } ?: return false
        loadDefinitions()
        return true
    }

    suspend fun loadTrash() {
        trashed = blocking { entities.listTrashed() } ?: return
    }

    suspend fun quickAdd(input: QuickAddInput): QuickAddResult {
        problem = null
        if (!QuickAdd.canSave(input)) return QuickAddResult.Invalid
        val created = outcome { entities.createPerson(input.name) } ?: return QuickAddResult.Failed
        var allSaved = true
        for (value in QuickAdd.values(input)) {
            if (outcome { fields.add(created.id, value) } == null) allSaved = false
        }
        loadPeople()
        if (allSaved) return QuickAddResult.Saved(created.id)
        problem = Problem.PartiallySaved
        return QuickAddResult.Partial(created.id)
    }

    /** Adds a value, or replaces it when [valueId] is given. Returns true when it was stored. */
    suspend fun saveField(
        entityId: String,
        valueId: String?,
        input: FieldValueInput,
    ): Boolean {
        problem = null
        val saved = outcome { if (valueId == null) fields.add(entityId, input) else fields.replace(valueId, input) }
        if (saved != null) loadProfile(entityId)
        return saved != null
    }

    /** Marks a value as the primary one of its field; the repository clears the mark on the others. */
    suspend fun makePrimary(
        entityId: String,
        row: ProfileRow,
    ): Boolean = saveField(entityId, row.valueId, row.toInput(isPrimary = true))

    suspend fun deleteField(
        entityId: String,
        valueId: String,
    ): Boolean {
        problem = null
        val done = outcome { fields.delete(valueId) }
        if (done != null) loadProfile(entityId)
        return done != null
    }

    suspend fun rename(
        entityId: String,
        name: String,
    ): Boolean {
        problem = null
        val renamed = outcome { entities.rename(entityId, name) } ?: return false
        profile = profile?.takeIf { it.entity.id == renamed.id }?.copy(entity = renamed) ?: profile
        loadPeople()
        return true
    }

    suspend fun setFavorite(
        entityId: String,
        favorite: Boolean,
    ) {
        problem = null
        val updated = outcome { entities.setFavorite(entityId, favorite) } ?: return
        profile = profile?.takeIf { it.entity.id == updated.id }?.copy(entity = updated) ?: profile
        loadPeople()
    }

    suspend fun moveToTrash(entityId: String): Boolean {
        problem = null
        outcome { entities.moveToTrash(entityId) } ?: return false
        profile = null
        loadPeople()
        return true
    }

    suspend fun restore(entityId: String): Boolean {
        problem = null
        outcome { entities.restore(entityId) } ?: return false
        loadTrash()
        loadPeople()
        return true
    }

    /** Deletes a trashed person for good. The screen asks for confirmation before calling this. */
    suspend fun purge(entityId: String): Boolean {
        problem = null
        outcome { entities.purge(entityId) } ?: return false
        loadTrash()
        return true
    }

    /** Runs a blocking call on the worker. Returns null and sets [Problem.Storage] if it throws. */
    private suspend fun <T> blocking(block: () -> T): T? =
        try {
            withContext(worker + NonCancellable) { block() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            problem = Problem.Storage
            null
        }

    /** Like [blocking] for repository calls that return an [Outcome]: a failure sets [problem] and returns null. */
    private suspend fun <T> outcome(
        conflict: Problem = Problem.Conflict,
        block: () -> Outcome<T>,
    ): T? =
        when (val result = blocking(block)) {
            null -> null
            is Outcome.Success -> result.value
            is Outcome.Failure -> {
                problem = result.reason.toProblem(conflict)
                null
            }
        }

    private fun FailureReason.toProblem(conflict: Problem): Problem =
        when (this) {
            FailureReason.NotFound -> Problem.NotFound
            FailureReason.InvalidInput, FailureReason.SensitivityLowered -> Problem.InvalidInput
            FailureReason.NotAllowed -> Problem.NotAllowed
            FailureReason.Conflict -> conflict
        }
}
