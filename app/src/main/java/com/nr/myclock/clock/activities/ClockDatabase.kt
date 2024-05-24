package com.nr.myclock.clock.activities

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nr.myclock.clock.activities.extensions.config
import com.nr.myclock.clock.activities.helpers.Converters
import com.nr.myclock.clock.activities.models.Timer
import com.nr.myclock.clock.activities.models.StateTimer
import java.util.concurrent.Executors

@Database(entities = [Timer::class], version = 2)
@TypeConverters(Converters::class)
abstract class ClockDatabase : RoomDatabase() {

    abstract fun TimerDao(): TimerDao

    companion object {
        private var db: ClockDatabase? = null

        fun getInstance(context: Context): ClockDatabase {
            if (db == null) {
                synchronized(ClockDatabase::class) {
                    if (db == null) {
                        db = Room.databaseBuilder(context.applicationContext, ClockDatabase::class.java, "app.db")
                            .fallbackToDestructiveMigration()
                            .addMigrations(MIGRATION_1_2)
                            .addCallback(object : Callback() {
                                override fun onCreate(db: SupportSQLiteDatabase) {
                                    super.onCreate(db)
                                    insertDefaultTimer(context)
                                }
                            })
                            .build()
                    }
                }
            }
            return db!!
        }

        private fun insertDefaultTimer(context: Context) {
            Executors.newSingleThreadScheduledExecutor().execute {
                val config = context.config
                db!!.TimerDao().insertOrUpdateTimer(
                    Timer(
                        id = null,
                        seconds = config.timerSeconds,
                        state = StateTimer.Idle,
                        vibrate = config.timerVibrate,
                        soundUri = config.timerSoundUri,
                        soundTitle = config.timerSoundTitle,
                        label = config.timerLabel ?: "",
                        createdAt = System.currentTimeMillis(),
                        channelId = config.timerChannelId,
                    )
                )
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `timers` ADD COLUMN `oneShot` INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
