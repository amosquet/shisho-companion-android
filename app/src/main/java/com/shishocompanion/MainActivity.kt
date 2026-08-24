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
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.launch
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
import android.content.SharedPreferences
import javax.inject.Inject
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

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var sharedPrefs: SharedPreferences

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
            val dynamicColorPreference by themeViewModel.dynamicColorPreference.collectAsState()
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
                                                    navController.navigate(AddNote)
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.add_reminder)) },
                                                onClick = {
                                                    expanded = false
                                                    navController.navigate(AddReminder)
                                                }
                                            )
                                        }
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
                                    val coroutineScope = rememberCoroutineScope()
                                    val splitScreenPreference by themeViewModel.splitScreenPreference.collectAsState()
                                    val scaffoldNavigator = rememberListDetailPaneScaffoldNavigator<String>(
                                        scaffoldDirective = calculatePaneScaffoldDirective(
                                            currentWindowAdaptiveInfo()
                                        ).copy(maxHorizontalPartitions = if (splitScreenPreference) 2 else 1)
                                    )

                                    NavigableListDetailPaneScaffold(
                                        navigator = scaffoldNavigator,
                                        listPane = {
                                            AnimatedPane {
                                                ReadingListScreen(
                                                    uiState = listUiState,
                                                    hasUnreadAnnouncements = hasUnreadAnnouncements,
                                                    onSyncClick = { listViewModel.syncReadingList() },
                                                    onItemClick = { id ->
                                                        coroutineScope.launch {
                                                            scaffoldNavigator.navigateTo(ListDetailPaneScaffoldRole.Detail, id)
                                                        }
                                                    },
                                                    onSettingsClick = { navController.navigate(Settings) }
                                                )
                                            }
                                        },
                                        detailPane = {
                                            AnimatedPane {
                                                val selectedId = scaffoldNavigator.currentDestination?.contentKey
                                                if (selectedId != null) {
                                                    val item = (listUiState as? ReadingListUiState.Success)?.items?.find { it.id == selectedId }
                                                        ?: listViewModel.getItemById(selectedId)
                                                    if (item != null) {
                                                        val isDualPane = scaffoldNavigator.scaffoldValue[ListDetailPaneScaffoldRole.List] == PaneAdaptedValue.Expanded &&
                                                                scaffoldNavigator.scaffoldValue[ListDetailPaneScaffoldRole.Detail] == PaneAdaptedValue.Expanded
                                                        BookDetailScreen(
                                                            item = item,
                                                            onEditClick = { navController.navigate(Edit(selectedId)) },
                                                            onDeleteClick = {
                                                                listViewModel.deleteItem(selectedId)
                                                                coroutineScope.launch {
                                                                    scaffoldNavigator.navigateBack()
                                                                }
                                                            },
                                                            onNavigateBack = {
                                                                coroutineScope.launch {
                                                                    scaffoldNavigator.navigateBack()
                                                                }
                                                            },
                                                            isSplitScreen = isDualPane,
                                                            dateFormatPreference = dateFormatPreference
                                                        )
                                                    } else {
                                                        Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                                                            Text(stringResource(R.string.list_label))
                                                        }
                                                    }
                                                } else {
                                                    Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                                                        Text(stringResource(R.string.list_label))
                                                    }
                                                }
                                            }
                                        }
                                    )
                                }
                                composable<Notes> {
                                    val coroutineScope = rememberCoroutineScope()
                                    val splitScreenPreference by themeViewModel.splitScreenPreference.collectAsState()
                                    val scaffoldNavigator = rememberListDetailPaneScaffoldNavigator<String>(
                                        scaffoldDirective = calculatePaneScaffoldDirective(
                                            currentWindowAdaptiveInfo()
                                        ).copy(maxHorizontalPartitions = if (splitScreenPreference) 2 else 1)
                                    )
                                    val selectedNoteIds by notesViewModel.selectedNoteIds.collectAsState()

                                    NavigableListDetailPaneScaffold(
                                        navigator = scaffoldNavigator,
                                        listPane = {
                                            AnimatedPane {
                                                NotesListScreen(
                                                    uiState = notesUiState,
                                                    hasUnreadAnnouncements = hasUnreadAnnouncements,
                                                    selectedNoteIds = selectedNoteIds,
                                                    onSyncClick = { notesViewModel.syncNotes() },
                                                    onItemClick = { id ->
                                                        coroutineScope.launch {
                                                            scaffoldNavigator.navigateTo(ListDetailPaneScaffoldRole.Detail, id)
                                                        }
                                                    },
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
                                                val selectedId = scaffoldNavigator.currentDestination?.contentKey
                                                if (selectedId != null) {
                                                    val item = (notesUiState as? NotesUiState.Success)?.items?.find { it.id == selectedId }
                                                        ?: notesViewModel.getNoteById(selectedId)
                                                    if (item != null) {
                                                        val isDualPane = scaffoldNavigator.scaffoldValue[ListDetailPaneScaffoldRole.List] == PaneAdaptedValue.Expanded &&
                                                                scaffoldNavigator.scaffoldValue[ListDetailPaneScaffoldRole.Detail] == PaneAdaptedValue.Expanded
                                                        NoteDetailScreen(
                                                            item = item,
                                                            onEditClick = { navController.navigate(EditNote(selectedId)) },
                                                            onDeleteClick = {
                                                                notesViewModel.deleteNote(selectedId)
                                                                coroutineScope.launch {
                                                                    scaffoldNavigator.navigateBack()
                                                                }
                                                            },
                                                            onArchiveClick = {
                                                                notesViewModel.archiveNote(selectedId)
                                                                coroutineScope.launch {
                                                                    scaffoldNavigator.navigateBack()
                                                                }
                                                            },
                                                            onTextUpdate = { newText ->
                                                                val req = com.shishocompanion.data.CreateNoteRequest(
                                                                    owner = item.owner,
                                                                    title = item.title,
                                                                    text = newText,
                                                                    attachment = item.attachment,
                                                                    archived = item.archived
                                                                )
                                                                notesViewModel.updateNote(selectedId, req)
                                                            },
                                                            onNavigateBack = {
                                                                coroutineScope.launch {
                                                                    scaffoldNavigator.navigateBack()
                                                                }
                                                            },
                                                            isSplitScreen = isDualPane,
                                                            dateFormatPreference = dateFormatPreference
                                                        )
                                                    } else {
                                                        Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                                                            Text(stringResource(R.string.notes))
                                                        }
                                                    }
                                                } else {
                                                    Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                                                        Text(stringResource(R.string.notes))
                                                    }
                                                }
                                            }
                                        }
                                    )
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
                                composable<Detail> { backStackEntry ->
                                    val route = backStackEntry.toRoute<Detail>()
                                    val id = route.id
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
                                composable<Edit> { backStackEntry ->
                                    val route = backStackEntry.toRoute<Edit>()
                                    val id = route.id
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
                                composable<AddNote> {
                                    val userId = sharedPrefs.getString("USER_ID", "") ?: ""
                                    AddNoteScreen(
                                        owner = userId,
                                        onSave = { req -> notesViewModel.addNote(req) },
                                        onNavigateBack = { navController.popBackStack() }
                                    )
                                }
                                composable<NoteDetail> { backStackEntry ->
                                    val route = backStackEntry.toRoute<NoteDetail>()
                                    val id = route.id
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
                                                    owner = item.owner,
                                                    title = item.title,
                                                    text = newText,
                                                    attachment = item.attachment,
                                                    archived = item.archived
                                                )
                                                notesViewModel.updateNote(id, req)
                                            },
                                            onNavigateBack = { navController.popBackStack() },
                                            dateFormatPreference = dateFormatPreference
                                        )
                                    }
                                }
                                composable<EditNote> { backStackEntry ->
                                    val route = backStackEntry.toRoute<EditNote>()
                                    val id = route.id
                                    val item = (notesUiState as? NotesUiState.Success)?.items?.find { it.id == id } ?: notesViewModel.getNoteById(id)
                                    if (item != null) {
                                        EditNoteScreen(
                                            item = item,
                                            onSave = { req -> notesViewModel.updateNote(id, req) },
                                            onNavigateBack = { navController.popBackStack() }
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
                                    val coroutineScope = rememberCoroutineScope()
                                    val splitScreenPreference by themeViewModel.splitScreenPreference.collectAsState()
                                    val scaffoldNavigator = rememberListDetailPaneScaffoldNavigator<String>(
                                        scaffoldDirective = calculatePaneScaffoldDirective(
                                            currentWindowAdaptiveInfo()
                                        ).copy(maxHorizontalPartitions = if (splitScreenPreference) 2 else 1)
                                    )
                                    val selectedReminderIds by remindersViewModel.selectedReminderIds.collectAsState()

                                    NavigableListDetailPaneScaffold(
                                        navigator = scaffoldNavigator,
                                        listPane = {
                                            AnimatedPane {
                                                RemindersListScreen(
                                                    uiState = remindersUiState,
                                                    hasUnreadAnnouncements = hasUnreadAnnouncements,
                                                    selectedReminderIds = selectedReminderIds,
                                                    use24HourTime = use24HourTime,
                                                    dateFormatPreference = dateFormatPreference,
                                                    onSyncClick = { remindersViewModel.syncReminders() },
                                                    onItemClick = { id ->
                                                        coroutineScope.launch {
                                                            scaffoldNavigator.navigateTo(ListDetailPaneScaffoldRole.Detail, id)
                                                        }
                                                    },
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
                                                val selectedId = scaffoldNavigator.currentDestination?.contentKey
                                                if (selectedId != null) {
                                                    val item = remindersUiState.reminders.find { it.id == selectedId }
                                                        ?: remindersViewModel.getReminderById(selectedId)
                                                    if (item != null) {
                                                        val isDualPane = scaffoldNavigator.scaffoldValue[ListDetailPaneScaffoldRole.List] == PaneAdaptedValue.Expanded &&
                                                                scaffoldNavigator.scaffoldValue[ListDetailPaneScaffoldRole.Detail] == PaneAdaptedValue.Expanded
                                                        ReminderDetailScreen(
                                                            item = item,
                                                            use24HourTime = use24HourTime,
                                                            dateFormatPreference = dateFormatPreference,
                                                            onEditClick = { navController.navigate(EditReminder(selectedId)) },
                                                            onDeleteClick = {
                                                                remindersViewModel.deleteReminder(selectedId)
                                                                coroutineScope.launch {
                                                                    scaffoldNavigator.navigateBack()
                                                                }
                                                            },
                                                            onNavigateBack = {
                                                                coroutineScope.launch {
                                                                    scaffoldNavigator.navigateBack()
                                                                }
                                                            },
                                                            isSplitScreen = isDualPane
                                                        )
                                                    } else {
                                                        Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                                                            Text(stringResource(R.string.reminders))
                                                        }
                                                    }
                                                } else {
                                                    Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                                                        Text(stringResource(R.string.reminders))
                                                    }
                                                }
                                            }
                                        }
                                    )
                                }
                                composable<ArchivedReminders> {
                                    val coroutineScope = rememberCoroutineScope()
                                    val splitScreenPreference by themeViewModel.splitScreenPreference.collectAsState()
                                    val scaffoldNavigator = rememberListDetailPaneScaffoldNavigator<String>(
                                        scaffoldDirective = calculatePaneScaffoldDirective(
                                            currentWindowAdaptiveInfo()
                                        ).copy(maxHorizontalPartitions = if (splitScreenPreference) 2 else 1)
                                    )
                                    val selectedReminderIds by remindersViewModel.selectedReminderIds.collectAsState()

                                    NavigableListDetailPaneScaffold(
                                        navigator = scaffoldNavigator,
                                        listPane = {
                                            AnimatedPane {
                                                ArchivedRemindersScreen(
                                                    uiState = remindersUiState,
                                                    selectedReminderIds = selectedReminderIds,
                                                    use24HourTime = use24HourTime,
                                                    dateFormatPreference = dateFormatPreference,
                                                    onNavigateBack = { navController.popBackStack() },
                                                    onItemClick = { id ->
                                                        coroutineScope.launch {
                                                            scaffoldNavigator.navigateTo(ListDetailPaneScaffoldRole.Detail, id)
                                                        }
                                                    },
                                                    onToggleSelection = { id -> remindersViewModel.toggleReminderSelection(id) },
                                                    onClearSelection = { remindersViewModel.clearReminderSelection() },
                                                    onDeleteSelected = { remindersViewModel.deleteSelectedReminders() }
                                                )
                                            }
                                        },
                                        detailPane = {
                                            AnimatedPane {
                                                val selectedId = scaffoldNavigator.currentDestination?.contentKey
                                                if (selectedId != null) {
                                                    val item = remindersUiState.reminders.find { it.id == selectedId }
                                                        ?: remindersViewModel.getReminderById(selectedId)
                                                    if (item != null) {
                                                        val isDualPane = scaffoldNavigator.scaffoldValue[ListDetailPaneScaffoldRole.List] == PaneAdaptedValue.Expanded &&
                                                                scaffoldNavigator.scaffoldValue[ListDetailPaneScaffoldRole.Detail] == PaneAdaptedValue.Expanded
                                                        ReminderDetailScreen(
                                                            item = item,
                                                            use24HourTime = use24HourTime,
                                                            dateFormatPreference = dateFormatPreference,
                                                            onEditClick = { navController.navigate(EditReminder(selectedId)) },
                                                            onDeleteClick = {
                                                                remindersViewModel.deleteReminder(selectedId)
                                                                coroutineScope.launch {
                                                                    scaffoldNavigator.navigateBack()
                                                                }
                                                            },
                                                            onNavigateBack = {
                                                                coroutineScope.launch {
                                                                    scaffoldNavigator.navigateBack()
                                                                }
                                                            },
                                                            isSplitScreen = isDualPane
                                                        )
                                                    } else {
                                                        Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                                                            Text(stringResource(R.string.reminders))
                                                        }
                                                    }
                                                } else {
                                                    Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                                                        Text(stringResource(R.string.reminders))
                                                    }
                                                }
                                            }
                                        }
                                    )
                                }
                                composable<ArchivedNotes> {
                                    val coroutineScope = rememberCoroutineScope()
                                    val splitScreenPreference by themeViewModel.splitScreenPreference.collectAsState()
                                    val scaffoldNavigator = rememberListDetailPaneScaffoldNavigator<String>(
                                        scaffoldDirective = calculatePaneScaffoldDirective(
                                            currentWindowAdaptiveInfo()
                                        ).copy(maxHorizontalPartitions = if (splitScreenPreference) 2 else 1)
                                    )
                                    val selectedNoteIds by notesViewModel.selectedNoteIds.collectAsState()

                                    NavigableListDetailPaneScaffold(
                                        navigator = scaffoldNavigator,
                                        listPane = {
                                            AnimatedPane {
                                                ArchivedNotesScreen(
                                                    uiState = notesUiState,
                                                    selectedNoteIds = selectedNoteIds,
                                                    onNavigateBack = { navController.popBackStack() },
                                                    onItemClick = { id ->
                                                        coroutineScope.launch {
                                                            scaffoldNavigator.navigateTo(ListDetailPaneScaffoldRole.Detail, id)
                                                        }
                                                    },
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
                                                val selectedId = scaffoldNavigator.currentDestination?.contentKey
                                                if (selectedId != null) {
                                                    val item = (notesUiState as? NotesUiState.Success)?.items?.find { it.id == selectedId }
                                                        ?: notesViewModel.getNoteById(selectedId)
                                                    if (item != null) {
                                                        val isDualPane = scaffoldNavigator.scaffoldValue[ListDetailPaneScaffoldRole.List] == PaneAdaptedValue.Expanded &&
                                                                scaffoldNavigator.scaffoldValue[ListDetailPaneScaffoldRole.Detail] == PaneAdaptedValue.Expanded
                                                        NoteDetailScreen(
                                                            item = item,
                                                            onEditClick = { navController.navigate(EditNote(selectedId)) },
                                                            onDeleteClick = {
                                                                notesViewModel.deleteNote(selectedId)
                                                                coroutineScope.launch {
                                                                    scaffoldNavigator.navigateBack()
                                                                }
                                                            },
                                                            onArchiveClick = {
                                                                notesViewModel.archiveNote(selectedId)
                                                                coroutineScope.launch {
                                                                    scaffoldNavigator.navigateBack()
                                                                }
                                                            },
                                                            onTextUpdate = { newText ->
                                                                val req = com.shishocompanion.data.CreateNoteRequest(
                                                                    owner = item.owner,
                                                                    title = item.title,
                                                                    text = newText,
                                                                    attachment = item.attachment,
                                                                    archived = item.archived
                                                                )
                                                                notesViewModel.updateNote(selectedId, req)
                                                            },
                                                            onNavigateBack = {
                                                                coroutineScope.launch {
                                                                    scaffoldNavigator.navigateBack()
                                                                }
                                                            },
                                                            isSplitScreen = isDualPane,
                                                            dateFormatPreference = dateFormatPreference
                                                        )
                                                    } else {
                                                        Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                                                            Text(stringResource(R.string.notes))
                                                        }
                                                    }
                                                } else {
                                                    Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                                                        Text(stringResource(R.string.notes))
                                                    }
                                                }
                                            }
                                        }
                                    )
                                }
                                composable<AddReminder> {
                                    val userId = sharedPrefs.getString("USER_ID", "") ?: ""
                                    AddReminderScreen(
                                        userId = userId,
                                        use24HourTime = use24HourTime,
                                        dateFormatPreference = dateFormatPreference,
                                        onSave = { req -> remindersViewModel.addReminder(req) },
                                        onNavigateBack = { navController.popBackStack() }
                                    )
                                }
                                composable<ReminderDetail> { backStackEntry ->
                                    val route = backStackEntry.toRoute<ReminderDetail>()
                                    val id = route.id
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
                                composable<EditReminder> { backStackEntry ->
                                    val route = backStackEntry.toRoute<EditReminder>()
                                    val id = route.id
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