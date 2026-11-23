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
data class AddNote(val bookId: String = "")

@Serializable
data class AddReminder(val bookId: String = "", val defaultText: String = "")



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
