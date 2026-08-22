package com.shishocompanion

import android.os.Bundle
import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.shishocompanion.navigation.*
import androidx.navigation.toRoute
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.shishocompanion.data.AppDatabase
import com.shishocompanion.data.NoteRepository
import com.shishocompanion.data.ReadingListRepository
import com.shishocompanion.data.ReminderRepository
import com.shishocompanion.ui.AddNoteScreen
import com.shishocompanion.ui.AddReadingListItemScreen
import com.shishocompanion.ui.AddReminderScreen
import com.shishocompanion.ui.ArchivedNotesScreen
import com.shishocompanion.ui.ArchivedRemindersScreen
import com.shishocompanion.ui.BookDetailScreen
import com.shishocompanion.ui.EditNoteScreen
import com.shishocompanion.ui.EditReadingListItemScreen
import com.shishocompanion.ui.EditReminderScreen
import com.shishocompanion.ui.LoginScreen
import com.shishocompanion.ui.LoginViewModel
import com.shishocompanion.ui.NoteDetailScreen
import com.shishocompanion.ui.NotesListScreen
import com.shishocompanion.ui.NotesUiState
import com.shishocompanion.ui.NotesViewModel
import com.shishocompanion.ui.ReadingListScreen
import com.shishocompanion.ui.ReadingListUiState
import com.shishocompanion.ui.ReadingListViewModel
import com.shishocompanion.ui.ReminderDetailScreen
import com.shishocompanion.ui.RemindersListScreen
import com.shishocompanion.ui.RemindersViewModel
import com.shishocompanion.ui.SettingsScreen
import com.shishocompanion.ui.SettingsViewModel
import com.shishocompanion.ui.DiscordLinkUiState
import com.shishocompanion.ui.ThemeViewModel
import com.shishocompanion.ui.AnnouncementsViewModel
import com.shishocompanion.ui.theme.ShishoCompanionTheme
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import dagger.hilt.android.AndroidEntryPoint
import androidx.hilt.navigation.compose.hiltViewModel

// Route string constants for type-safe navigation route matching
private val ROUTE_READING_LIST = ReadingList::class.qualifiedName!!
private val ROUTE_NOTES = Notes::class.qualifiedName!!
private val ROUTE_REMINDERS = Reminders::class.qualifiedName!!
private val MAIN_TABS = listOf(ROUTE_READING_LIST, ROUTE_NOTES, ROUTE_REMINDERS)

private fun getTabIndex(route: String?): Int {
    return when (route?.substringBefore("/")) {
        ROUTE_READING_LIST -> 0
        ROUTE_NOTES -> 1
        ROUTE_REMINDERS -> 2
        else -> -1
    }
}

sealed interface RightPaneState {
    object Empty : RightPaneState
    data class View(val id: String) : RightPaneState
    object Add : RightPaneState
    data class Edit(val id: String) : RightPaneState
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val intentAction = MutableStateFlow<String?>(null)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intentAction.value = intent.action
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        com.shishocompanion.notifications.NotificationHelper.createNotificationChannel(this)

        intent?.action?.let { intentAction.value = it }
        
