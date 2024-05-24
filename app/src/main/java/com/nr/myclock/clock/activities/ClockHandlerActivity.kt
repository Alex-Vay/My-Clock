package com.nr.myclock.clock.activities

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.provider.AlarmClock
import com.nr.myclock.R
import com.nr.myclock.clock.activities.dialogs.EditClockAlarmDialog
import com.nr.myclock.clock.activities.dialogs.EditClockTimerDialog
import com.nr.myclock.clock.activities.dialogs.SelectClockAlarmDialog
import com.nr.myclock.clock.activities.extensions.config
import com.nr.myclock.clock.activities.extensions.createNewAlarm
import com.nr.myclock.clock.activities.extensions.createNewTimer
import com.nr.myclock.clock.activities.extensions.dbHelper
import com.nr.myclock.clock.activities.extensions.getDismissAlarmPendingIntent
import com.nr.myclock.clock.activities.extensions.getHideTimerPendingIntent
import com.nr.myclock.clock.activities.extensions.isBitSet
import com.nr.myclock.clock.activities.extensions.scheduleNextAlarm
import com.nr.myclock.clock.activities.extensions.secondsToMillis
import com.nr.myclock.clock.activities.extensions.timerHelper
import com.nr.myclock.clock.activities.helpers.DEFAULT_ALARM_MINUTES
import com.nr.myclock.clock.activities.helpers.EARLY_ALARM_NOTIF_ID
import com.nr.myclock.clock.activities.helpers.TODAY_BIT
import com.nr.myclock.clock.activities.helpers.TOMORROW_BIT
import com.nr.myclock.clock.activities.helpers.getBitForCalendarDay
import com.nr.myclock.clock.activities.helpers.getCurrentDayMinutes
import com.nr.myclock.clock.activities.helpers.getTodayBit
import com.nr.myclock.clock.activities.helpers.getTomorrowBit
import com.nr.myclock.clock.activities.models.Alarm
import com.nr.myclock.clock.activities.models.EventAlarm
import com.nr.myclock.clock.activities.models.EventTimer
import com.nr.myclock.clock.activities.models.StateTimer
import com.nr.myclock.clock.activities.models.Timer
import org.fossify.commons.dialogs.PermissionRequiredDialog
import org.fossify.commons.extensions.getDefaultAlarmSound
import org.fossify.commons.extensions.getFilenameFromUri
import org.fossify.commons.extensions.openNotificationSettings
import org.fossify.commons.helpers.SILENT
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.commons.models.AlarmSound
import org.greenrobot.eventbus.EventBus
import java.util.concurrent.TimeUnit

class ClockHandlerActivity : SimpleActivity() {
    companion object {
        @SuppressLint("InlinedApi")
        val HANDLED_ACTIONS = listOf(
            AlarmClock.ACTION_SET_ALARM,
            AlarmClock.ACTION_SET_TIMER,
            AlarmClock.ACTION_DISMISS_ALARM,
            AlarmClock.ACTION_DISMISS_TIMER
        )

        private const val URI_SCHEME = "id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        isMaterialActivity = true
        super.onCreate(savedInstanceState)

        handleIntent(intent)
    }



    private fun handleIntent(intentToHandle: Intent) {
        intentToHandle.apply {
            when (action) {
                AlarmClock.ACTION_SET_ALARM -> setNewAlarm()
                AlarmClock.ACTION_SET_TIMER -> setNewTimer()
                AlarmClock.ACTION_DISMISS_ALARM -> dismissAlarm()
                AlarmClock.ACTION_DISMISS_TIMER -> dismissTimer()
                else -> finish()
            }
        }
    }

