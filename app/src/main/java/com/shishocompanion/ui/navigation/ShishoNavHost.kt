package com.shishocompanion.ui.navigation

import android.content.SharedPreferences
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.shishocompanion.R
import com.shishocompanion.data.Announcement
import com.shishocompanion.data.CreateNoteRequest
import com.shishocompanion.data.ReadingListItem
import com.shishocompanion.navigation.*
import com.shishocompanion.ui.*
import com.shishocompanion.ui.components.AdaptiveListDetailPane
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

private val ROUTE_READING_LIST = ReadingList::class.qualifiedName!!
private val ROUTE_NOTES = Notes::class.qualifiedName!!
private val ROUTE_REMINDERS = Reminders::class.qualifiedName!!

private fun getTabIndex(route: String?): Int {
    return when (route?.substringBefore("/")) {
        ROUTE_READING_LIST -> 0
        ROUTE_NOTES -> 1
        ROUTE_REMINDERS -> 2
        else -> -1
    }
}

@Composable
fun ShishoNavHost(
    navController: NavHostController,
    startDestination: Any,
    modifier: Modifier = Modifier,
    sharedPrefs: SharedPreferences,
    intentAction: MutableStateFlow<String?>,
    loginViewModel: LoginViewModel,
    listViewModel: ReadingListViewModel,
    notesViewModel: NotesViewModel,
    remindersViewModel: RemindersViewModel,
    themeViewModel: ThemeViewModel,
    announcementsViewModel: AnnouncementsViewModel,
    use24HourTime: Boolean,
    dateFormatPreference: String,
    themePreference: String,
    dynamicColorPreference: Boolean,
    timeFormatPreference: String,
    languagePreference: String,
    hasUnreadAnnouncements: Boolean,
    announcements: List<Announcement>,
    bookList: List<ReadingListItem>,
    bookTitleLookup: Map<String, String>
) {
    val context = LocalContext.current
    val loginUiState by loginViewModel.uiState.collectAsState()
    val listUiState by listViewModel.uiState.collectAsState()
    val notesUiState by notesViewModel.uiState.collectAsState()
    val remindersUiState by remindersViewModel.uiState.collectAsState()

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = {
            val initialIndex = getTabIndex(initialState.destination.route)
            val targetIndex = getTabIndex(targetState.destination.route)
            if (initialIndex != -1 && targetIndex != -1) {
                val direction = if (initialIndex > targetIndex) -1 else 1
                slideInHorizontally(initialOffsetX = { direction * it }, animationSpec = tween(200))
            } else {
                slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(200))
            }
        },
        exitTransition = {
            val initialIndex = getTabIndex(initialState.destination.route)
            val targetIndex = getTabIndex(targetState.destination.route)
            if (initialIndex != -1 && targetIndex != -1) {
                val direction = if (initialIndex > targetIndex) 1 else -1
                slideOutHorizontally(targetOffsetX = { direction * it / 3 }, animationSpec = tween(200))
            } else {
                slideOutHorizontally(targetOffsetX = { -it / 3 }, animationSpec = tween(200))
            }
        },
        popEnterTransition = {
            val initialIndex = getTabIndex(initialState.destination.route)
            val targetIndex = getTabIndex(targetState.destination.route)
            if (initialIndex != -1 && targetIndex != -1) {
                val direction = if (initialIndex > targetIndex) -1 else 1
                slideInHorizontally(initialOffsetX = { direction * it }, animationSpec = tween(200))
            } else {
                slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = tween(200))
            }
        },
        popExitTransition = {
            val initialIndex = getTabIndex(initialState.destination.route)
            val targetIndex = getTabIndex(targetState.destination.route)
            if (initialIndex != -1 && targetIndex != -1) {
                val direction = if (initialIndex > targetIndex) 1 else -1
                slideOutHorizontally(targetOffsetX = { direction * it / 3 }, animationSpec = tween(200))
            } else {
                slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(200))
            }
        }
    ) {
        composable<Login> {
            LoginScreen(
                uiState = loginUiState,
                onLogin = { email, pin -> loginViewModel.login(email, pin) },
                onRegister = { email, pin -> loginViewModel.register(email, pin) },
                onContinueAsGuest = { loginViewModel.continueAsGuest() },
                onNavigateToList = {
                    themeViewModel.reloadPreferences()
                    navController.navigate(Notes) {
                        popUpTo<Login> { inclusive = true }
                    }
                    when (intentAction.value) {
                        "com.shishocompanion.ADD_NOTE" -> {
                            intentAction.value = null
                            navController.navigate(AddNote())
                        }
                        "com.shishocompanion.ADD_BOOK" -> {
                            intentAction.value = null
                            navController.navigate(Add)
                        }
                        "com.shishocompanion.ADD_REMINDER" -> {
                            intentAction.value = null
                            navController.navigate(AddReminder())
                        }
                    }
                    listViewModel.syncReadingList()
                    notesViewModel.syncNotes()
                    remindersViewModel.syncReminders()
                }
            )
        }
        composable<ReadingList> {
            val splitScreenPreference by themeViewModel.splitScreenPreference.collectAsState()
            val listSnackbarHostState = remember { SnackbarHostState() }
            val coroutineScope = rememberCoroutineScope()
            AdaptiveListDetailPane<String>(
                splitScreen = splitScreenPreference,
                listPane = { onItemClick ->
                    ReadingListScreen(
                        uiState = listUiState,
                        hasUnreadAnnouncements = hasUnreadAnnouncements,
                        onSyncClick = { listViewModel.syncReadingList() },
                        onItemClick = onItemClick,
                        onSettingsClick = { navController.navigate(Settings) },
                        snackbarHostState = listSnackbarHostState
                    )
                },
                detailPane = { selectedId, isDualPane, onNavigateBack ->
                    val item = (listUiState as? ReadingListUiState.Success)?.items?.find { it.id == selectedId }
                        ?: listViewModel.getItemById(selectedId)
                    if (item != null) {
                        val linkedNotes = (notesUiState as? NotesUiState.Success)?.items?.filter { it.book == selectedId } ?: emptyList()
                        val linkedReminders = remindersUiState.reminders.filter { it.book == selectedId }
                        BookDetailScreen(
                            item = item,
                            onEditClick = { navController.navigate(Edit(selectedId)) },
                            onDeleteClick = {
                                val deletedBook = item
                                listViewModel.deleteItem(selectedId)
                                onNavigateBack()
                                coroutineScope.launch {
                                    val res = listSnackbarHostState.showSnackbar(
                                        message = context.getString(R.string.deleted_book_message, deletedBook.title),
                                        actionLabel = context.getString(R.string.undo),
                                        duration = SnackbarDuration.Short
                                    )
                                    if (res == SnackbarResult.ActionPerformed) {
                                        listViewModel.restoreItem(deletedBook)
                                    }
                                }
                            },
                            onNavigateBack = onNavigateBack,
                            isSplitScreen = isDualPane,
                            dateFormatPreference = dateFormatPreference,
                            linkedNotes = linkedNotes,
                            linkedReminders = linkedReminders,
                            onAddNoteClick = { navController.navigate(AddNote(bookId = selectedId)) },
                            onAddReminderClick = {
                                navController.navigate(
                                    AddReminder(bookId = selectedId, defaultText = "Read ${item.title}")
                                )
                            },
                            onQuickUpdate = { req -> listViewModel.updateItem(selectedId, req) }
                        )
                    } else {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.list_label))
                        }
                    }
                },
                placeholderPane = {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.list_label))
                    }
                }
            )
        }
        composable<Notes> {
            val splitScreenPreference by themeViewModel.splitScreenPreference.collectAsState()
            val swipeActionPreference by themeViewModel.swipeActionPreference.collectAsState()
            val selectedNoteIds by notesViewModel.selectedNoteIds.collectAsState()
            val notesSnackbarHostState = remember { SnackbarHostState() }
            val notesCoroutineScope = rememberCoroutineScope()
            AdaptiveListDetailPane<String>(
                splitScreen = splitScreenPreference,
                listPane = { onItemClick ->
                    NotesListScreen(
                        uiState = notesUiState,
                        hasUnreadAnnouncements = hasUnreadAnnouncements,
                        selectedNoteIds = selectedNoteIds,
                        onSyncClick = { notesViewModel.syncNotes() },
                        onItemClick = onItemClick,
                        onSettingsClick = { navController.navigate(Settings) },
                        onArchiveClick = {
                            notesViewModel.clearNoteSelection()
                            navController.navigate(ArchivedNotes)
                        },
                        onToggleSelection = { id -> notesViewModel.toggleNoteSelection(id) },
                        onClearSelection = { notesViewModel.clearNoteSelection() },
                        onDeleteSelected = { notesViewModel.deleteSelectedNotes() },
                        onArchiveSelected = { notesViewModel.archiveSelectedNotes() },
                        onDeleteNote = { notesViewModel.deleteNote(it) },
                        onArchiveNote = { notesViewModel.archiveNote(it) },
                        onRestoreNote = { notesViewModel.restoreNote(it) },
                        bookTitleLookup = bookTitleLookup,
                        dateFormatPreference = dateFormatPreference,
                        swipeActionPreference = swipeActionPreference,
                        snackbarHostState = notesSnackbarHostState
                    )
                },
                detailPane = { selectedId, isDualPane, onNavigateBack ->
                    val item = (notesUiState as? NotesUiState.Success)?.items?.find { it.id == selectedId }
                        ?: notesViewModel.getNoteById(selectedId)
                    if (item != null) {
                        NoteDetailScreen(
                            item = item,
                            onEditClick = { navController.navigate(EditNote(selectedId)) },
                            onDeleteClick = {
                                val deletedNote = item
                                notesViewModel.deleteNote(selectedId)
                                onNavigateBack()
                                notesCoroutineScope.launch {
                                    val res = notesSnackbarHostState.showSnackbar(
                                        message = context.getString(R.string.deleted_note_message, deletedNote.title),
                                        actionLabel = context.getString(R.string.undo),
                                        duration = SnackbarDuration.Short
                                    )
                                    if (res == SnackbarResult.ActionPerformed) {
                                        notesViewModel.restoreNote(deletedNote)
                                    }
                                }
                            },
                            onArchiveClick = {
                                notesViewModel.archiveNote(selectedId)
                                onNavigateBack()
                            },
                            onTextUpdate = { newText ->
                                notesViewModel.updateNote(
                                    selectedId,
                                    CreateNoteRequest(
                                        owner = item.owner,
                                        title = item.title,
                                        text = newText,
                                        attachment = item.attachment,
                                        archived = item.archived,
                                        book = item.book
                                    )
                                )
                            },
                            onNavigateBack = onNavigateBack,
                            isSplitScreen = isDualPane,
                            dateFormatPreference = dateFormatPreference,
                            authToken = sharedPrefs.getString("TOKEN", null),
                            linkedBookTitle = bookTitleLookup[item.book]
                        )
                    } else {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.notes))
                        }
                    }
                },
                placeholderPane = {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.notes))
                    }
                }
            )
        }
        composable<Add> {
            AddReadingListItemScreen(
                onSave = { req -> listViewModel.addItem(req) },
                onNavigateBack = { navController.popBackStack() },
                onSearchBooks = { title, author, isbn, onResult ->
                    listViewModel.searchGoogleBooks(title, author, isbn, onResult)
                }
            )
        }
        composable<Edit> { backStackEntry ->
            val route = backStackEntry.toRoute<Edit>()
            val id = route.id
            val item = (listUiState as? ReadingListUiState.Success)?.items?.find { it.id == id }
                ?: listViewModel.getItemById(id)
            if (item != null) {
                EditReadingListItemScreen(
                    item = item,
                    onSave = { req -> listViewModel.updateItem(id, req) },
                    onNavigateBack = { navController.popBackStack() },
                    onSearchBooks = { title, author, isbn, onResult ->
                        listViewModel.searchGoogleBooks(title, author, isbn, onResult)
                    }
                )
            }
        }
        composable<AddNote> { backStackEntry ->
            val route = backStackEntry.toRoute<AddNote>()
            val userId = sharedPrefs.getString("USER_ID", "") ?: ""
            AddNoteScreen(
                owner = userId,
                onSave = { req -> notesViewModel.addNote(req) },
                onNavigateBack = { navController.popBackStack() },
                books = bookList,
                initialBookId = route.bookId
            )
        }
        composable<EditNote> { backStackEntry ->
            val route = backStackEntry.toRoute<EditNote>()
            val id = route.id
            val item = (notesUiState as? NotesUiState.Success)?.items?.find { it.id == id }
                ?: notesViewModel.getNoteById(id)
            if (item != null) {
                EditNoteScreen(
                    item = item,
                    onSave = { req -> notesViewModel.updateNote(id, req) },
                    onNavigateBack = { navController.popBackStack() },
                    books = bookList
                )
            }
        }
        composable<Settings> {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val discordLinkUiState by settingsViewModel.uiState.collectAsState()
            val linkedDiscordId by settingsViewModel.linkedDiscordId.collectAsState()
            val googleBooksApiKey by settingsViewModel.googleBooksApiKey.collectAsState()
            val swipeActionPreference by settingsViewModel.swipeActionPreference.collectAsState()
            val sendRemindersToDiscordPreference by themeViewModel.sendRemindersToDiscordPreference.collectAsState()
            val splitScreenPreference by themeViewModel.splitScreenPreference.collectAsState()

            LaunchedEffect(Unit) {
                announcementsViewModel.markAsRead()
            }

            SettingsScreen(
                userEmail = sharedPrefs.getString("USER_EMAIL", "") ?: "",
                themePreference = themePreference,
                onThemePreferenceChanged = { themeViewModel.setThemePreference(it) },
                dynamicColorPreference = dynamicColorPreference,
                onDynamicColorPreferenceChanged = { themeViewModel.setDynamicColorPreference(it) },
                timeFormatPreference = timeFormatPreference,
                onTimeFormatPreferenceChanged = { themeViewModel.setTimeFormatPreference(it) },
                dateFormatPreference = dateFormatPreference,
                onDateFormatPreferenceChanged = { themeViewModel.setDateFormatPreference(it) },
                languagePreference = languagePreference,
                onLanguagePreferenceChanged = { themeViewModel.setLanguagePreference(it) },
                googleBooksApiKey = googleBooksApiKey,
                onGoogleBooksApiKeyChanged = { settingsViewModel.updateGoogleBooksApiKey(it) },
                swipeActionPreference = swipeActionPreference,
                onSwipeActionPreferenceChanged = { settingsViewModel.updateSwipeActionPreference(it) },
                sendRemindersToDiscordPreference = sendRemindersToDiscordPreference,
                onSendRemindersToDiscordPreferenceChanged = { themeViewModel.setSendRemindersToDiscordPreference(it) },
                linkedDiscordId = linkedDiscordId,
                discordLinkUiState = discordLinkUiState,
                onRequestDiscordLink = { id -> settingsViewModel.requestDiscordPin(id) },
                onVerifyDiscordLink = { pin ->
                    settingsViewModel.verifyDiscordPin(
                        linkedDiscordId.ifBlank { (discordLinkUiState as? DiscordLinkUiState.PinRequested)?.discordId ?: "" },
                        pin
                    )
                },
                onUnlinkDiscord = { settingsViewModel.unlinkDiscord() },
                onResetDiscordLinkState = { settingsViewModel.resetState() },
                announcements = announcements,
                splitScreenPreference = splitScreenPreference,
                onSplitScreenPreferenceChanged = { themeViewModel.setSplitScreenPreference(it) },
                onLogout = {
                    loginViewModel.logout()
                    navController.navigate(Login) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateBack = {
                    settingsViewModel.resetState()
                    navController.popBackStack()
                }
            )
        }
        composable<Reminders> {
            val splitScreenPreference by themeViewModel.splitScreenPreference.collectAsState()
            val swipeActionPreference by themeViewModel.swipeActionPreference.collectAsState()
            val selectedReminderIds by remindersViewModel.selectedReminderIds.collectAsState()
            val remindersSnackbarHostState = remember { SnackbarHostState() }
            val remindersCoroutineScope = rememberCoroutineScope()
            AdaptiveListDetailPane<String>(
                splitScreen = splitScreenPreference,
                listPane = { onItemClick ->
                    RemindersListScreen(
                        uiState = remindersUiState,
                        hasUnreadAnnouncements = hasUnreadAnnouncements,
                        selectedReminderIds = selectedReminderIds,
                        use24HourTime = use24HourTime,
                        dateFormatPreference = dateFormatPreference,
                        onSyncClick = { remindersViewModel.syncReminders() },
                        onItemClick = onItemClick,
                        onSettingsClick = { navController.navigate(Settings) },
                        onArchiveClick = {
                            remindersViewModel.clearReminderSelection()
                            navController.navigate(ArchivedReminders)
                        },
                        onToggleSelection = { id -> remindersViewModel.toggleReminderSelection(id) },
                        onClearSelection = { remindersViewModel.clearReminderSelection() },
                        onDeleteSelected = { remindersViewModel.deleteSelectedReminders() },
                        onDeleteReminder = { remindersViewModel.deleteReminder(it) },
                        onArchiveReminder = { remindersViewModel.archiveReminder(it) },
                        onRestoreReminder = { remindersViewModel.restoreReminder(it) },
                        bookTitleLookup = bookTitleLookup,
                        swipeActionPreference = swipeActionPreference,
                        snackbarHostState = remindersSnackbarHostState
                    )
                },
                detailPane = { selectedId, isDualPane, onNavigateBack ->
                    val item = remindersUiState.reminders.find { it.id == selectedId }
                        ?: remindersViewModel.getReminderById(selectedId)
                    if (item != null) {
                        ReminderDetailScreen(
                            item = item,
                            use24HourTime = use24HourTime,
                            dateFormatPreference = dateFormatPreference,
                            onEditClick = { navController.navigate(EditReminder(selectedId)) },
                            onDeleteClick = {
                                val deletedReminder = item
                                remindersViewModel.deleteReminder(selectedId)
                                onNavigateBack()
                                remindersCoroutineScope.launch {
                                    val res = remindersSnackbarHostState.showSnackbar(
                                        message = context.getString(R.string.deleted_reminder_message),
                                        actionLabel = context.getString(R.string.undo),
                                        duration = SnackbarDuration.Short
                                    )
                                    if (res == SnackbarResult.ActionPerformed) {
                                        remindersViewModel.restoreReminder(deletedReminder)
                                    }
                                }
                            },
                            onNavigateBack = onNavigateBack,
                            isSplitScreen = isDualPane,
                            linkedBookTitle = bookTitleLookup[item.book]
                        )
                    } else {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.reminders))
                        }
                    }
                },
                placeholderPane = {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.reminders))
                    }
                }
            )
        }
        composable<ArchivedReminders> {
            val splitScreenPreference by themeViewModel.splitScreenPreference.collectAsState()
            val selectedReminderIds by remindersViewModel.selectedReminderIds.collectAsState()
            AdaptiveListDetailPane<String>(
                splitScreen = splitScreenPreference,
                listPane = { onItemClick ->
                    ArchivedRemindersScreen(
                        uiState = remindersUiState,
                        selectedReminderIds = selectedReminderIds,
                        use24HourTime = use24HourTime,
                        dateFormatPreference = dateFormatPreference,
                        onNavigateBack = {
                            remindersViewModel.clearReminderSelection()
                            navController.popBackStack()
                        },
                        onItemClick = onItemClick,
                        onToggleSelection = { id -> remindersViewModel.toggleReminderSelection(id) },
                        onClearSelection = { remindersViewModel.clearReminderSelection() },
                        onDeleteSelected = { remindersViewModel.deleteSelectedReminders() }
                    )
                },
                detailPane = { selectedId, isDualPane, onNavigateBack ->
                    val item = remindersUiState.reminders.find { it.id == selectedId }
                        ?: remindersViewModel.getReminderById(selectedId)
                    if (item != null) {
                        ReminderDetailScreen(
                            item = item,
                            use24HourTime = use24HourTime,
                            dateFormatPreference = dateFormatPreference,
                            onEditClick = { navController.navigate(EditReminder(selectedId)) },
                            onDeleteClick = {
                                remindersViewModel.deleteReminder(selectedId)
                                onNavigateBack()
                            },
                            onNavigateBack = onNavigateBack,
                            isSplitScreen = isDualPane
                        )
                    } else {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.reminders))
                        }
                    }
                },
                placeholderPane = {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.reminders))
                    }
                }
            )
        }
        composable<ArchivedNotes> {
            val splitScreenPreference by themeViewModel.splitScreenPreference.collectAsState()
            val selectedNoteIds by notesViewModel.selectedNoteIds.collectAsState()
            AdaptiveListDetailPane<String>(
                splitScreen = splitScreenPreference,
                listPane = { onItemClick ->
                    ArchivedNotesScreen(
                        uiState = notesUiState,
                        selectedNoteIds = selectedNoteIds,
                        onNavigateBack = {
                            notesViewModel.clearNoteSelection()
                            navController.popBackStack()
                        },
                        onItemClick = onItemClick,
                        onToggleSelection = { id -> notesViewModel.toggleNoteSelection(id) },
                        onClearSelection = { notesViewModel.clearNoteSelection() },
                        onDeleteSelected = { notesViewModel.deleteSelectedNotes() },
                        onUnarchiveSelected = { notesViewModel.unarchiveSelectedNotes() },
                        dateFormatPreference = dateFormatPreference
                    )
                },
                detailPane = { selectedId, isDualPane, onNavigateBack ->
                    val item = (notesUiState as? NotesUiState.Success)?.items?.find { it.id == selectedId }
                        ?: notesViewModel.getNoteById(selectedId)
                    if (item != null) {
                        NoteDetailScreen(
                            item = item,
                            onEditClick = { navController.navigate(EditNote(selectedId)) },
                            onDeleteClick = {
                                notesViewModel.deleteNote(selectedId)
                                onNavigateBack()
                            },
                            onArchiveClick = {
                                notesViewModel.archiveNote(selectedId)
                                onNavigateBack()
                            },
                            onTextUpdate = { newText ->
                                notesViewModel.updateNote(
                                    selectedId,
                                    CreateNoteRequest(
                                        owner = item.owner,
                                        title = item.title,
                                        text = newText,
                                        attachment = item.attachment,
                                        archived = item.archived,
                                        book = item.book
                                    )
                                )
                            },
                            onNavigateBack = onNavigateBack,
                            isSplitScreen = isDualPane,
                            dateFormatPreference = dateFormatPreference,
                            authToken = sharedPrefs.getString("TOKEN", null),
                            linkedBookTitle = bookTitleLookup[item.book]
                        )
                    } else {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.notes))
                        }
                    }
                },
                placeholderPane = {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.notes))
                    }
                }
            )
        }
        composable<AddReminder> { backStackEntry ->
            val route = backStackEntry.toRoute<AddReminder>()
            val userId = sharedPrefs.getString("USER_ID", "") ?: ""
            AddReminderScreen(
                owner = userId,
                use24HourTime = use24HourTime,
                dateFormatPreference = dateFormatPreference,
                onSave = { req -> remindersViewModel.addReminder(req) },
                onNavigateBack = { navController.popBackStack() },
                books = bookList,
                initialBookId = route.bookId,
                initialText = route.defaultText
            )
        }
        composable<EditReminder> { backStackEntry ->
            val route = backStackEntry.toRoute<EditReminder>()
            val id = route.id
            val item = remindersUiState.reminders.find { it.id == id }
                ?: remindersViewModel.getReminderById(id)
            if (item != null) {
                EditReminderScreen(
                    item = item,
                    use24HourTime = use24HourTime,
                    dateFormatPreference = dateFormatPreference,
                    onSave = { req -> remindersViewModel.updateReminder(id, req) },
                    onNavigateBack = { navController.popBackStack() },
                    books = bookList
                )
            }
        }
    }
}
