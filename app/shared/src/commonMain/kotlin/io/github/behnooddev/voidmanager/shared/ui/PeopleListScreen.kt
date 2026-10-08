package io.github.behnooddev.voidmanager.shared.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.github.behnooddev.voidmanager.core.designsystem.components.VmButton
import io.github.behnooddev.voidmanager.core.designsystem.components.VmButtonStyle
import io.github.behnooddev.voidmanager.core.designsystem.components.VmEmptyState
import io.github.behnooddev.voidmanager.core.designsystem.components.VmListItem
import io.github.behnooddev.voidmanager.core.designsystem.components.VmSectionHeader
import io.github.behnooddev.voidmanager.core.designsystem.components.VmTextField
import io.github.behnooddev.voidmanager.core.designsystem.components.VmTopBar
import io.github.behnooddev.voidmanager.core.designsystem.theme.VmSpacing
import io.github.behnooddev.voidmanager.core.designsystem.theme.VmTheme
import io.github.behnooddev.voidmanager.shared.people.PeopleModel
import io.github.behnooddev.voidmanager.shared.resources.Res
import io.github.behnooddev.voidmanager.shared.resources.action_lock
import io.github.behnooddev.voidmanager.shared.resources.dismiss
import io.github.behnooddev.voidmanager.shared.resources.nav_people
import io.github.behnooddev.voidmanager.shared.resources.people_add
import io.github.behnooddev.voidmanager.shared.resources.people_all
import io.github.behnooddev.voidmanager.shared.resources.people_empty_message
import io.github.behnooddev.voidmanager.shared.resources.people_empty_title
import io.github.behnooddev.voidmanager.shared.resources.people_favorites
import io.github.behnooddev.voidmanager.shared.resources.people_no_results
import io.github.behnooddev.voidmanager.shared.resources.people_search
import io.github.behnooddev.voidmanager.shared.resources.people_trash
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun PeopleListScreen(
    model: PeopleModel,
    onOpen: (String) -> Unit,
    onAdd: () -> Unit,
    onTrash: () -> Unit,
    onLock: () -> Unit,
    onActivity: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf(model.query) }
    LaunchedEffect(model) { model.loadPeople() }

    Column(modifier = Modifier.fillMaxSize()) {
        VmTopBar(
            title = stringResource(Res.string.nav_people),
            actions = {
                VmButton(text = stringResource(Res.string.people_trash), onClick = onTrash, style = VmButtonStyle.Text)
                VmButton(text = stringResource(Res.string.action_lock), onClick = onLock, style = VmButtonStyle.Text)
            },
        )
        model.problem?.let { problem ->
            Column(modifier = Modifier.padding(horizontal = VmSpacing.lg)) {
                StatusText(
                    problemText(problem),
                    VmTheme.colors.danger,
                )
                VmButton(
                    text = stringResource(Res.string.dismiss),
                    onClick = model::clearProblem,
                    style = VmButtonStyle.Text,
                )
            }
        }
        Column(
            modifier = Modifier.padding(horizontal = VmSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(VmSpacing.md),
        ) {
            VmTextField(
                value = text,
                onValueChange = {
                    text = it
                    onActivity()
                    scope.launch { model.setQuery(it) }
                },
                label = stringResource(Res.string.people_search),
            )
            VmButton(
                text = stringResource(Res.string.people_add),
                onClick = onAdd,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        val people = model.people
        val searching = text.isNotBlank()
        when {
            people.isEmpty() && !searching -> {
                VmEmptyState(
                    title = stringResource(Res.string.people_empty_title),
                    message = stringResource(Res.string.people_empty_message),
                )
            }
            people.isEmpty() -> {
                BodyText(stringResource(Res.string.people_no_results), modifier = Modifier.padding(VmSpacing.lg))
            }
            else -> {
                val favorites = if (searching) emptyList() else people.filter { it.isFavorite }
                val others = if (searching) people else people.filterNot { it.isFavorite }
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    if (favorites.isNotEmpty()) {
                        item(key = "header-favorites") { VmSectionHeader(stringResource(Res.string.people_favorites)) }
                        items(
                            favorites,
                            key = { it.id },
                        ) { person -> VmListItem(title = person.displayName, onClick = { onOpen(person.id) }) }
                    }
                    if (others.isNotEmpty()) {
                        if (favorites.isNotEmpty()) {
                            item(
                                key = "header-all",
                            ) { VmSectionHeader(stringResource(Res.string.people_all)) }
                        }
                        items(
                            others,
                            key = { it.id },
                        ) { person -> VmListItem(title = person.displayName, onClick = { onOpen(person.id) }) }
                    }
                }
            }
        }
    }
}
