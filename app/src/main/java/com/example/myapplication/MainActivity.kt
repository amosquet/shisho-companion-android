package com.example.myapplication

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Notifications
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
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.myapplication.data.AppDatabase
import com.example.myapplication.data.NoteRepository
import com.example.myapplication.data.ReadingListRepository
import com.example.myapplication.data.ReminderRepository
import com.example.myapplication.ui.AddNoteScreen
import com.example.myapplication.ui.AddReadingListItemScreen
import com.example.myapplication.ui.AddReminderScreen
import com.example.myapplication.ui.ArchivedNotesScreen
import com.example.myapplication.ui.ArchivedRemindersScreen
import com.example.myapplication.ui.BookDetailScreen
import com.example.myapplication.ui.EditNoteScreen
import com.example.myapplication.ui.EditReadingListItemScreen
import com.example.myapplication.ui.EditReminderScreen
import com.example.myapplication.ui.LoginScreen
import com.example.myapplication.ui.LoginViewModel
import com.example.myapplication.ui.NoteDetailScreen
import com.example.myapplication.ui.NotesListScreen
import com.example.myapplication.ui.NotesUiState
import com.example.myapplication.ui.NotesViewModel
import com.example.myapplication.ui.ReadingListScreen
import com.example.myapplication.ui.ReadingListUiState
import com.example.myapplication.ui.ReadingListViewModel
import com.example.myapplication.ui.ReminderDetailScreen
import com.example.myapplication.ui.RemindersListScreen
import com.example.myapplication.ui.RemindersViewModel
import com.example.myapplication.ui.SettingsScreen
import com.example.myapplication.ui.SettingsViewModel
import com.example.myapplication.ui.DiscordLinkUiState
import com.example.myapplication.ui.ThemeViewModel
import com.example.myapplication.ui.AnnouncementsViewModel
import com.example.myapplication.ui.theme.MyApplicationTheme
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

private fun getTabIndex(route: String?): Int {
    return when (route?.substringBefore("/")) {
        "list" -> 0
        "notes" -> 1
        "reminders" -> 2
        else -> -1
    }
}

class MainActivity : ComponentActivity() {
    private val intentAction = MutableStateFlow<String?>(null)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intentAction.value = intent.action
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        com.example.myapplication.notifications.NotificationHelper.createNotificationChannel(this)

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
        
        val oldPrefs = applicationContext.getSharedPreferences("app_prefs", MODE_PRIVATE)
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
        
        if (oldPrefs.all.isNotEmpty()) {
            val editor = sharedPrefs.edit()
            oldPrefs.all.forEach { (key, value) ->
                when (value) {
                    is String -> editor.putString(key, value)
                    is Boolean -> editor.putBoolean(key, value)
                    is Int -> editor.putInt(key, value)
                    is Float -> editor.putFloat(key, value)
                    is Long -> editor.putLong(key, value)
                    is Set<*> -> @Suppress("UNCHECKED_CAST") editor.putStringSet(key, value as Set<String>)
                }
            }
            editor.apply()
            oldPrefs.edit().clear().apply()
        }
        