    private fun Intent.setNewAlarm() {
        val hour = getIntExtra(AlarmClock.EXTRA_HOUR, 0).coerceIn(0, 23)
        val minute = getIntExtra(AlarmClock.EXTRA_MINUTES, 0).coerceIn(0, 59)
        val days = getIntegerArrayListExtra(AlarmClock.EXTRA_DAYS) ?: getIntArrayExtra(AlarmClock.EXTRA_DAYS)?.toList()
        val message = getStringExtra(AlarmClock.EXTRA_MESSAGE)
        val ringtone = getStringExtra(AlarmClock.EXTRA_RINGTONE)
        val skipUi = getBooleanExtra(AlarmClock.EXTRA_SKIP_UI, false)
        val defaultAlarmSound = getDefaultAlarmSound(RingtoneManager.TYPE_ALARM)

        var weekDays = 0
        days?.forEach {
            weekDays += getBitForCalendarDay(it)
        }
        val soundToUse = ringtone?.let {
            if (it == AlarmClock.VALUE_RINGTONE_SILENT) {
                AlarmSound(0, getString(org.fossify.commons.R.string.no_sound), SILENT)
            } else {
                try {
                    val uri = Uri.parse(it)
                    var filename = getFilenameFromUri(uri)
                    if (filename.isEmpty()) {
                        filename = getString(org.fossify.commons.R.string.alarm)
                    }
                    AlarmSound(0, filename, it)
                } catch (e: Exception) {
                    null
                }
            }
        } ?: defaultAlarmSound

        if (hasExtra(AlarmClock.EXTRA_HOUR) && skipUi) {
            var daysToCompare = weekDays
            val timeInMinutes = hour * 60 + minute
            if (weekDays <= 0) {
                daysToCompare = if (timeInMinutes > getCurrentDayMinutes()) {
                    TODAY_BIT
                } else {
                    TOMORROW_BIT
                }
            }
            val existingAlarm = dbHelper.getAlarms().firstOrNull {
                it.days == daysToCompare
                    && it.soundTitle == soundToUse.title
                    && it.soundUri == soundToUse.uri
                    && it.label == (message ?: "")
                    && it.timeInMinutes == timeInMinutes
            }

            if (existingAlarm != null && !existingAlarm.isEnabled) {
                existingAlarm.isEnabled = true
                startAlarm(existingAlarm)
                finish()
            }
        }

        val newAlarm = createNewAlarm(DEFAULT_ALARM_MINUTES, 0)
        newAlarm.isEnabled = true
        newAlarm.days = weekDays
        newAlarm.soundTitle = soundToUse.title
        newAlarm.soundUri = soundToUse.uri
        if (message != null) {
            newAlarm.label = message
        }

        if (!hasExtra(AlarmClock.EXTRA_HOUR) || !skipUi) {
            newAlarm.id = -1
            newAlarm.timeInMinutes += minute
            openEditAlarm(newAlarm)
        } else {
            newAlarm.timeInMinutes = hour * 60 + minute
            if (newAlarm.days <= 0) {
                newAlarm.days = if (newAlarm.timeInMinutes > getCurrentDayMinutes()) {
                    TODAY_BIT
                } else {
                    TOMORROW_BIT
                }
                newAlarm.oneShot = true
            }

            ensureBackgroundThread {
                newAlarm.id = dbHelper.insertAlarm(newAlarm)
                runOnUiThread {
                    startAlarm(newAlarm)
                    finish()
                }
            }
        }
    }

    private fun Intent.setNewTimer() {
        val length = getIntExtra(AlarmClock.EXTRA_LENGTH, -1)
        val message = getStringExtra(AlarmClock.EXTRA_MESSAGE)
        val skipUi = getBooleanExtra(AlarmClock.EXTRA_SKIP_UI, false)

        fun createAndStartNewTimer() {
            val newTimer = createNewTimer()
            if (message != null) {
                newTimer.label = message
            }

            if (length < 0 || !skipUi) {
                newTimer.id = -1
                openEditTimer(newTimer)
            } else {
                newTimer.seconds = length
                newTimer.oneShot = true

                timerHelper.insertOrUpdateTimer(newTimer) {
                    config.timerLastConfig = newTimer
                    newTimer.id = it.toInt()
                    startTimer(newTimer)
                }
            }
        }

        if (hasExtra(AlarmClock.EXTRA_LENGTH)) {
            timerHelper.findTimers(length, message ?: "") {
                val existingTimer = it.firstOrNull { it.state is StateTimer.Idle }

                if (existingTimer != null
                    && skipUi
                    && (existingTimer.state is StateTimer.Idle || (existingTimer.state is StateTimer.Finished && !existingTimer.oneShot))
                ) {
                    startTimer(existingTimer)
                } else {
                    createAndStartNewTimer()
                }
            }
        } else {
            createAndStartNewTimer()
        }
    }

