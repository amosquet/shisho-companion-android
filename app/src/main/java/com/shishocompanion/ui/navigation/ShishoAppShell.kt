package com.shishocompanion.ui.navigation

import android.content.ContextWrapper
import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Resources
import android.text.format.DateFormat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.shishocompanion.R
import com.shishocompanion.navigation.*
import com.shishocompanion.ui.*
import com.shishocompanion.ui.theme.ShishoCompanionTheme
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.Locale

private val ROUTE_READING_LIST = ReadingList::class.qualifiedName!!
private val ROUTE_NOTES = Notes::class.qualifiedName!!
private val ROUTE_REMINDERS = Reminders::class.qualifiedName!!
private val MAIN_TABS = listOf(ROUTE_READING_LIST, ROUTE_NOTES, ROUTE_REMINDERS)

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun ShishoAppShell(
    sharedPrefs: SharedPreferences,
    intentAction: MutableStateFlow<String?>
) {
    val themeViewModel: ThemeViewModel = hiltViewModel()
    val themePreference by themeViewModel.themePreference.collectAsState()
    val dynamicColorPreference by themeViewModel.dynamicColorPreference.collectAsState()
    val timeFormatPreference by themeViewModel.timeFormatPreference.collectAsState()
    val dateFormatPreference by themeViewModel.dateFormatPreference.collectAsState()
    val languagePreference by themeViewModel.languagePreference.collectAsState()

    val context = LocalContext.current
    val use24HourTime = when (timeFormatPreference) {
        "24_HOUR" -> true
        "12_HOUR" -> false
        else -> DateFormat.is24HourFormat(context)
    }

    val locale = if (languagePreference == "SYSTEM") {
        Locale.getDefault()
    } else {
        Locale.forLanguageTag(languagePreference)
    }
    val configuration = LocalConfiguration.current
    val updatedContext = remember(context, locale, configuration) {
        val config = Configuration(configuration)
        config.setLocale(locale)
        val newContext = context.createConfigurationContext(config)
        object : ContextWrapper(context) {
            override fun getResources(): Resources = newContext.resources
        }
    }

    CompositionLocalProvider(
        LocalContext provides updatedContext,
        LocalConfiguration provides updatedContext.resources.configuration
    ) {
        ShishoCompanionTheme(
            themePreference = themePreference,
            dynamicColor = dynamicColorPreference
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                val navController = rememberNavController()
                val loginViewModel: LoginViewModel = hiltViewModel()
                val listViewModel: ReadingListViewModel = hiltViewModel()
                val notesViewModel: NotesViewModel = hiltViewModel()
                val remindersViewModel: RemindersViewModel = hiltViewModel()
                val announcementsViewModel: AnnouncementsViewModel = hiltViewModel()

                val listUiState by listViewModel.uiState.collectAsState()
                val hasUnreadAnnouncements by announcementsViewModel.hasUnreadAnnouncement.collectAsState()
                val announcements by announcementsViewModel.announcements.collectAsState()

                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination?.route

                val bookList = (listUiState as? ReadingListUiState.Success)?.items ?: emptyList()
                val bookTitleLookup = remember(bookList) { bookList.associate { it.id to it.title } }

                val currentIntentAction by intentAction.collectAsState()
                LaunchedEffect(currentIntentAction, currentDestination) {
                    val action = currentIntentAction
                    if (action != null && currentDestination != null && currentDestination != Login::class.qualifiedName) {
                        when (action) {
                            "com.shishocompanion.ADD_NOTE" -> {
                                navController.navigate(Notes) {
                                    popUpTo<ReadingList> { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                                navController.navigate(AddNote())
                                intentAction.value = null
                            }
                            "com.shishocompanion.ADD_BOOK" -> {
                                navController.navigate(ReadingList) {
                                    popUpTo<ReadingList> { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                                navController.navigate(Add)
                                intentAction.value = null
                            }
                            "com.shishocompanion.ADD_REMINDER" -> {
                                navController.navigate(Reminders) {
                                    popUpTo<ReadingList> { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                                navController.navigate(AddReminder())
                                intentAction.value = null
                            }
                        }
                    }
                }

                val startDestination: Any = remember {
                    if (loginViewModel.hasActiveOrGuestSession()) Notes else Login
                }
                LaunchedEffect(Unit) {
                    if (loginViewModel.hasActiveOrGuestSession()) {
                        listViewModel.syncReadingList()
                        notesViewModel.syncNotes()
                        remindersViewModel.syncReminders()
                    }
                }

                val showSessionExpired by loginViewModel.showSessionExpiredDialog.collectAsState()
                if (showSessionExpired) {
                    AlertDialog(
                        onDismissRequest = { loginViewModel.dismissSessionExpiredDialog() },
                        title = { Text(stringResource(R.string.app_name)) },
                        text = { Text(stringResource(R.string.session_expired_message)) },
                        confirmButton = {
                            TextButton(onClick = {
                                loginViewModel.dismissSessionExpiredDialog()
                                loginViewModel.prepareReLogin()
                                navController.navigate(Login) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }) {
                                Text(stringResource(R.string.log_in))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { loginViewModel.dismissSessionExpiredDialog() }) {
                                Text(stringResource(R.string.later))
                            }
                        }
                    )
                }

                NavigationSuiteScaffold(
                    navigationSuiteItems = {
                        if (currentDestination in MAIN_TABS) {
                            item(
                                selected = currentDestination == ROUTE_READING_LIST,
                                onClick = {
                                    navController.navigate(ReadingList) {
                                        popUpTo<ReadingList> { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = stringResource(R.string.reading_list)) },
                                label = { Text(stringResource(R.string.list_label)) }
                            )
                            item(
                                selected = currentDestination == ROUTE_NOTES,
                                onClick = {
                                    navController.navigate(Notes) {
                                        popUpTo<ReadingList> { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.notes)) },
                                label = { Text(stringResource(R.string.notes)) }
                            )
                            item(
                                selected = currentDestination == ROUTE_REMINDERS,
                                onClick = {
                                    navController.navigate(Reminders) {
                                        popUpTo<ReadingList> { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(Icons.Filled.Notifications, contentDescription = stringResource(R.string.reminders)) },
                                label = { Text(stringResource(R.string.reminders)) }
                            )
                        }
                    },
                    layoutType = if (currentDestination in MAIN_TABS) {
                        NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(currentWindowAdaptiveInfo())
                    } else {
                        NavigationSuiteType.None
                    }
                ) {
                    Scaffold(
                        floatingActionButton = {
                            if (currentDestination in MAIN_TABS) {
                                var expanded by remember { mutableStateOf(false) }
                                Box {
                                    FloatingActionButton(
                                        onClick = { expanded = true },
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ) {
                                        Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_item))
                                    }
                                    DropdownMenu(
                                        expanded = expanded,
                                        onDismissRequest = { expanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.add_book)) },
                                            onClick = {
                                                expanded = false
                                                navController.navigate(Add)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.add_note)) },
                                            onClick = {
                                                expanded = false
                                                navController.navigate(AddNote())
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.add_reminder)) },
                                            onClick = {
                                                expanded = false
                                                navController.navigate(AddReminder())
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    ) { innerPadding ->
                        ShishoNavHost(
                            navController = navController,
                            startDestination = startDestination,
                            modifier = Modifier
                                .padding(innerPadding)
                                .consumeWindowInsets(innerPadding),
                            sharedPrefs = sharedPrefs,
                            intentAction = intentAction,
                            loginViewModel = loginViewModel,
                            listViewModel = listViewModel,
                            notesViewModel = notesViewModel,
                            remindersViewModel = remindersViewModel,
                            themeViewModel = themeViewModel,
                            announcementsViewModel = announcementsViewModel,
                            use24HourTime = use24HourTime,
                            dateFormatPreference = dateFormatPreference,
                            themePreference = themePreference,
                            dynamicColorPreference = dynamicColorPreference,
                            timeFormatPreference = timeFormatPreference,
                            languagePreference = languagePreference,
                            hasUnreadAnnouncements = hasUnreadAnnouncements,
                            announcements = announcements,
                            bookList = bookList,
                            bookTitleLookup = bookTitleLookup
                        )
                    }
                }
            }
        }
    }
}
