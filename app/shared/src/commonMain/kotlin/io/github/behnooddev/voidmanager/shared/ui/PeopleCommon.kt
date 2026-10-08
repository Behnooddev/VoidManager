package io.github.behnooddev.voidmanager.shared.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.behnooddev.voidmanager.core.designsystem.components.VmButton
import io.github.behnooddev.voidmanager.core.designsystem.components.VmButtonStyle
import io.github.behnooddev.voidmanager.core.designsystem.components.VmTopBar
import io.github.behnooddev.voidmanager.core.designsystem.theme.VmSpacing
import io.github.behnooddev.voidmanager.core.designsystem.theme.VmTheme
import io.github.behnooddev.voidmanager.core.model.FieldSection
import io.github.behnooddev.voidmanager.shared.people.FieldIssue
import io.github.behnooddev.voidmanager.shared.people.PeopleModel
import io.github.behnooddev.voidmanager.shared.people.Problem
import io.github.behnooddev.voidmanager.shared.resources.Res
import io.github.behnooddev.voidmanager.shared.resources.action_back
import io.github.behnooddev.voidmanager.shared.resources.dismiss
import io.github.behnooddev.voidmanager.shared.resources.field_issue_date
import io.github.behnooddev.voidmanager.shared.resources.field_issue_email
import io.github.behnooddev.voidmanager.shared.resources.field_issue_number
import io.github.behnooddev.voidmanager.shared.resources.field_issue_phone
import io.github.behnooddev.voidmanager.shared.resources.field_issue_url
import io.github.behnooddev.voidmanager.shared.resources.problem_conflict
import io.github.behnooddev.voidmanager.shared.resources.problem_invalid
import io.github.behnooddev.voidmanager.shared.resources.problem_not_allowed
import io.github.behnooddev.voidmanager.shared.resources.problem_not_found
import io.github.behnooddev.voidmanager.shared.resources.problem_partial
import io.github.behnooddev.voidmanager.shared.resources.problem_storage
import io.github.behnooddev.voidmanager.shared.resources.section_accounts
import io.github.behnooddev.voidmanager.shared.resources.section_address
import io.github.behnooddev.voidmanager.shared.resources.section_contact
import io.github.behnooddev.voidmanager.shared.resources.section_custom
import io.github.behnooddev.voidmanager.shared.resources.section_dates
import io.github.behnooddev.voidmanager.shared.resources.section_education
import io.github.behnooddev.voidmanager.shared.resources.section_financial
import io.github.behnooddev.voidmanager.shared.resources.section_identity
import io.github.behnooddev.voidmanager.shared.resources.section_notes
import io.github.behnooddev.voidmanager.shared.resources.section_social
import io.github.behnooddev.voidmanager.shared.resources.section_work
import org.jetbrains.compose.resources.stringResource

/**
 * The frame of every People and Personal screen: a top bar with an optional Back button, a
 * dismissable message when something went wrong, and a scrolling column for the content.
 */
@Composable
internal fun ScreenFrame(
    model: PeopleModel,
    title: String,
    onBack: (() -> Unit)?,
    actions: @Composable RowScope.() -> Unit = {},
    scrollable: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        VmTopBar(
            title = title,
            leading = {
                if (onBack != null) {
                    VmButton(
                        text = stringResource(Res.string.action_back),
                        onClick = onBack,
                        style = VmButtonStyle.Text,
                    )
                }
            },
            actions = actions,
        )
        val problem = model.problem
        if (problem != null) {
            Column(
                modifier = Modifier.padding(horizontal = VmSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(VmSpacing.xs),
            ) {
                StatusText(problemText(problem), VmTheme.colors.danger)
                VmButton(
                    text = stringResource(Res.string.dismiss),
                    onClick = model::clearProblem,
                    style = VmButtonStyle.Text,
                )
            }
        }
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .let { if (scrollable) it.verticalScroll(rememberScrollState()) else it }
                    .padding(VmSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(VmSpacing.md),
            content = content,
        )
    }
}

@Composable
internal fun problemText(problem: Problem): String =
    stringResource(
        when (problem) {
            Problem.NotFound -> Res.string.problem_not_found
            Problem.InvalidInput -> Res.string.problem_invalid
            Problem.Conflict -> Res.string.problem_conflict
            Problem.NotAllowed -> Res.string.problem_not_allowed
            Problem.Storage -> Res.string.problem_storage
            Problem.PartiallySaved -> Res.string.problem_partial
        },
    )

@Composable
internal fun fieldIssueText(issue: FieldIssue): String? =
    when (issue) {
        FieldIssue.Empty -> null
        FieldIssue.InvalidPhone -> stringResource(Res.string.field_issue_phone)
        FieldIssue.InvalidEmail -> stringResource(Res.string.field_issue_email)
        FieldIssue.InvalidNumber -> stringResource(Res.string.field_issue_number)
        FieldIssue.InvalidDate -> stringResource(Res.string.field_issue_date)
        FieldIssue.InvalidUrl -> stringResource(Res.string.field_issue_url)
    }

@Composable
internal fun sectionText(section: FieldSection): String =
    stringResource(
        when (section) {
            FieldSection.Identity -> Res.string.section_identity
            FieldSection.Contact -> Res.string.section_contact
            FieldSection.Social -> Res.string.section_social
            FieldSection.Accounts -> Res.string.section_accounts
            FieldSection.Financial -> Res.string.section_financial
            FieldSection.Address -> Res.string.section_address
            FieldSection.Work -> Res.string.section_work
            FieldSection.Education -> Res.string.section_education
            FieldSection.Dates -> Res.string.section_dates
            FieldSection.Notes -> Res.string.section_notes
            FieldSection.Custom -> Res.string.section_custom
        },
    )