    private fun Intent.dismissAlarm() {
        val searchMode = getStringExtra(AlarmClock.EXTRA_ALARM_SEARCH_MODE)
        val uri = data
        if (uri != null) {
            if (uri.scheme == URI_SCHEME) {
                val id = uri.schemeSpecificPart.toIntOrNull()
                if (id != null) {
                    val alarm = dbHelper.getAlarmWithId(id)
                    if (alarm != null) {
                        getDismissAlarmPendingIntent(alarm.id, EARLY_ALARM_NOTIF_ID).send()
                        EventBus.getDefault().post(EventAlarm.Refresh)
                        finish()
                    }
                }
            }
            finish()
        } else {
            var alarms = dbHelper.getAlarms().filter { it.isEnabled }

            if (searchMode != null) {
                when (searchMode) {
                    AlarmClock.ALARM_SEARCH_MODE_TIME -> {
                        if (hasExtra(AlarmClock.EXTRA_HOUR)) {
                            val hour = getIntExtra(AlarmClock.EXTRA_HOUR, -1).coerceIn(0, 23)
                            alarms = alarms.filter { it.timeInMinutes / 60 == hour || it.timeInMinutes / 60 == hour + 12 }
                        }
                        if (hasExtra(AlarmClock.EXTRA_MINUTES)) {
                            val minute = getIntExtra(AlarmClock.EXTRA_MINUTES, -1).coerceIn(0, 59)
                            alarms = alarms.filter { it.timeInMinutes % 60 == minute }
                        }
                        if (hasExtra(AlarmClock.EXTRA_IS_PM)) {
                            val isPm = getBooleanExtra(AlarmClock.EXTRA_IS_PM, false)
                            alarms = alarms.filter {
                                val hour = it.timeInMinutes / 60
                                if (isPm) {
                                    hour in 12..23
                                } else {
                                    hour in 0..11
                                }
                            }
                        }
                    }

                    AlarmClock.ALARM_SEARCH_MODE_NEXT -> {
                        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                        val next = alarmManager.nextAlarmClock
                        val timeInMinutes = TimeUnit.MILLISECONDS.toMinutes(next.triggerTime).toInt()
                        val dayBitToLookFor = if (timeInMinutes <= getCurrentDayMinutes()) {
                            getTomorrowBit()
                        } else {
                            getTodayBit()
                        }
                        val dayToLookFor = if (timeInMinutes <= getCurrentDayMinutes()) {
                            TOMORROW_BIT
                        } else {
                            TODAY_BIT
                        }
                        alarms = alarms.filter {
                            it.timeInMinutes == timeInMinutes && (it.days.isBitSet(dayBitToLookFor) || it.days == dayToLookFor)
                        }
                    }

                    AlarmClock.ALARM_SEARCH_MODE_LABEL -> {
                        val messageToSearchFor = getStringExtra(AlarmClock.EXTRA_MESSAGE)
                        if (messageToSearchFor != null) {
                            alarms = alarms.filter { it.label.contains(messageToSearchFor, ignoreCase = true) }
                        }
                    }

                    AlarmClock.ALARM_SEARCH_MODE_ALL -> {
                        // no-op - no further filtering needed
                    }
                }
            }

            if (alarms.count() == 1) {
                getDismissAlarmPendingIntent(alarms.first().id, EARLY_ALARM_NOTIF_ID).send()
                EventBus.getDefault().post(EventAlarm.Refresh)
                finish()
            } else if (alarms.count() > 1) {
                SelectClockAlarmDialog(this@ClockHandlerActivity, alarms, R.string.select_alarm_to_dismiss) {
                    if (it != null) {
                        getDismissAlarmPendingIntent(it.id, EARLY_ALARM_NOTIF_ID).send()
                    }
                    EventBus.getDefault().post(EventAlarm.Refresh)
                    finish()
                }
            } else {
                finish()
            }
        }
    }

    private fun Intent.dismissTimer() {
        val uri = data
        if (uri == null) {
            timerHelper.getTimers {
                it.filter { it.state == StateTimer.Finished }.forEach {
                    getHideTimerPendingIntent(it.id!!).send()
                }
                EventBus.getDefault().post(EventTimer.Refresh)
                finish()
            }
            return
        } else if (uri.scheme == URI_SCHEME) {
            val id = uri.schemeSpecificPart.toIntOrNull()
            if (id != null) {
                timerHelper.tryGetTimer(id) {
                    if (it != null) {
                        getHideTimerPendingIntent(it.id!!).send()
                        EventBus.getDefault().post(EventTimer.Refresh)
                        finish()
                    } else {
                        finish()
                    }
                }
                return
            }
        }
        finish()
    }

    private fun openEditAlarm(alarm: Alarm) {
        EditClockAlarmDialog(this, alarm, onDismiss = { finish() }) {
            alarm.id = it
            startAlarm(alarm)
            finish()
        }
    }

    private fun openEditTimer(timer: Timer) {
        EditClockTimerDialog(this, timer) {
            timer.id = it.toInt()
            startTimer(timer)
        }
    }

    private fun startAlarm(alarm: Alarm) {
        scheduleNextAlarm(alarm, true)
        EventBus.getDefault().post(EventAlarm.Refresh)
    }

    private fun startTimer(timer: Timer) {
        handleNotificationPermission { granted ->
            val newState = StateTimer.Running(timer.seconds.secondsToMillis, timer.seconds.secondsToMillis)
            val newTimer = timer.copy(state = newState)
            fun notifyAndStartTimer() {
                EventBus.getDefault().post(EventTimer.Start(newTimer.id!!, newTimer.seconds.secondsToMillis))
                EventBus.getDefault().post(EventTimer.Refresh)
            }

            if (granted) {
                timerHelper.insertOrUpdateTimer(newTimer) {
                    notifyAndStartTimer()
                    finish()
                }
            } else {
                PermissionRequiredDialog(
                    this,
                    org.fossify.commons.R.string.allow_notifications_reminders,
                    positiveActionCallback = {
                        openNotificationSettings()
                        timerHelper.insertOrUpdateTimer(newTimer) {
                            notifyAndStartTimer()
                            finish()
                        }
                    },
                    negativeActionCallback = {
                        finish()
                    })
            }
        }
    }
}