        val viewModelFactory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(ReadingListViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return ReadingListViewModel(repository) as T
                }
                if (modelClass.isAssignableFrom(NotesViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return NotesViewModel(application, noteRepository) as T
                }
                if (modelClass.isAssignableFrom(RemindersViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return RemindersViewModel(application, reminderRepository) as T
                }
                if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return LoginViewModel(repository, noteRepository, reminderRepository, database, sharedPrefs) as T
                }
                if (modelClass.isAssignableFrom(ThemeViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return ThemeViewModel(repository, sharedPrefs) as T
                }
                if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return SettingsViewModel(repository, sharedPrefs) as T
                }
                if (modelClass.isAssignableFrom(AnnouncementsViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return AnnouncementsViewModel(httpClient, sharedPrefs) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }

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
            val themeViewModel: ThemeViewModel = viewModel(factory = viewModelFactory)
            val themePreference by themeViewModel.themePreference.collectAsState()
            val timeFormatPreference by themeViewModel.timeFormatPreference.collectAsState()
            val sendRemindersToDiscordPreference by themeViewModel.sendRemindersToDiscordPreference.collectAsState()
            val context = androidx.compose.ui.platform.LocalContext.current
            val use24HourTime = when (timeFormatPreference) {
                "24_HOUR" -> true
                "12_HOUR" -> false
                else -> android.text.format.DateFormat.is24HourFormat(context)
            }

            MyApplicationTheme(themePreference = themePreference) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    
                    val loginViewModel: LoginViewModel = viewModel(factory = viewModelFactory)
                    val loginUiState by loginViewModel.uiState.collectAsState()

                    val listViewModel: ReadingListViewModel = viewModel(factory = viewModelFactory)
                    val listUiState by listViewModel.uiState.collectAsState()

                    val notesViewModel: NotesViewModel = viewModel(factory = viewModelFactory)
                    val notesUiState by notesViewModel.uiState.collectAsState()

                    val remindersViewModel: RemindersViewModel = viewModel(factory = viewModelFactory)
                    val remindersUiState by remindersViewModel.uiState.collectAsState()
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination?.route
                    
                    val currentIntentAction by intentAction.collectAsState()
                    LaunchedEffect(currentIntentAction, currentDestination) {
                        if (currentIntentAction == "com.example.myapplication.ADD_NOTE") {
                            if (currentDestination != null && currentDestination != "login") {
                                navController.navigate("notes") {
                                    popUpTo("list") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                                navController.navigate("add_note")
                                intentAction.value = null
                            }
                        }
                    }
                    
                    val announcementsViewModel: AnnouncementsViewModel = viewModel(factory = viewModelFactory)
                    val hasUnreadAnnouncements by announcementsViewModel.hasUnreadAnnouncement.collectAsState()
                    val announcements by announcementsViewModel.announcements.collectAsState()

                    Scaffold(
                        floatingActionButton = {
                            if (currentDestination in listOf("list", "notes", "reminders")) {
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
                                                navController.navigate("add")
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.add_note)) },
                                            onClick = {
                                                expanded = false
                                                navController.navigate("add_note")
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.add_reminder)) },
                                            onClick = {
                                                expanded = false
                                                navController.navigate("add_reminder")
                                            }
                                        )
                                    }
                                }
                            }
                        },
                        bottomBar = {
                            if (currentDestination in listOf("list", "notes", "reminders")) {
                                NavigationBar {
                                    NavigationBarItem(
                                        icon = { Icon(Icons.Filled.List, contentDescription = stringResource(R.string.reading_list)) },
                                        label = { Text(stringResource(R.string.list_label)) },
                                        selected = currentDestination == "list",
                                        onClick = {
                                            navController.navigate("list") {
                                                popUpTo("list") { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    )
                                    NavigationBarItem(
                                        icon = { Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.notes)) },
                                        label = { Text(stringResource(R.string.notes)) },
                                        selected = currentDestination == "notes",
                                        onClick = {
                                            navController.navigate("notes") {
                                                popUpTo("list") { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    )
                                    NavigationBarItem(
                                        icon = { Icon(Icons.Filled.Notifications, contentDescription = stringResource(R.string.reminders)) },
                                        label = { Text(stringResource(R.string.reminders)) },
                                        selected = currentDestination == "reminders",
                                        onClick = {
                                            navController.navigate("reminders") {
                                                popUpTo("list") { saveState = true }
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
                            startDestination = "login", 
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
                            composable("login") {
                                LoginScreen(
                                    uiState = loginUiState,
                                    onLogin = { email, pin -> loginViewModel.login(email, pin) },
                                    onRegister = { email, pin -> loginViewModel.register(email, pin) },
                                    onContinueAsGuest = { loginViewModel.continueAsGuest() },
                                    onNavigateToList = { 
                                        themeViewModel.reloadPreferences()
                                        
                                        navController.navigate("notes") {
                                            popUpTo("login") { inclusive = true }
                                        }
                                        if (intentAction.value == "com.example.myapplication.ADD_NOTE") {
                                            intentAction.value = null
                                            navController.navigate("add_note")
                                        }
                                        listViewModel.syncReadingList()
                                        notesViewModel.syncNotes()
                                        remindersViewModel.syncReminders()
                                    }
                                )
                            }
                            composable("list") {
                                ReadingListScreen(
                                    uiState = listUiState,
                                    hasUnreadAnnouncements = hasUnreadAnnouncements,
                                    onSyncClick = { listViewModel.syncReadingList() },
                                    onItemClick = { id -> navController.navigate("detail/$id") },
                                    onSettingsClick = { navController.navigate("settings") }
                                )
                            }
                            composable("notes") {
                                val selectedNoteIds by notesViewModel.selectedNoteIds.collectAsState()
                                NotesListScreen(
                                    uiState = notesUiState,
                                    hasUnreadAnnouncements = hasUnreadAnnouncements,
                                    selectedNoteIds = selectedNoteIds,
                                    onSyncClick = { notesViewModel.syncNotes() },
                                    onItemClick = { id -> navController.navigate("note_detail/$id") },
                                    onSettingsClick = { navController.navigate("settings") },
                                    onArchiveClick = { navController.navigate("archived_notes") },
                                    onToggleSelection = { id -> notesViewModel.toggleNoteSelection(id) },
                                    onClearSelection = { notesViewModel.clearNoteSelection() },
                                    onDeleteSelected = { notesViewModel.deleteSelectedNotes() },
                                    onArchiveSelected = { notesViewModel.archiveSelectedNotes() }
                                )
                            }
                            composable("add") {
                                AddReadingListItemScreen(
                                    onSave = { req ->
                                        listViewModel.addItem(req)
                                    },
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }
                            composable("detail/{id}") { backStackEntry ->
                                val id = backStackEntry.arguments?.getString("id")
                                if (id != null) {
                                    val item = (listUiState as? ReadingListUiState.Success)?.items?.find { it.id == id } ?: listViewModel.getItemById(id)
                                    if (item != null) {
                                        BookDetailScreen(
                                            item = item,
                                            onEditClick = { navController.navigate("edit/$id") },
                                            onNavigateBack = { navController.popBackStack() }
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
                            composable("add_note") {
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
                                            onEditClick = { navController.navigate("edit_note/$id") },
                                            onDeleteClick = {
                                                notesViewModel.deleteNote(id)
                                                navController.popBackStack()
                                            },
                                            onArchiveClick = {
                                                notesViewModel.archiveNote(id)
                                                navController.popBackStack()
                                            },
                                            onTextUpdate = { newText ->
                                                val req = com.example.myapplication.data.CreateNoteRequest(
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
                            composable("settings") {
                                val settingsViewModel: SettingsViewModel = viewModel(factory = viewModelFactory)
                                val discordLinkUiState by settingsViewModel.uiState.collectAsState()
                                val linkedDiscordId by settingsViewModel.linkedDiscordId.collectAsState()
                                SettingsScreen(
                                    userEmail = sharedPrefs.getString("USER_EMAIL", "") ?: "",
                                    themePreference = themePreference,
                                    onThemePreferenceChanged = { themeViewModel.setThemePreference(it) },
                                    timeFormatPreference = timeFormatPreference,
                                    onTimeFormatPreferenceChanged = { themeViewModel.setTimeFormatPreference(it) },
                                    sendRemindersToDiscordPreference = sendRemindersToDiscordPreference,
                                    onSendRemindersToDiscordPreferenceChanged = { themeViewModel.setSendRemindersToDiscordPreference(it) },
                                    linkedDiscordId = linkedDiscordId,
                                    discordLinkUiState = discordLinkUiState,
                                    onRequestDiscordLink = { id -> settingsViewModel.requestDiscordPin(id) },
                                    onVerifyDiscordLink = { pin -> settingsViewModel.verifyDiscordPin(linkedDiscordId.ifBlank { (discordLinkUiState as? DiscordLinkUiState.PinRequested)?.discordId ?: "" }, pin) },
                                    onUnlinkDiscord = { settingsViewModel.unlinkDiscord() },
                                    onResetDiscordLinkState = { settingsViewModel.resetState() },
                                    announcements = announcements,
                                    onLogout = {
                                        loginViewModel.logout()
                                        navController.navigate("login") {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    },
                                    onNavigateBack = { 
                                        settingsViewModel.resetState()
                                        navController.popBackStack() 
                                    }
                                )
                            }
                            composable("reminders") {
                                val selectedReminderIds by remindersViewModel.selectedReminderIds.collectAsState()
                                RemindersListScreen(
                                    uiState = remindersUiState,
                                    hasUnreadAnnouncements = hasUnreadAnnouncements,
                                    selectedReminderIds = selectedReminderIds,
                                    use24HourTime = use24HourTime,
                                    onSyncClick = { remindersViewModel.syncReminders() },
                                    onItemClick = { id -> navController.navigate("reminder_detail/$id") },
                                    onSettingsClick = { navController.navigate("settings") },
                                    onArchiveClick = { navController.navigate("archived_reminders") },
                                    onToggleSelection = { id -> remindersViewModel.toggleReminderSelection(id) },
                                    onClearSelection = { remindersViewModel.clearReminderSelection() },
                                    onDeleteSelected = { remindersViewModel.deleteSelectedReminders() }
                                )
                            }
                            composable("archived_reminders") {
                                val selectedReminderIds by remindersViewModel.selectedReminderIds.collectAsState()
                                ArchivedRemindersScreen(
                                    uiState = remindersUiState,
                                    selectedReminderIds = selectedReminderIds,
                                    use24HourTime = use24HourTime,
                                    onNavigateBack = { navController.popBackStack() },
                                    onItemClick = { id -> navController.navigate("reminder_detail/$id") },
                                    onToggleSelection = { id -> remindersViewModel.toggleReminderSelection(id) },
                                    onClearSelection = { remindersViewModel.clearReminderSelection() },
                                    onDeleteSelected = { remindersViewModel.deleteSelectedReminders() }
                                )
                            }
                            composable("archived_notes") {
                                val selectedNoteIds by notesViewModel.selectedNoteIds.collectAsState()
                                ArchivedNotesScreen(
                                    uiState = notesUiState,
                                    selectedNoteIds = selectedNoteIds,
                                    onNavigateBack = { navController.popBackStack() },
                                    onItemClick = { id -> navController.navigate("note_detail/$id") },
                                    onToggleSelection = { id -> notesViewModel.toggleNoteSelection(id) },
                                    onClearSelection = { notesViewModel.clearNoteSelection() },
                                    onDeleteSelected = { notesViewModel.deleteSelectedNotes() },
                                    onUnarchiveSelected = { notesViewModel.unarchiveSelectedNotes() }
                                )
                            }
                            composable("add_reminder") {
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
                                            onEditClick = { navController.navigate("edit_reminder/$id") },
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