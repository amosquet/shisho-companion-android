package com.shishocompanion.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import net.zetetic.database.sqlcipher.SQLiteDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.security.SecureRandom
import android.util.Base64

@Database(entities = [ReadingListEntity::class, NoteEntity::class, ReminderEntity::class], version = 10, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun readingListDao(): ReadingListDao
    abstract fun noteDao(): NoteDao
    abstract fun reminderDao(): ReminderDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE reminders ADD COLUMN isPendingSync INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE reading_list ADD COLUMN completed TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes RENAME COLUMN userId TO owner")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val masterKey = MasterKey.Builder(context.applicationContext)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
                    
                val sharedPrefs = EncryptedSharedPreferences.create(
                    context.applicationContext,
                    "secret_shared_prefs",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
                
                var passphrase = sharedPrefs.getString("db_passphrase", null)
                if (passphrase == null) {
                    val secureRandom = SecureRandom()
                    val bytes = ByteArray(32)
                    secureRandom.nextBytes(bytes)
                    passphrase = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    sharedPrefs.edit().putString("db_passphrase", passphrase).apply()
                }

                val dbFile = context.getDatabasePath("reading_list_database")
                if (dbFile.exists()) {
                    try {
                        val header = ByteArray(16)
                        val bytesRead = dbFile.inputStream().use { it.read(header) }
                        if (bytesRead > 0 && String(header).startsWith("SQLite format 3")) {
                            context.deleteDatabase("reading_list_database")
                        } else {
                            System.loadLibrary("sqlcipher")
                            try {
                                val db = SQLiteDatabase.openDatabase(
                                    dbFile.absolutePath,
                                    passphrase,
                                    null,
                                    SQLiteDatabase.OPEN_READONLY,
                                    null
                                )
                                db.close()
                            } catch (e: Exception) {
                                e.printStackTrace()
                                context.deleteDatabase("reading_list_database")
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        context.deleteDatabase("reading_list_database")
                    }
                }
                
                System.loadLibrary("sqlcipher")
                val factory = SupportOpenHelperFactory(passphrase.toByteArray(Charsets.UTF_8))

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "reading_list_database"
                )
                .openHelperFactory(factory)
                .addMigrations(MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)
                .fallbackToDestructiveMigration(true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
