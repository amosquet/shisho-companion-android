package com.shishocompanion.ui

import androidx.compose.foundation.layout.*
import com.shishocompanion.R
import androidx.compose.ui.res.stringResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.shishocompanion.data.Announcement

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    userEmail: String,
    themePreference: String,
    onThemePreferenceChanged: (String) -> Unit,
    dynamicColorPreference: Boolean = false,
    onDynamicColorPreferenceChanged: (Boolean) -> Unit = {},
    timeFormatPreference: String,
    onTimeFormatPreferenceChanged: (String) -> Unit,
    dateFormatPreference: String,
    onDateFormatPreferenceChanged: (String) -> Unit,
    languagePreference: String,
    onLanguagePreferenceChanged: (String) -> Unit,
    googleBooksApiKey: String,
    onGoogleBooksApiKeyChanged: (String) -> Unit,
    swipeActionPreference: String,
    onSwipeActionPreferenceChanged: (String) -> Unit,
    sendRemindersToDiscordPreference: Boolean,
    onSendRemindersToDiscordPreferenceChanged: (Boolean) -> Unit,
    linkedDiscordId: String,
    discordLinkUiState: DiscordLinkUiState,
    onRequestDiscordLink: (String) -> Unit,
    onVerifyDiscordLink: (String) -> Unit,
    onUnlinkDiscord: () -> Unit,
    onResetDiscordLinkState: () -> Unit,
    announcements: List<Announcement>,
    splitScreenPreference: Boolean,
    onSplitScreenPreferenceChanged: (Boolean) -> Unit,
    onLogout: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val scrollState = rememberScrollState()
    val uriHandler = LocalUriHandler.current
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showUnlinkDiscordDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.account),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )
            
            Text(stringResource(R.string.logged_in_as, if (userEmail.isNotBlank()) userEmail else stringResource(R.string.guest)), style = MaterialTheme.typography.bodyMedium)

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.theme),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        SettingsDropdown(
                            selectedText = when (themePreference) {
                                "LIGHT" -> stringResource(R.string.light)
                                "DARK" -> stringResource(R.string.dark)
                                "OLED" -> stringResource(R.string.oled)
                                else -> stringResource(R.string.system_default)
                            },
                            options = listOf(
                                "SYSTEM" to stringResource(R.string.system_default),
                                "LIGHT" to stringResource(R.string.light),
                                "DARK" to stringResource(R.string.dark),
                                "OLED" to stringResource(R.string.oled)
                            ),
                            onOptionSelected = onThemePreferenceChanged
                        )
                    }

                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                                Text(
                                    text = stringResource(R.string.dynamic_color),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = stringResource(R.string.dynamic_color_description),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                            Switch(
                                checked = dynamicColorPreference,
                                onCheckedChange = { onDynamicColorPreferenceChanged(it) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.general),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.time_format),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        SettingsDropdown(
                            selectedText = when (timeFormatPreference) {
                                "12_HOUR" -> stringResource(R.string.twelve_hour)
                                "24_HOUR" -> stringResource(R.string.twenty_four_hour)
                                else -> stringResource(R.string.system_default)
                            },
                            options = listOf(
                                "SYSTEM" to stringResource(R.string.system_default),
                                "12_HOUR" to stringResource(R.string.twelve_hour),
                                "24_HOUR" to stringResource(R.string.twenty_four_hour)
                            ),
                            onOptionSelected = onTimeFormatPreferenceChanged
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.date_format),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        SettingsDropdown(
                            selectedText = when (dateFormatPreference) {
                                "DD_MM_YYYY" -> stringResource(R.string.format_dd_mm_yyyy)
                                "MM_DD_YYYY" -> stringResource(R.string.format_mm_dd_yyyy)
                                "YYYY_MM_DD" -> stringResource(R.string.format_yyyy_mm_dd)
                                else -> stringResource(R.string.system_default)
                            },
                            options = listOf(
                                "SYSTEM" to stringResource(R.string.system_default),
                                "DD_MM_YYYY" to stringResource(R.string.format_dd_mm_yyyy),
                                "MM_DD_YYYY" to stringResource(R.string.format_mm_dd_yyyy),
                                "YYYY_MM_DD" to stringResource(R.string.format_yyyy_mm_dd)
                            ),
                            onOptionSelected = onDateFormatPreferenceChanged
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.language),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        SettingsDropdown(
                            selectedText = when (languagePreference) {
                                "en" -> stringResource(R.string.english)
                                "fr" -> stringResource(R.string.french)
                                "ja" -> stringResource(R.string.japanese)
                                else -> stringResource(R.string.system_default)
                            },
                            options = listOf(
                                "SYSTEM" to stringResource(R.string.system_default),
                                "en" to stringResource(R.string.english),
                                "fr" to stringResource(R.string.french),
                                "ja" to stringResource(R.string.japanese)
                            ),
                            onOptionSelected = onLanguagePreferenceChanged
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.split_screen_large_devices),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Switch(
                            checked = splitScreenPreference,
                            onCheckedChange = { onSplitScreenPreferenceChanged(it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.swipe_to_dismiss_action),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        SettingsDropdown(
                            selectedText = when (swipeActionPreference) {
                                "ARCHIVE" -> stringResource(R.string.archive)
                                "NONE" -> stringResource(R.string.disabled)
                                "DELETE" -> stringResource(R.string.delete)
                                else -> stringResource(R.string.delete)
                            },
                            options = listOf(
                                "DELETE" to stringResource(R.string.delete),
                                "ARCHIVE" to stringResource(R.string.archive),
                                "NONE" to stringResource(R.string.disabled)
                            ),
                            onOptionSelected = onSwipeActionPreferenceChanged
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.integrations),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.google_books_api_key),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = googleBooksApiKey,
                        onValueChange = onGoogleBooksApiKeyChanged,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text(stringResource(R.string.enter_api_key)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))

            Text(stringResource(R.string.discord_integration), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            if (userEmail.isBlank()) {
                Text(stringResource(R.string.please_login_discord), style = MaterialTheme.typography.bodyMedium)
            } else if (linkedDiscordId.isNotBlank()) {
                Text(stringResource(R.string.linked_to_discord, linkedDiscordId), style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.send_reminders_discord), style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = sendRemindersToDiscordPreference,
                        onCheckedChange = { onSendRemindersToDiscordPreferenceChanged(it) }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { showUnlinkDiscordDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.unlink_discord))
                }
            } else {
                Text(stringResource(R.string.link_discord_description), style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))

                var inputDiscordId by remember { mutableStateOf("") }
                var inputPin by remember { mutableStateOf("") }
                var showDiscordIdInfoDialog by remember { mutableStateOf(false) }

                when (discordLinkUiState) {
                    is DiscordLinkUiState.Idle -> {
                        OutlinedTextField(
                            value = inputDiscordId,
                            onValueChange = { inputDiscordId = it },
                            label = { Text(stringResource(R.string.discord_user_id)) },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            trailingIcon = {
                                IconButton(onClick = { showDiscordIdInfoDialog = true }) {
                                    Icon(Icons.Filled.Info, contentDescription = "What is a Discord ID?")
                                }
                            }
                        )
                        if (showDiscordIdInfoDialog) {
                            AlertDialog(
                                onDismissRequest = { showDiscordIdInfoDialog = false },
                                title = { Text(stringResource(R.string.discord_user_id)) },
                                text = { Text(stringResource(R.string.discord_id_help)) },
                                confirmButton = {
                                    TextButton(onClick = { showDiscordIdInfoDialog = false }) {
                                        Text(stringResource(R.string.ok))
                                    }
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { onRequestDiscordLink(inputDiscordId) },
                            enabled = inputDiscordId.isNotBlank()
                        ) {
                            Text(stringResource(R.string.request_pin_dm))
                        }
                    }
                    is DiscordLinkUiState.Loading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    }
                    is DiscordLinkUiState.PinRequested -> {
                        Text(stringResource(R.string.pin_sent), style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = inputPin,
                            onValueChange = { inputPin = it },
                            label = { Text(stringResource(R.string.enter_pin)) },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row {
                            Button(
                                onClick = { onVerifyDiscordLink(inputPin) },
                                enabled = inputPin.isNotBlank() && !discordLinkUiState.isVerifying
                            ) {
                                Text(stringResource(R.string.verify_pin))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(onClick = onResetDiscordLinkState) {
                                Text(stringResource(R.string.cancel))
                            }
                        }
                        if (discordLinkUiState.verifyError != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(discordLinkUiState.verifyError, color = MaterialTheme.colorScheme.error)
                        }
                    }
                    is DiscordLinkUiState.Success -> {
                        LaunchedEffect(Unit) {
                            onResetDiscordLinkState()
                        }
                    }
                    is DiscordLinkUiState.Error -> {
                        Text(discordLinkUiState.message, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = onResetDiscordLinkState) {
                            Text(stringResource(R.string.try_again))
                        }
                    }
                }
            }

            if (announcements.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(24.dp))
                Text(stringResource(R.string.announcements), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                announcements.forEach { ann ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(ann.timestamp, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(ann.message, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { showLogoutDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.logout))
            }

            if (showLogoutDialog) {
                AlertDialog(
                    onDismissRequest = { showLogoutDialog = false },
                    title = { Text(stringResource(R.string.confirm_logout)) },
                    text = { Text(stringResource(R.string.logout_confirm)) },
                    confirmButton = {
                        Button(
                            onClick = {
                                showLogoutDialog = false
                                onLogout()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text(stringResource(R.string.logout))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showLogoutDialog = false }) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                )
            }

            if (showUnlinkDiscordDialog) {
                AlertDialog(
                    onDismissRequest = { showUnlinkDiscordDialog = false },
                    title = { Text(stringResource(R.string.confirm_unlink)) },
                    text = { Text(stringResource(R.string.unlink_discord_confirm)) },
                    confirmButton = {
                        Button(
                            onClick = {
                                showUnlinkDiscordDialog = false
                                onUnlinkDiscord()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text(stringResource(R.string.unlink))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showUnlinkDiscordDialog = false }) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> SettingsDropdown(
    selectedText: String,
    options: List<Pair<T, String>>,
    onOptionSelected: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        TextButton(
            onClick = { expanded = true },
            modifier = Modifier.menuAnchor(
                type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                enabled = true
            )
        ) {
            Text(text = selectedText)
        }
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { (value, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onOptionSelected(value)
                        expanded = false
                    }
                )
            }
        }
    }
}
