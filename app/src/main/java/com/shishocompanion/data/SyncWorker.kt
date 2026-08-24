package com.shishocompanion.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SyncWorkerEntryPoint {
        fun noteRepository(): NoteRepository
        fun reminderRepository(): ReminderRepository
        fun readingListRepository(): ReadingListRepository
    }

    override suspend fun doWork(): Result {
        return try {
            val hiltEntryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                SyncWorkerEntryPoint::class.java
            )
            
            val noteRepository = hiltEntryPoint.noteRepository()
            noteRepository.sync()
            
            // Also sync others to be thorough
            val reminderRepository = hiltEntryPoint.reminderRepository()
            reminderRepository.sync()
            
            val readingListRepository = hiltEntryPoint.readingListRepository()
            readingListRepository.sync()

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
