package com.example.myapplication.ui

import androidx.compose.foundation.layout.*
import com.example.myapplication.R
import androidx.compose.ui.res.stringResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.myapplication.data.Announcement

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    userEmail: String,
    themePreference: String,
    onThemePreferenceChanged: (String) -> Unit,
    timeFormatPreference: String,
    onTimeFormatPreferenceChanged: (String) -> Unit,
    linkedDiscordId: String,
    discordLinkUiState: DiscordLinkUiState,
    onRequestDiscordLink: (String) -> Unit,
    onVerifyDiscordLink: (String) -> Unit,
    onUnlinkDiscord: () -> Unit,
    onResetDiscordLinkState: () -> Unit,
    announcements: List<Announcement>,
    onLogout: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val scrollState = rememberScrollState()
    val uriHandler = LocalUriHandler.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
                text = "Account",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )
            
            Text("Logged in as: ${if (userEmail.isNotBlank()) userEmail else "Guest"}", style = MaterialTheme.typography.bodyMedium)

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
                            text = "Theme",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        var themeDropdownExpanded by remember { mutableStateOf(false) }
                        Box {
                            TextButton(onClick = { themeDropdownExpanded = true }) {
                                Text(
                                    text = when (themePreference) {
                                        "LIGHT" -> "Light"
                                        "DARK" -> "Dark"
                                        "OLED" -> "OLED"
                                        else -> "System"
                                    }
                                )
                            }
                            DropdownMenu(
                                expanded = themeDropdownExpanded,
                                onDismissRequest = { themeDropdownExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("System Default") },
                                    onClick = {
                                        onThemePreferenceChanged("SYSTEM")
                                        themeDropdownExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Light") },
                                    onClick = {
                                        onThemePreferenceChanged("LIGHT")
                                        themeDropdownExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Dark") },
                                    onClick = {
                                        onThemePreferenceChanged("DARK")
                                        themeDropdownExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("OLED") },
                                    onClick = {
                                        onThemePreferenceChanged("OLED")
                                        themeDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Divider()
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "General",
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
                            text = "Time Format",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        var timeFormatDropdownExpanded by remember { mutableStateOf(false) }
                        Box {
                            TextButton(onClick = { timeFormatDropdownExpanded = true }) {
                                Text(
                                    text = when (timeFormatPreference) {
                                        "12_HOUR" -> "12-Hour"
                                        "24_HOUR" -> "24-Hour"
                                        else -> "System"
                                    }
                                )
                            }
                            DropdownMenu(
                                expanded = timeFormatDropdownExpanded,
                                onDismissRequest = { timeFormatDropdownExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("System Default") },
                                    onClick = {
                                        onTimeFormatPreferenceChanged("SYSTEM")
                                        timeFormatDropdownExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("12-Hour") },
                                    onClick = {
                                        onTimeFormatPreferenceChanged("12_HOUR")
                                        timeFormatDropdownExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("24-Hour") },
                                    onClick = {
                                        onTimeFormatPreferenceChanged("24_HOUR")
                                        timeFormatDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Divider()
            Spacer(modifier = Modifier.height(24.dp))

            Text("Discord Integration", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            if (linkedDiscordId.isNotBlank()) {
                Text("Linked to Discord ID: $linkedDiscordId", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onUnlinkDiscord,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Unlink Discord")
                }
            } else {
                Text("Link your Discord account to receive reminders and manage notes via the Shisho bot.", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))

                var inputDiscordId by remember { mutableStateOf("") }
                var inputPin by remember { mutableStateOf("") }

                when (discordLinkUiState) {
                    is DiscordLinkUiState.Idle -> {
                        OutlinedTextField(
                            value = inputDiscordId,
                            onValueChange = { inputDiscordId = it },
                            label = { Text("Discord User ID") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { onRequestDiscordLink(inputDiscordId) },
                            enabled = inputDiscordId.isNotBlank()
                        ) {
                            Text("Request PIN via DM")
                        }
                    }
                    is DiscordLinkUiState.Loading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    }
                    is DiscordLinkUiState.PinRequested -> {
                        Text("A PIN has been sent to your Discord account.", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = inputPin,
                            onValueChange = { inputPin = it },
                            label = { Text("Enter PIN") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row {
                            Button(
                                onClick = { onVerifyDiscordLink(inputPin) },
                                enabled = inputPin.isNotBlank() && !discordLinkUiState.isVerifying
                            ) {
                                Text("Verify PIN")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(onClick = onResetDiscordLinkState) {
                                Text("Cancel")
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
                            Text("Try Again")
                        }
                    }
                }
            }

            if (announcements.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Divider()
                Spacer(modifier = Modifier.height(24.dp))
                Text("Announcements", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
                onClick = onLogout,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Logout")
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
