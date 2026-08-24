package com.shishocompanion.navigation

import kotlinx.serialization.Serializable

@Serializable
object Login

@Serializable
object ReadingList

@Serializable
object Notes

@Serializable
object Reminders

@Serializable
object Add

@Serializable
object AddNote

@Serializable
object AddReminder



@Serializable
data class Edit(val id: String)

@Serializable
data class EditNote(val id: String)

@Serializable
data class EditReminder(val id: String)

@Serializable
object Settings

@Serializable
object ArchivedNotes

@Serializable
object ArchivedReminders
