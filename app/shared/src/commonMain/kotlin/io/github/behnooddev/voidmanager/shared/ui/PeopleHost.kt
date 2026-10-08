package io.github.behnooddev.voidmanager.shared.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import io.github.behnooddev.voidmanager.shared.BackDispatcher
import io.github.behnooddev.voidmanager.shared.people.PeopleModel
import io.github.behnooddev.voidmanager.shared.people.PeopleRoute

/**
 * Hosts the People tab, or the Personal tab when [personal] is set. The current route is kept as a
 * string that survives a screen rotation. People start at the list; Personal starts at the
 * profile of the Personal entity once it has been loaded.
 */
@Composable
internal fun PeopleHost(
    model: PeopleModel,
    personal: Boolean,
    back: BackDispatcher,
    onLock: () -> Unit,
    onActivity: () -> Unit,
) {
    var routeKey by rememberSaveable(key = if (personal) "route-personal" else "route-people") { mutableStateOf("") }
    LaunchedEffect(personal, model) { if (personal) model.loadSelf() }

    val root: PeopleRoute? = if (personal) model.selfId?.let { PeopleRoute.Profile(it) } else PeopleRoute.Home
    val route = PeopleRoute.parse(routeKey) ?: root

    fun navigate(target: PeopleRoute) {
        routeKey = if (target == root) "" else target.encode()
    }

    fun goBack() {
        when (val current = route) {
            is PeopleRoute.Rename -> navigate(PeopleRoute.Profile(current.entityId))
            is PeopleRoute.FieldEditor -> navigate(PeopleRoute.Profile(current.entityId))
            else -> routeKey = ""
        }
    }

    val canGoBack = route != null && route != root
    DisposableEffect(canGoBack, route) {
        val handler: () -> Boolean = {
            goBack()
            true
        }
        if (canGoBack) back.handler = handler
        onDispose { if (back.handler === handler) back.handler = null }
    }

    when (route) {
        null -> Unit
        PeopleRoute.Home ->
            PeopleListScreen(
                model = model,
                onOpen = { navigate(PeopleRoute.Profile(it)) },
                onAdd = { navigate(PeopleRoute.QuickAdd) },
                onTrash = { navigate(PeopleRoute.Trash) },
                onLock = onLock,
                onActivity = onActivity,
            )
        PeopleRoute.QuickAdd ->
            QuickAddScreen(
                model,
                onSaved = { navigate(PeopleRoute.Profile(it)) },
                onBack = ::goBack,
                onActivity = onActivity,
            )
        PeopleRoute.Trash -> TrashScreen(model, onBack = ::goBack)
        is PeopleRoute.Profile ->
            ProfileScreen(
                model = model,
                entityId = route.entityId,
                isSelf = personal,
                onBack = if (route == root) null else ::goBack,
                onRename = { navigate(PeopleRoute.Rename(route.entityId)) },
                onAddField = { navigate(PeopleRoute.FieldEditor(route.entityId, null)) },
                onEditField = { navigate(PeopleRoute.FieldEditor(route.entityId, it)) },
                onTrashed = ::goBack,
                onLock = onLock,
            )
        is PeopleRoute.Rename -> RenameScreen(model, route.entityId, onDone = ::goBack, onActivity = onActivity)
        is PeopleRoute.FieldEditor ->
            FieldEditorScreen(model, route.entityId, route.valueId, onDone = ::goBack, onActivity = onActivity)
    }
}