        val httpClient = HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    prettyPrint = true
                })
            }
        }
        
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = ReadingListRepository(httpClient, database.readingListDao())
        val noteRepository = NoteRepository(httpClient, database.noteDao())
        val reminderRepository = ReminderRepository(httpClient, database.reminderDao())
        
        val masterKey = MasterKey.Builder(applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
            
        val sharedPrefs = EncryptedSharedPreferences.create(
            applicationContext,
            "secret_shared_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        val imageLoader = coil.ImageLoader.Builder(applicationContext)
            .okHttpClient {
                okhttp3.OkHttpClient.Builder()
                    .addInterceptor { chain ->
                        val request = chain.request().newBuilder()
                        val token = sharedPrefs.getString("TOKEN", null)
                        if (token != null && chain.request().url.toString().contains("/api/files/")) {
                            request.addHeader("Authorization", "Bearer $token")
                        }
                        chain.proceed(request.build())
                    }
                    .build()
            }
            .build()
        coil.Coil.setImageLoader(imageLoader)

        enableEdgeToEdge()
        setContent {
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val themePreference by themeViewModel.themePreference.collectAsState()
            val timeFormatPreference by themeViewModel.timeFormatPreference.collectAsState()
            val dateFormatPreference by themeViewModel.dateFormatPreference.collectAsState()
            val sendRemindersToDiscordPreference by themeViewModel.sendRemindersToDiscordPreference.collectAsState()
            val context = androidx.compose.ui.platform.LocalContext.current
            val use24HourTime = when (timeFormatPreference) {
                "24_HOUR" -> true
                "12_HOUR" -> false
                else -> android.text.format.DateFormat.is24HourFormat(context)
            }

            val languagePreference by themeViewModel.languagePreference.collectAsState()
            
            val locale = if (languagePreference == "SYSTEM") java.util.Locale.getDefault() else java.util.Locale.forLanguageTag(languagePreference)
            val configuration = LocalConfiguration.current
            val updatedContext = remember(context, locale, configuration) {
                val config = android.content.res.Configuration(configuration)
                config.setLocale(locale)
                val newContext = context.createConfigurationContext(config)
                object : android.content.ContextWrapper(context) {
                    override fun getResources(): android.content.res.Resources {
                        return newContext.resources
                    }
                }
            }

            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalContext provides updatedContext,
                LocalConfiguration provides updatedContext.resources.configuration
            ) {
                ShishoCompanionTheme(themePreference = themePreference) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    
                    val loginViewModel: LoginViewModel = hiltViewModel()
                    val loginUiState by loginViewModel.uiState.collectAsState()

                    val listViewModel: ReadingListViewModel = hiltViewModel()
                    val listUiState by listViewModel.uiState.collectAsState()

                    val notesViewModel: NotesViewModel = hiltViewModel()
                    val notesUiState by notesViewModel.uiState.collectAsState()

                    val remindersViewModel: RemindersViewModel = hiltViewModel()
                    val remindersUiState by remindersViewModel.uiState.collectAsState()
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination?.route
                    
                    val currentIntentAction by intentAction.collectAsState()
                    LaunchedEffect(currentIntentAction, currentDestination) {
                        if (currentIntentAction == "com.shishocompanion.ADD_NOTE") {
                            if (currentDestination != null && currentDestination != "login") {
                                navController.navigate(Notes) {
                                    popUpTo<ReadingList>() { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                                navController.navigate(AddNote)
                                intentAction.value = null
                            }
                        }
                    }
                    
                    val announcementsViewModel: AnnouncementsViewModel = hiltViewModel()
                    val hasUnreadAnnouncements by announcementsViewModel.hasUnreadAnnouncement.collectAsState()
                    val announcements by announcementsViewModel.announcements.collectAsState()

                    var listRightPaneState by remember { mutableStateOf<RightPaneState>(RightPaneState.Empty) }
                    var notesRightPaneState by remember { mutableStateOf<RightPaneState>(RightPaneState.Empty) }
                    var remindersRightPaneState by remember { mutableStateOf<RightPaneState>(RightPaneState.Empty) }
                    var archivedNotesRightPaneState by remember { mutableStateOf<RightPaneState>(RightPaneState.Empty) }
                    var archivedRemindersRightPaneState by remember { mutableStateOf<RightPaneState>(RightPaneState.Empty) }

                    val showSessionExpired by loginViewModel.showSessionExpiredDialog.collectAsState()
                    if (showSessionExpired) {
                        AlertDialog(
                            onDismissRequest = { loginViewModel.dismissSessionExpiredDialog() },
                            title = { Text(stringResource(R.string.app_name)) },
                            text = { Text("Your session has expired. Please log in again to sync your data.") },
                            confirmButton = {
                                TextButton(onClick = {
                                    loginViewModel.dismissSessionExpiredDialog()
                                    loginViewModel.logout()
                                    navController.navigate(Login) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }) {
                                    Text("Log In")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { loginViewModel.dismissSessionExpiredDialog() }) {
                                    Text("Later")
                                }
                            }
                        )
                    }

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
                                        val windowInfo = androidx.compose.ui.platform.LocalWindowInfo.current
                                        val density = androidx.compose.ui.platform.LocalDensity.current
                                        val isLargeScreen = with(density) { windowInfo.containerSize.width.toDp().value >= 600 }

                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.add_book)) },
                                            onClick = {
                                                expanded = false
                                                if (isLargeScreen && themeViewModel.splitScreenPreference.value) {
                                                    if (currentDestination == ROUTE_READING_LIST) listRightPaneState = RightPaneState.Add
                                                    else {
                                                        navController.navigate(ReadingList) { popUpTo<ReadingList>() { saveState = true }; launchSingleTop = true; restoreState = true }
                                                        listRightPaneState = RightPaneState.Add
                                                    }
                                                } else {
                                                    navController.navigate(Add)
                                                }
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.add_note)) },
                                            onClick = {
                                                expanded = false
                                                if (isLargeScreen && themeViewModel.splitScreenPreference.value) {
                                                    if (currentDestination == ROUTE_NOTES) notesRightPaneState = RightPaneState.Add
                                                    else {
                                                        navController.navigate(Notes) { popUpTo<ReadingList>() { saveState = true }; launchSingleTop = true; restoreState = true }
                                                        notesRightPaneState = RightPaneState.Add
                                                    }
                                                } else {
                                                    navController.navigate(AddNote)
                                                }
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.add_reminder)) },
                                            onClick = {
                                                expanded = false
                                                if (isLargeScreen && themeViewModel.splitScreenPreference.value) {
                                                    if (currentDestination == ROUTE_REMINDERS) remindersRightPaneState = RightPaneState.Add
                                                    else {
                                                        navController.navigate(Reminders) { popUpTo<ReadingList>() { saveState = true }; launchSingleTop = true; restoreState = true }
                                                        remindersRightPaneState = RightPaneState.Add
                                                    }
                                                } else {
                                                    navController.navigate(AddReminder)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        },
                        bottomBar = {
                            if (currentDestination in MAIN_TABS) {
                                NavigationBar {
                                    NavigationBarItem(
                                        icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = stringResource(R.string.reading_list)) },
                                        label = { Text(stringResource(R.string.list_label)) },
                                        selected = currentDestination == ROUTE_READING_LIST,
                                        onClick = {
                                            navController.navigate(ReadingList) {
                                                popUpTo<ReadingList>() { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    )
                                    NavigationBarItem(
                                        icon = { Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.notes)) },
                                        label = { Text(stringResource(R.string.notes)) },
                                        selected = currentDestination == ROUTE_NOTES,
                                        onClick = {
                                            navController.navigate(Notes) {
                                                popUpTo<ReadingList>() { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    )
                                    NavigationBarItem(
                                        icon = { Icon(Icons.Filled.Notifications, contentDescription = stringResource(R.string.reminders)) },
                                        label = { Text(stringResource(R.string.reminders)) },
                                        selected = currentDestination == ROUTE_REMINDERS,
                                        onClick = {
                                            navController.navigate(Reminders) {
                                                popUpTo<ReadingList>() { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        NavHost(
                            navController = navController, 
                            startDestination = Login, 
                            modifier = Modifier
                                .padding(innerPadding)
                                .consumeWindowInsets(innerPadding),
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
                                            popUpTo<Login>() { inclusive = true }
                                        }
                                        if (intentAction.value == "com.shishocompanion.ADD_NOTE") {
                                            intentAction.value = null
                                            navController.navigate(AddNote)
                                        }
                                        listViewModel.syncReadingList()
                                        notesViewModel.syncNotes()
                                        remindersViewModel.syncReminders()
                                    }
                                )
                            }
                            composable<ReadingList> {
                                val splitScreenEnabled = themeViewModel.splitScreenPreference.collectAsState().value
                                val windowInfo = androidx.compose.ui.platform.LocalWindowInfo.current
                                val density = androidx.compose.ui.platform.LocalDensity.current
                                val isLargeScreen = with(density) { windowInfo.containerSize.width.toDp().value >= 600 }
                                val useSplitScreen = isLargeScreen && splitScreenEnabled

                                if (useSplitScreen) {
                                    val scaffoldNavigator = rememberListDetailPaneScaffoldNavigator<Any>()
                                    ListDetailPaneScaffold(
                                        directive = scaffoldNavigator.scaffoldDirective,
                                        value = scaffoldNavigator.scaffoldValue,
                                        listPane = {
                                            AnimatedPane {
                                                ReadingListScreen(
                                                    uiState = listUiState,
                                                    hasUnreadAnnouncements = hasUnreadAnnouncements,
                                                    onSyncClick = { listViewModel.syncReadingList() },
                                                    onItemClick = { id -> listRightPaneState = RightPaneState.View(id) },
                                                    onSettingsClick = { navController.navigate(Settings) },
                                                    onDeleteReadingListItem = { listViewModel.deleteItem(it) }
                                                )
                                            }
                                        },
                                        detailPane = {
                                            AnimatedPane {
                                                when (val state = listRightPaneState) {
                                                    is RightPaneState.Empty -> {
                                                        Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                                                            Text(stringResource(R.string.list_label))
                                                        }
                                                    }
                                                    is RightPaneState.View -> {
                                                        val item = (listUiState as? ReadingListUiState.Success)?.items?.find { it.id == state.id } ?: listViewModel.getItemById(state.id)
                                                        if (item != null) {
                                                            BookDetailScreen(
                                                                item = item,
                                                                onEditClick = { listRightPaneState = RightPaneState.Edit(state.id) },
                                                                onDeleteClick = {
                                                                    listViewModel.deleteItem(state.id)
                                                                    listRightPaneState = RightPaneState.Empty
                                                                },
                                                                onNavigateBack = { listRightPaneState = RightPaneState.Empty },
                                                                isSplitScreen = true,
                                                                dateFormatPreference = dateFormatPreference
                                                            )
                                                        }
                                                    }
                                                    is RightPaneState.Add -> {
                                                        AddReadingListItemScreen(
                                                            onSave = { req ->
                                                                listViewModel.addItem(req)
                                                                listRightPaneState = RightPaneState.Empty
                                                            },
                                                            onNavigateBack = { listRightPaneState = RightPaneState.Empty },
                                                            onSearchBooks = { query, isIsbn, onResult ->
                                                                listViewModel.searchGoogleBooks(query, isIsbn, onResult)
                                                            },
                                                            isSplitScreen = true
                                                        )
                                                    }
                                                    is RightPaneState.Edit -> {
                                                        val item = (listUiState as? ReadingListUiState.Success)?.items?.find { it.id == state.id } ?: listViewModel.getItemById(state.id)
                                                        if (item != null) {
                                                            EditReadingListItemScreen(
                                                                item = item,
                                                                onSave = { req ->
                                                                    listViewModel.updateItem(state.id, req)
                                                                    listRightPaneState = RightPaneState.View(state.id)
                                                                },
                                                                onNavigateBack = { listRightPaneState = RightPaneState.View(state.id) },
                                                                isSplitScreen = true
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    )
                                } else {
                                    ReadingListScreen(
                                        uiState = listUiState,
                                        hasUnreadAnnouncements = hasUnreadAnnouncements,
                                        onSyncClick = { listViewModel.syncReadingList() },
                                        onItemClick = { id -> navController.navigate(Detail(id)) },
                                        onSettingsClick = { navController.navigate(Settings) },
                                        onDeleteReadingListItem = { listViewModel.deleteItem(it) }
                                    )
                                }
                            }
                            composable<Notes> {
                                val splitScreenEnabled = themeViewModel.splitScreenPreference.collectAsState().value
                                val windowInfo = androidx.compose.ui.platform.LocalWindowInfo.current
                                val density = androidx.compose.ui.platform.LocalDensity.current
                                val isLargeScreen = with(density) { windowInfo.containerSize.width.toDp().value >= 600 }
                                val useSplitScreen = isLargeScreen && splitScreenEnabled

                                val selectedNoteIds by notesViewModel.selectedNoteIds.collectAsState()

                                if (useSplitScreen) {
                                    val scaffoldNavigator = rememberListDetailPaneScaffoldNavigator<Any>()
                                    ListDetailPaneScaffold(
                                        directive = scaffoldNavigator.scaffoldDirective,
                                        value = scaffoldNavigator.scaffoldValue,
                                        listPane = {
                                            AnimatedPane {
                                                NotesListScreen(
                                                    uiState = notesUiState,
                                                    hasUnreadAnnouncements = hasUnreadAnnouncements,
                                                    selectedNoteIds = selectedNoteIds,
                                                    onSyncClick = { notesViewModel.syncNotes() },
                                                    onItemClick = { id -> notesRightPaneState = RightPaneState.View(id) },
                                                    onSettingsClick = { navController.navigate(Settings) },
                                                    onArchiveClick = { navController.navigate(ArchivedNotes) },
                                                    onToggleSelection = { id -> notesViewModel.toggleNoteSelection(id) },
                                                    onClearSelection = { notesViewModel.clearNoteSelection() },
                                                    onDeleteSelected = { notesViewModel.deleteSelectedNotes() },
                                                    onArchiveSelected = { notesViewModel.archiveSelectedNotes() },
                                                    onDeleteNote = { notesViewModel.deleteNote(it) },
                                                    onArchiveNote = { notesViewModel.archiveNote(it) },
                                                    dateFormatPreference = dateFormatPreference
                                                )
                                            }
                                        },
                                        detailPane = {
                                            AnimatedPane {
                                                when (val state = notesRightPaneState) {
                                                    is RightPaneState.Empty -> {
                                                        Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                                                            Text(stringResource(R.string.notes))
                                                        }
                                                    }
                                                    is RightPaneState.View -> {
                                                        val item = (notesUiState as? NotesUiState.Success)?.items?.find { it.id == state.id } ?: notesViewModel.getNoteById(state.id)
                                                        if (item != null) {
                                                            NoteDetailScreen(
                                                                item = item,
                                                                onEditClick = { notesRightPaneState = RightPaneState.Edit(state.id) },
                                                                onDeleteClick = {
                                                                    notesViewModel.deleteNote(state.id)
                                                                    notesRightPaneState = RightPaneState.Empty
                                                                },
                                                                onArchiveClick = {
                                                                    notesViewModel.archiveNote(state.id)
                                                                    notesRightPaneState = RightPaneState.Empty
                                                                },
                                                                onTextUpdate = { newText ->
                                                                    val req = com.shishocompanion.data.CreateNoteRequest(
                                                                        userId = item.userId,
                                                                        title = item.title,
                                                                        text = newText,
                                                                        attachment = item.attachment,
                                                                        archived = item.archived
                                                                    )
                                                                    notesViewModel.updateNote(state.id, req)
                                                                },
                                                                onNavigateBack = { notesRightPaneState = RightPaneState.Empty },
                                                                isSplitScreen = true,
                                                                dateFormatPreference = dateFormatPreference
                                                            )
                                                        }
                                                    }
                                                    is RightPaneState.Add -> {
                                                        AddNoteScreen(
                                                            userId = sharedPrefs.getString("USER_ID", "") ?: "",
                                                            onSave = { req ->
                                                                notesViewModel.addNote(req)
                                                                notesRightPaneState = RightPaneState.Empty
                                                            },
                                                            onNavigateBack = { notesRightPaneState = RightPaneState.Empty },
                                                            isSplitScreen = true
                                                        )
                                                    }
                                                    is RightPaneState.Edit -> {
                                                        val item = (notesUiState as? NotesUiState.Success)?.items?.find { it.id == state.id } ?: notesViewModel.getNoteById(state.id)
                                                        if (item != null) {
                                                            EditNoteScreen(
                                                                item = item,
                                                                onSave = { req ->
                                                                    notesViewModel.updateNote(state.id, req)
                                                                    notesRightPaneState = RightPaneState.View(state.id)
                                                                },
                                                                onNavigateBack = { notesRightPaneState = RightPaneState.View(state.id) },
                                                                isSplitScreen = true
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    )
                                } else {
                                    NotesListScreen(
                                        uiState = notesUiState,
                                        hasUnreadAnnouncements = hasUnreadAnnouncements,
                                        selectedNoteIds = selectedNoteIds,
                                        onSyncClick = { notesViewModel.syncNotes() },
                                        onItemClick = { id -> navController.navigate(NoteDetail(id)) },
                                        onSettingsClick = { navController.navigate(Settings) },
                                        onArchiveClick = { navController.navigate(ArchivedNotes) },
                                        onToggleSelection = { id -> notesViewModel.toggleNoteSelection(id) },
                                        onClearSelection = { notesViewModel.clearNoteSelection() },
                                        onDeleteSelected = { notesViewModel.deleteSelectedNotes() },
                                        onArchiveSelected = { notesViewModel.archiveSelectedNotes() },
                                        onDeleteNote = { notesViewModel.deleteNote(it) },
                                        onArchiveNote = { notesViewModel.archiveNote(it) },
                                        dateFormatPreference = dateFormatPreference
                                    )
                                }
                            }
                            composable<Add> {
                                AddReadingListItemScreen(
                                    onSave = { req ->
                                        listViewModel.addItem(req)
                                    },
                                    onNavigateBack = { navController.popBackStack() },
                                    onSearchBooks = { query, isIsbn, onResult ->
                                        listViewModel.searchGoogleBooks(query, isIsbn, onResult)
                                    }
                                )
                            }
                            composable("detail/{id}") { backStackEntry ->
                                val id = backStackEntry.arguments?.getString("id")
                                if (id != null) {
                                    val item = (listUiState as? ReadingListUiState.Success)?.items?.find { it.id == id } ?: listViewModel.getItemById(id)
                                    if (item != null) {
                                        BookDetailScreen(
                                            item = item,
                                            onEditClick = { navController.navigate(Edit(id)) },
                                            onDeleteClick = {
                                                listViewModel.deleteItem(id)
                                                navController.popBackStack()
                                            },
                                            onNavigateBack = { navController.popBackStack() },
                                            dateFormatPreference = dateFormatPreference
                                        )
                                    }
                                }
                            }
                            composable("edit/{id}") { backStackEntry ->
                                val id = backStackEntry.arguments?.getString("id")
                                if (id != null) {
                                    val item = (listUiState as? ReadingListUiState.Success)?.items?.find { it.id == id } ?: listViewModel.getItemById(id)
                                    if (item != null) {
                                        EditReadingListItemScreen(
                                            item = item,
                                            onSave = { req ->
                                                listViewModel.updateItem(id, req)
                                            },
                                            onNavigateBack = { navController.popBackStack() }
                                        )
                                    }
                                }
                            }
                            composable<AddNote> {
                                val userId = sharedPrefs.getString("USER_ID", "") ?: ""
                                AddNoteScreen(
                                    userId = userId,
                                    onSave = { req -> notesViewModel.addNote(req) },
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }
                            composable("note_detail/{id}") { backStackEntry ->
                                val id = backStackEntry.arguments?.getString("id")
                                if (id != null) {
                                    val item = (notesUiState as? NotesUiState.Success)?.items?.find { it.id == id } ?: notesViewModel.getNoteById(id)
                                    if (item != null) {
                                        NoteDetailScreen(
                                            item = item,
                                            onEditClick = { navController.navigate(EditNote(id)) },
                                            onDeleteClick = {
                                                notesViewModel.deleteNote(id)
                                                navController.popBackStack()
                                            },
                                            onArchiveClick = {
                                                notesViewModel.archiveNote(id)
                                                navController.popBackStack()
                                            },
                                            onTextUpdate = { newText ->
                                                val req = com.shishocompanion.data.CreateNoteRequest(
                                                    userId = item.userId,
                                                    title = item.title,
                                                    text = newText,
                                                    attachment = item.attachment,
                                                    archived = item.archived
                                                )
                                                notesViewModel.updateNote(id, req)
                                            },
                                            onNavigateBack = { navController.popBackStack() }
                                        )
                                    }
                                }
                            }
                            composable("edit_note/{id}") { backStackEntry ->
                                val id = backStackEntry.arguments?.getString("id")
                                if (id != null) {
                                    val item = (notesUiState as? NotesUiState.Success)?.items?.find { it.id == id } ?: notesViewModel.getNoteById(id)
                                    if (item != null) {
                                        EditNoteScreen(
                                            item = item,
                                            onSave = { req -> notesViewModel.updateNote(id, req) },
                                            onNavigateBack = { navController.popBackStack() }
                                        )
                                    }
                                }
                            }
                            composable<Settings> {
                                val settingsViewModel: SettingsViewModel = hiltViewModel()
                                val discordLinkUiState by settingsViewModel.uiState.collectAsState()
                                val linkedDiscordId by settingsViewModel.linkedDiscordId.collectAsState()
                                val googleBooksApiKey by settingsViewModel.googleBooksApiKey.collectAsState()
                                val swipeActionPreference by settingsViewModel.swipeActionPreference.collectAsState()
                                val sendRemindersToDiscordPreference by themeViewModel.sendRemindersToDiscordPreference.collectAsState()

                                LaunchedEffect(Unit) {
                                    announcementsViewModel.markAsRead()
                                }

                                SettingsScreen(
                                    userEmail = sharedPrefs.getString("USER_EMAIL", "") ?: "",
                                    themePreference = themePreference,
                                    onThemePreferenceChanged = { themeViewModel.setThemePreference(it) },
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
                                    onVerifyDiscordLink = { pin -> settingsViewModel.verifyDiscordPin(linkedDiscordId.ifBlank { (discordLinkUiState as? DiscordLinkUiState.PinRequested)?.discordId ?: "" }, pin) },
                                    onUnlinkDiscord = { settingsViewModel.unlinkDiscord() },
                                    onResetDiscordLinkState = { settingsViewModel.resetState() },
                                    announcements = announcements,
                                    splitScreenPreference = themeViewModel.splitScreenPreference.collectAsState().value,
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
                                val splitScreenEnabled = themeViewModel.splitScreenPreference.collectAsState().value
                                val windowInfo = androidx.compose.ui.platform.LocalWindowInfo.current
                                val density = androidx.compose.ui.platform.LocalDensity.current
                                val isLargeScreen = with(density) { windowInfo.containerSize.width.toDp().value >= 600 }
                                val useSplitScreen = isLargeScreen && splitScreenEnabled

                                val selectedReminderIds by remindersViewModel.selectedReminderIds.collectAsState()

                                if (useSplitScreen) {
                                    val scaffoldNavigator = rememberListDetailPaneScaffoldNavigator<Any>()
                                    ListDetailPaneScaffold(
                                        directive = scaffoldNavigator.scaffoldDirective,
                                        value = scaffoldNavigator.scaffoldValue,
                                        listPane = {
                                            AnimatedPane {
                                                RemindersListScreen(
                                                    uiState = remindersUiState,
                                                    hasUnreadAnnouncements = hasUnreadAnnouncements,
                                                    selectedReminderIds = selectedReminderIds,
                                                    use24HourTime = use24HourTime,
                                                    dateFormatPreference = dateFormatPreference,
                                                    onSyncClick = { remindersViewModel.syncReminders() },
                                                    onItemClick = { id -> remindersRightPaneState = RightPaneState.View(id) },
                                                    onSettingsClick = { navController.navigate(Settings) },
                                                    onArchiveClick = { navController.navigate(ArchivedReminders) },
                                                    onToggleSelection = { id -> remindersViewModel.toggleReminderSelection(id) },
                                                    onClearSelection = { remindersViewModel.clearReminderSelection() },
                                                    onDeleteSelected = { remindersViewModel.deleteSelectedReminders() },
                                                    onDeleteReminder = { remindersViewModel.deleteReminder(it) },
                                                    onArchiveReminder = { remindersViewModel.archiveReminder(it) }
                                                )
                                            }
                                        },
                                        detailPane = {
                                            AnimatedPane {
                                                when (val state = remindersRightPaneState) {
                                                    is RightPaneState.Empty -> {
                                                        Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                                                            Text(stringResource(R.string.reminders))
                                                        }
                                                    }
                                                    is RightPaneState.View -> {
                                                        val item = remindersUiState.reminders.find { it.id == state.id } ?: remindersViewModel.getReminderById(state.id)
                                                        if (item != null) {
                                                            ReminderDetailScreen(
                                                                item = item,
                                                                use24HourTime = use24HourTime,
                                                                dateFormatPreference = dateFormatPreference,
                                                                onEditClick = { remindersRightPaneState = RightPaneState.Edit(state.id) },
                                                                onDeleteClick = {
                                                                    remindersViewModel.deleteReminder(state.id)
                                                                    remindersRightPaneState = RightPaneState.Empty
                                                                },
                                                                onNavigateBack = { remindersRightPaneState = RightPaneState.Empty },
                                                                isSplitScreen = true
                                                            )
                                                        }
                                                    }
                                                    is RightPaneState.Add -> {
                                                        AddReminderScreen(
                                                            userId = sharedPrefs.getString("USER_ID", "") ?: "",
                                                            use24HourTime = use24HourTime,
                                                            dateFormatPreference = dateFormatPreference,
                                                            onSave = { req ->
                                                                remindersViewModel.addReminder(req)
                                                                remindersRightPaneState = RightPaneState.Empty
                                                            },
                                                            onNavigateBack = { remindersRightPaneState = RightPaneState.Empty },
                                                            isSplitScreen = true
                                                        )
                                                    }
                                                    is RightPaneState.Edit -> {
                                                        val item = remindersUiState.reminders.find { it.id == state.id } ?: remindersViewModel.getReminderById(state.id)
                                                        if (item != null) {
                                                            EditReminderScreen(
                                                                item = item,
                                                                use24HourTime = use24HourTime,
                                                                onSave = { req ->
                                                                    remindersViewModel.updateReminder(state.id, req)
                                                                    remindersRightPaneState = RightPaneState.View(state.id)
                                                                },
                                                                onNavigateBack = { remindersRightPaneState = RightPaneState.View(state.id) },
                                                                isSplitScreen = true
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    )
                                } else {
                                    RemindersListScreen(
                                        uiState = remindersUiState,
                                        hasUnreadAnnouncements = hasUnreadAnnouncements,
                                        selectedReminderIds = selectedReminderIds,
                                        use24HourTime = use24HourTime,
                                        dateFormatPreference = dateFormatPreference,
                                        onSyncClick = { remindersViewModel.syncReminders() },
                                        onItemClick = { id -> navController.navigate(ReminderDetail(id)) },
                                        onSettingsClick = { navController.navigate(Settings) },
                                        onArchiveClick = { navController.navigate(ArchivedReminders) },
                                        onToggleSelection = { id -> remindersViewModel.toggleReminderSelection(id) },
                                        onClearSelection = { remindersViewModel.clearReminderSelection() },
                                        onDeleteSelected = { remindersViewModel.deleteSelectedReminders() },
                                        onDeleteReminder = { remindersViewModel.deleteReminder(it) },
                                        onArchiveReminder = { remindersViewModel.archiveReminder(it) }
                                    )
                                }
                            }
                            composable<ArchivedReminders> {
                                val splitScreenEnabled = themeViewModel.splitScreenPreference.collectAsState().value
                                val windowInfo = androidx.compose.ui.platform.LocalWindowInfo.current
                                val density = androidx.compose.ui.platform.LocalDensity.current
                                val isLargeScreen = with(density) { windowInfo.containerSize.width.toDp().value >= 600 }
                                val useSplitScreen = isLargeScreen && splitScreenEnabled

                                val selectedReminderIds by remindersViewModel.selectedReminderIds.collectAsState()

                                if (useSplitScreen) {
                                    val scaffoldNavigator = rememberListDetailPaneScaffoldNavigator<Any>()
                                    ListDetailPaneScaffold(
                                        directive = scaffoldNavigator.scaffoldDirective,
                                        value = scaffoldNavigator.scaffoldValue,
                                        listPane = {
                                            AnimatedPane {
                                                ArchivedRemindersScreen(
                                                    uiState = remindersUiState,
                                                    selectedReminderIds = selectedReminderIds,
                                                    use24HourTime = use24HourTime,
                                                    dateFormatPreference = dateFormatPreference,
                                                    onNavigateBack = { navController.popBackStack() },
                                                    onItemClick = { id -> archivedRemindersRightPaneState = RightPaneState.View(id) },
                                                    onToggleSelection = { id -> remindersViewModel.toggleReminderSelection(id) },
                                                    onClearSelection = { remindersViewModel.clearReminderSelection() },
                                                    onDeleteSelected = { remindersViewModel.deleteSelectedReminders() }
                                                )
                                            }
                                        },
                                        detailPane = {
                                            AnimatedPane {
                                                when (val state = archivedRemindersRightPaneState) {
                                                    is RightPaneState.Empty -> {
                                                        Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                                                            Text(stringResource(R.string.reminders))
                                                        }
                                                    }
                                                    is RightPaneState.View -> {
                                                        val item = remindersUiState.reminders.find { it.id == state.id } ?: remindersViewModel.getReminderById(state.id)
                                                        if (item != null) {
                                                            ReminderDetailScreen(
                                                                item = item,
                                                                use24HourTime = use24HourTime,
                                                                dateFormatPreference = dateFormatPreference,
                                                                onEditClick = { archivedRemindersRightPaneState = RightPaneState.Edit(state.id) },
                                                                onDeleteClick = {
                                                                    remindersViewModel.deleteReminder(state.id)
                                                                    archivedRemindersRightPaneState = RightPaneState.Empty
                                                                },
                                                                onNavigateBack = { archivedRemindersRightPaneState = RightPaneState.Empty },
                                                                isSplitScreen = true
                                                            )
                                                        }
                                                    }
                                                    is RightPaneState.Edit -> {
                                                        val item = remindersUiState.reminders.find { it.id == state.id } ?: remindersViewModel.getReminderById(state.id)
                                                        if (item != null) {
                                                            EditReminderScreen(
                                                                item = item,
                                                                use24HourTime = use24HourTime,
                                                                onSave = { req ->
                                                                    remindersViewModel.updateReminder(state.id, req)
                                                                    archivedRemindersRightPaneState = RightPaneState.View(state.id)
                                                                },
                                                                onNavigateBack = { archivedRemindersRightPaneState = RightPaneState.View(state.id) },
                                                                isSplitScreen = true
                                                            )
                                                        }
                                                    }
                                                    else -> {}
                                                }
                                            }
                                        }
                                    )
                                } else {
                                    ArchivedRemindersScreen(
                                        uiState = remindersUiState,
                                        selectedReminderIds = selectedReminderIds,
                                        use24HourTime = use24HourTime,
                                        dateFormatPreference = dateFormatPreference,
                                        onNavigateBack = { navController.popBackStack() },
                                        onItemClick = { id -> navController.navigate(ReminderDetail(id)) },
                                        onToggleSelection = { id -> remindersViewModel.toggleReminderSelection(id) },
                                        onClearSelection = { remindersViewModel.clearReminderSelection() },
                                        onDeleteSelected = { remindersViewModel.deleteSelectedReminders() }
                                    )
                                }
                            }
                            composable<ArchivedNotes> {
                                val splitScreenEnabled = themeViewModel.splitScreenPreference.collectAsState().value
                                val windowInfo = androidx.compose.ui.platform.LocalWindowInfo.current
                                val density = androidx.compose.ui.platform.LocalDensity.current
                                val isLargeScreen = with(density) { windowInfo.containerSize.width.toDp().value >= 600 }
                                val useSplitScreen = isLargeScreen && splitScreenEnabled

                                val selectedNoteIds by notesViewModel.selectedNoteIds.collectAsState()

                                if (useSplitScreen) {
                                    val scaffoldNavigator = rememberListDetailPaneScaffoldNavigator<Any>()
                                    ListDetailPaneScaffold(
                                        directive = scaffoldNavigator.scaffoldDirective,
                                        value = scaffoldNavigator.scaffoldValue,
                                        listPane = {
                                            AnimatedPane {
                                                ArchivedNotesScreen(
                                                    uiState = notesUiState,
                                                    selectedNoteIds = selectedNoteIds,
                                                    onNavigateBack = { navController.popBackStack() },
                                                    onItemClick = { id -> archivedNotesRightPaneState = RightPaneState.View(id) },
                                                    onToggleSelection = { id -> notesViewModel.toggleNoteSelection(id) },
                                                    onClearSelection = { notesViewModel.clearNoteSelection() },
                                                    onDeleteSelected = { notesViewModel.deleteSelectedNotes() },
                                                    onUnarchiveSelected = { notesViewModel.unarchiveSelectedNotes() },
                                                    dateFormatPreference = dateFormatPreference
                                                )
                                            }
                                        },
                                        detailPane = {
                                            AnimatedPane {
                                                when (val state = archivedNotesRightPaneState) {
                                                    is RightPaneState.Empty -> {
                                                        Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                                                            Text(stringResource(R.string.notes))
                                                        }
                                                    }
                                                    is RightPaneState.View -> {
                                                        val item = (notesUiState as? NotesUiState.Success)?.items?.find { it.id == state.id } ?: notesViewModel.getNoteById(state.id)
                                                        if (item != null) {
                                                            NoteDetailScreen(
                                                                item = item,
                                                                onEditClick = { archivedNotesRightPaneState = RightPaneState.Edit(state.id) },
                                                                onDeleteClick = {
                                                                    notesViewModel.deleteNote(state.id)
                                                                    archivedNotesRightPaneState = RightPaneState.Empty
                                                                },
                                                                onArchiveClick = {
                                                                    notesViewModel.archiveNote(state.id)
                                                                    archivedNotesRightPaneState = RightPaneState.Empty
                                                                },
                                                                onTextUpdate = { newText ->
                                                                    val req = com.shishocompanion.data.CreateNoteRequest(
                                                                        userId = item.userId,
                                                                        title = item.title,
                                                                        text = newText,
                                                                        attachment = item.attachment,
                                                                        archived = item.archived
                                                                    )
                                                                    notesViewModel.updateNote(state.id, req)
                                                                },
                                                                onNavigateBack = { archivedNotesRightPaneState = RightPaneState.Empty },
                                                                isSplitScreen = true
                                                            )
                                                        }
                                                    }
                                                    is RightPaneState.Edit -> {
                                                        val item = (notesUiState as? NotesUiState.Success)?.items?.find { it.id == state.id } ?: notesViewModel.getNoteById(state.id)
                                                        if (item != null) {
                                                            EditNoteScreen(
                                                                item = item,
                                                                onSave = { req ->
                                                                    notesViewModel.updateNote(state.id, req)
                                                                    archivedNotesRightPaneState = RightPaneState.View(state.id)
                                                                },
                                                                onNavigateBack = { archivedNotesRightPaneState = RightPaneState.View(state.id) },
                                                                isSplitScreen = true
                                                            )
                                                        }
                                                    }
                                                    else -> {}
                                                }
                                            }
                                        }
                                    )
                                } else {
                                    ArchivedNotesScreen(
                                        uiState = notesUiState,
                                        selectedNoteIds = selectedNoteIds,
                                        onNavigateBack = { navController.popBackStack() },
                                        onItemClick = { id -> navController.navigate(NoteDetail(id)) },
                                        onToggleSelection = { id -> notesViewModel.toggleNoteSelection(id) },
                                        onClearSelection = { notesViewModel.clearNoteSelection() },
                                        onDeleteSelected = { notesViewModel.deleteSelectedNotes() },
                                        onUnarchiveSelected = { notesViewModel.unarchiveSelectedNotes() },
                                        dateFormatPreference = dateFormatPreference
                                    )
                                }
                            }
                            composable<AddReminder> {
                                val userId = sharedPrefs.getString("USER_ID", "") ?: ""
                                AddReminderScreen(
                                    userId = userId,
                                    use24HourTime = use24HourTime,
                                    onSave = { req -> remindersViewModel.addReminder(req) },
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }
                            composable("reminder_detail/{id}") { backStackEntry ->
                                val id = backStackEntry.arguments?.getString("id")
                                if (id != null) {
                                    val item = remindersUiState.reminders.find { it.id == id } ?: remindersViewModel.getReminderById(id)
                                    if (item != null) {
                                        ReminderDetailScreen(
                                            item = item,
                                            use24HourTime = use24HourTime,
                                            dateFormatPreference = dateFormatPreference,
                                            onEditClick = { navController.navigate(EditReminder(id)) },
                                            onDeleteClick = {
                                                remindersViewModel.deleteReminder(id)
                                                navController.popBackStack()
                                            },
                                            onNavigateBack = { navController.popBackStack() }
                                        )
                                    }
                                }
                            }
                            composable("edit_reminder/{id}") { backStackEntry ->
                                val id = backStackEntry.arguments?.getString("id")
                                if (id != null) {
                                    val item = remindersUiState.reminders.find { it.id == id } ?: remindersViewModel.getReminderById(id)
                                    if (item != null) {
                                        EditReminderScreen(
                                            item = item,
                                            use24HourTime = use24HourTime,
                                            dateFormatPreference = dateFormatPreference,
                                            onSave = { req -> remindersViewModel.updateReminder(id, req) },
                                            onNavigateBack = { navController.popBackStack() }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                }
            }
        }
    }
}