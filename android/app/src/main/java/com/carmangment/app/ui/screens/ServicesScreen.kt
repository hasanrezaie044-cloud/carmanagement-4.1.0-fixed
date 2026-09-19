package com.carmangment.app.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import com.carmangment.app.core.jalali.Jalali
import com.carmangment.app.data.repo.AppRepository
import com.carmangment.app.ui.components.ServiceTutorialDialog

/**
 * The Services tab.
 *
 * It used to be a searchable list with a type filter, and registration happened in a
 * floating sheet on top of it. Both are gone: the search box duplicated what the
 * calendar already does better, and the floating registration was the cramped,
 * keyboard-fighting UX. The tab is now exactly one thing — a dedicated, full-screen
 * "register a service" form that resets itself after each save so several services can
 * be entered in a row.
 *
 * Browsing, editing and deleting existing services live in the Calendar (pick a day,
 * tap a service) and in Reports, both of which open the same full-screen form.
 *
 * The very first time this tab is opened on a given install, a short walkthrough
 * explains the form (and specifically that "ساعت کارکرد" is always typed in by hand).
 * The flag lives in AppSettings and is never shown again afterwards.
 */
@Composable
fun ServicesScreen(repository: AppRepository) {
    val settings by repository.settings.collectAsState(initial = repository.settingsNow)
    var showTutorial by remember(settings.hasSeenServiceTutorial) {
        mutableStateOf(!settings.hasSeenServiceTutorial)
    }

    if (showTutorial) {
        ServiceTutorialDialog(
            onDismiss = {
                showTutorial = false
                repository.markServiceTutorialSeen()
            },
        )
    }

    ServiceFormScreen(
        repository = repository,
        initialDate = Jalali.todayString(),
        editing = null,
        onDone = null,
    )
}
