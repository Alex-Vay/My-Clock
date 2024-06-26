package com.nr.myclock.clock.activities.extensions

import android.app.*
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager.STREAM_ALARM
import android.media.RingtoneManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.text.SpannableString
import android.text.format.DateFormat
import android.text.style.RelativeSizeSpan
import android.widget.Toast
import androidx.core.app.AlarmManagerCompat
import androidx.core.app.NotificationCompat
import com.nr.myclock.R
import com.nr.myclock.clock.activities.ClockReminderActivity
import com.nr.myclock.clock.activities.ClockSnoozeReminderActivity
import com.nr.myclock.clock.activities.ClockplashActivity
import com.nr.myclock.clock.activities.ClockDatabase
import com.nr.myclock.clock.activities.TimerDao
import com.nr.myclock.games.game128.Game128Activity
import com.nr.myclock.games.math.MathActivity
import com.nr.myclock.games.memory.MemoryGameActivity
import com.nr.myclock.games.quiz.QuizActivity
import com.nr.myclock.games.riddle.RiddlesActivity
import com.nr.myclock.games.schulteTable.SchulteActivity
import com.nr.myclock.clock.activities.models.Alarm
import com.nr.myclock.clock.activities.models.CurrentTimeZone
import com.nr.myclock.clock.activities.models.Timer
import com.nr.myclock.clock.activities.models.StateTimer
import com.nr.myclock.clock.activities.AlarmReceiver
import com.nr.myclock.clock.activities.DismissAlarmReceiver
import com.nr.myclock.clock.activities.EarlyAlarmDismissalReceiver
import com.nr.myclock.clock.activities.HideAlarmReceiver
import com.nr.myclock.clock.activities.HideTimerReceiver
import com.nr.myclock.clock.activities.SnoozeService
import com.nr.myclock.clock.activities.helpers.ALARM_ID
import com.nr.myclock.clock.activities.helpers.ALARM_NOTIFICATION_CHANNEL_ID
import com.nr.myclock.clock.activities.helpers.AnalogueTimeWidgetProvider
import com.nr.myclock.clock.activities.helpers.Configuration
import com.nr.myclock.clock.activities.helpers.DBHelper
import com.nr.myclock.clock.activities.helpers.DigitalTimeWidgetProvider
import com.nr.myclock.clock.activities.helpers.EARLY_ALARM_DISMISSAL_INTENT_ID
import com.nr.myclock.clock.activities.helpers.EDITED_TIME_ZONE_SEPARATOR
import com.nr.myclock.clock.activities.helpers.NOTIFICATION_ID
import com.nr.myclock.clock.activities.helpers.OPEN_ALARMS_TAB_INTENT_ID
import com.nr.myclock.clock.activities.helpers.OPEN_STOPWATCH_TAB_INTENT_ID
import com.nr.myclock.clock.activities.helpers.OPEN_TAB
import com.nr.myclock.clock.activities.helpers.REMINDER_ACTIVITY_INTENT_ID
import com.nr.myclock.clock.activities.helpers.TAB_ALARM
import com.nr.myclock.clock.activities.helpers.TAB_STOPWATCH
import com.nr.myclock.clock.activities.helpers.TAB_TIMER
import com.nr.myclock.clock.activities.helpers.TIMER_ID
import com.nr.myclock.clock.activities.helpers.TODAY_BIT
import com.nr.myclock.clock.activities.helpers.TOMORROW_BIT
import com.nr.myclock.clock.activities.helpers.TimerHelper
import com.nr.myclock.clock.activities.helpers.formatTime
import com.nr.myclock.clock.activities.helpers.getAllTimeZones
import com.nr.myclock.clock.activities.helpers.getCurrentDayMinutes
import com.nr.myclock.clock.activities.helpers.getDefaultTimeZoneTitle
import com.nr.myclock.clock.activities.helpers.getPassedSeconds
import com.nr.myclock.clock.activities.helpers.getTimeUntilNextAlarm
import org.fossify.commons.extensions.*
import org.fossify.commons.helpers.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.pow
import kotlin.random.Random
import kotlin.time.Duration.Companion.minutes

val Context.config: Configuration get() = Configuration.newInstance(applicationContext)

val Context.dbHelper: DBHelper get() = DBHelper.newInstance(applicationContext)
val Context.timerDb: TimerDao get() = ClockDatabase.getInstance(applicationContext).TimerDao()
val Context.timerHelper: TimerHelper get() = TimerHelper(this)

fun Context.getFormattedDate(calendar: Calendar): String {
    val dayOfWeek = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7    // make sure index 0 means monday
    val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
    val month = calendar.get(Calendar.MONTH)

    val dayString = resources.getStringArray(org.fossify.commons.R.array.week_days_short)[dayOfWeek]
    val monthString = resources.getStringArray(org.fossify.commons.R.array.months)[month]
    return "$dayString, $dayOfMonth $monthString"
}

fun Context.getEditedTimeZonesMap(): HashMap<Int, String> {
    val editedTimeZoneTitles = config.editedTimeZoneTitles
    val editedTitlesMap = HashMap<Int, String>()
    editedTimeZoneTitles.forEach {
        val parts = it.split(EDITED_TIME_ZONE_SEPARATOR.toRegex(), 2)
        editedTitlesMap[parts[0].toInt()] = parts[1]
    }
    return editedTitlesMap
}

fun Context.getAllTimeZonesModified(): ArrayList<CurrentTimeZone> {
    val timeZones = getAllTimeZones()
    val editedTitlesMap = getEditedTimeZonesMap()
    timeZones.forEach {
        if (editedTitlesMap.keys.contains(it.id)) {
            it.title = editedTitlesMap[it.id]!!
        } else {
            it.title = it.title.substring(it.title.indexOf(' ')).trim()
        }
    }
    return timeZones
}

fun Context.getModifiedTimeZoneTitle(id: Int) = getAllTimeZonesModified().firstOrNull { it.id == id }?.title ?: getDefaultTimeZoneTitle(id)

fun Context.createNewAlarm(timeInMinutes: Int, weekDays: Int): Alarm {
    val defaultAlarmSound = getDefaultAlarmSound(RingtoneManager.TYPE_ALARM)
    return Alarm(0, timeInMinutes, weekDays, false, false, defaultAlarmSound.title, defaultAlarmSound.uri, "")
}

fun Context.showRemainingTimeMessage(totalMinutes: Int) {
    val fullString = String.format(getString(org.fossify.commons.R.string.time_remaining), formatMinutesToTimeString(totalMinutes))
    toast(fullString, Toast.LENGTH_LONG)
}

fun Context.setupAlarmClock(alarm: Alarm, triggerInSeconds: Int) {
    val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val targetMS = System.currentTimeMillis() + triggerInSeconds * 1000
    try {
        AlarmManagerCompat.setAlarmClock(alarmManager, targetMS, getOpenAlarmTabIntent(), getAlarmIntent(alarm))

        // show a notification to allow dismissing the alarm 10 minutes before it actually triggers
        val dismissalTriggerTime = if (targetMS - System.currentTimeMillis() < 10.minutes.inWholeMilliseconds) {
            System.currentTimeMillis() + 500
        } else {
            targetMS - 10.minutes.inWholeMilliseconds
        }
        AlarmManagerCompat.setExactAndAllowWhileIdle(alarmManager, 0, dismissalTriggerTime, getEarlyAlarmDismissalIntent(alarm))
    } catch (e: Exception) {
        showErrorToast(e)
    }
}

fun Context.createNewTimer(): Timer {
    return Timer(
        null,
        config.timerSeconds,
        StateTimer.Idle,
        config.timerVibrate,
        config.timerSoundUri,
        config.timerSoundTitle,
        config.timerLabel ?: "",
        System.currentTimeMillis(),
        config.timerChannelId,
    )
}

fun Context.scheduleNextAlarm(alarm: Alarm, showToast: Boolean) {
    val calendar = Calendar.getInstance()
    calendar.firstDayOfWeek = Calendar.MONDAY
    val currentTimeInMinutes = getCurrentDayMinutes()

    if (alarm.days == TODAY_BIT) {
        val triggerInMinutes = alarm.timeInMinutes - currentTimeInMinutes
        setupAlarmClock(alarm, triggerInMinutes * 60 - calendar.get(Calendar.SECOND))

        if (showToast) {
            showRemainingTimeMessage(triggerInMinutes)
        }
    } else if (alarm.days == TOMORROW_BIT) {
        val triggerInMinutes = alarm.timeInMinutes - currentTimeInMinutes + DAY_MINUTES
        setupAlarmClock(alarm, triggerInMinutes * 60 - calendar.get(Calendar.SECOND))

        if (showToast) {
            showRemainingTimeMessage(triggerInMinutes)
        }
    } else {
        for (i in 0..7) {
            val currentDay = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7
            val isCorrectDay = alarm.days and 2.0.pow(currentDay).toInt() != 0
            if (isCorrectDay && (alarm.timeInMinutes > currentTimeInMinutes || i > 0)) {
                val triggerInMinutes = alarm.timeInMinutes - currentTimeInMinutes + (i * DAY_MINUTES)
                setupAlarmClock(alarm, triggerInMinutes * 60 - calendar.get(Calendar.SECOND))

                if (showToast) {
                    showRemainingTimeMessage(triggerInMinutes)
                }
                break
            } else {
                calendar.add(Calendar.DAY_OF_MONTH, 1)
            }
        }
    }
}

fun Context.getEarlyAlarmDismissalIntent(alarm: Alarm): PendingIntent {
    val intent = Intent(this, EarlyAlarmDismissalReceiver::class.java).apply {
        putExtra(ALARM_ID, alarm.id)
    }
    return PendingIntent.getBroadcast(this, EARLY_ALARM_DISMISSAL_INTENT_ID, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
}

fun Context.getOpenAlarmTabIntent(): PendingIntent {
    val intent = getLaunchIntent() ?: Intent(this, ClockplashActivity::class.java)
    intent.putExtra(OPEN_TAB, TAB_ALARM)
    return PendingIntent.getActivity(this, OPEN_ALARMS_TAB_INTENT_ID, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
}

fun Context.getOpenTimerTabIntent(timerId: Int): PendingIntent {
    val intent = getLaunchIntent() ?: Intent(this, ClockplashActivity::class.java)
    intent.putExtra(OPEN_TAB, TAB_TIMER)
    intent.putExtra(TIMER_ID, timerId)
    return PendingIntent.getActivity(this, timerId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
}

fun Context.getOpenStopwatchTabIntent(): PendingIntent {
    val intent = getLaunchIntent() ?: Intent(this, ClockplashActivity::class.java)
    intent.putExtra(OPEN_TAB, TAB_STOPWATCH)
    return PendingIntent.getActivity(this, OPEN_STOPWATCH_TAB_INTENT_ID, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
}

fun Context.getAlarmIntent(alarm: Alarm): PendingIntent {
    val intent = Intent(this, AlarmReceiver::class.java)
    intent.putExtra(ALARM_ID, alarm.id)
    return PendingIntent.getBroadcast(this, alarm.id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
}

fun Context.cancelAlarmClock(alarm: Alarm) {
    val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
    alarmManager.cancel(getAlarmIntent(alarm))
    alarmManager.cancel(getEarlyAlarmDismissalIntent(alarm))
}

fun Context.hideNotification(id: Int) {
    val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    manager.cancel(id)
}

fun Context.deleteNotificationChannel(channelId: String) {
    if (isOreoPlus()) {
        try {
            val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.deleteNotificationChannel(channelId)
        } catch (_: Throwable) {
        }
    }
}

fun Context.hideTimerNotification(timerId: Int) = hideNotification(timerId)

fun Context.updateWidgets() {
    updateDigitalWidgets()
    updateAnalogueWidgets()
}

fun Context.updateDigitalWidgets() {
    val component = ComponentName(applicationContext, DigitalTimeWidgetProvider::class.java)
    val widgetIds = AppWidgetManager.getInstance(applicationContext)?.getAppWidgetIds(component) ?: return
    if (widgetIds.isNotEmpty()) {
        val ids = intArrayOf(R.xml.widget_digital_clock_info)
        Intent(applicationContext, DigitalTimeWidgetProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            sendBroadcast(this)
        }
    }
}

fun Context.updateAnalogueWidgets() {
    val component = ComponentName(applicationContext, AnalogueTimeWidgetProvider::class.java)
    val widgetIds = AppWidgetManager.getInstance(applicationContext)?.getAppWidgetIds(component) ?: return
    if (widgetIds.isNotEmpty()) {
        val ids = intArrayOf(R.xml.widget_analogue_clock_info)
        Intent(applicationContext, AnalogueTimeWidgetProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            sendBroadcast(this)
        }
    }
}

fun Context.getFormattedTime(passedSeconds: Int, showSeconds: Boolean, makeAmPmSmaller: Boolean): SpannableString {
    val use24HourFormat = DateFormat.is24HourFormat(this)
    val hours = (passedSeconds / 3600) % 24
    val minutes = (passedSeconds / 60) % 60
    val seconds = passedSeconds % 60

    return if (use24HourFormat) {
        val formattedTime = formatTime(showSeconds, use24HourFormat, hours, minutes, seconds)
        SpannableString(formattedTime)
    } else {
        val formattedTime = formatTo12HourFormat(showSeconds, hours, minutes, seconds)
        val spannableTime = SpannableString(formattedTime)
        val amPmMultiplier = if (makeAmPmSmaller) 0.4f else 1f
        spannableTime.setSpan(RelativeSizeSpan(amPmMultiplier), spannableTime.length - 3, spannableTime.length, 0)
        spannableTime
    }
}

fun Context.formatTo12HourFormat(showSeconds: Boolean, hours: Int, minutes: Int, seconds: Int): String {
    val appendable = getString(if (hours >= 12) org.fossify.commons.R.string.p_m else org.fossify.commons.R.string.a_m)
    val newHours = if (hours == 0 || hours == 12) 12 else hours % 12
    return "${formatTime(showSeconds, false, newHours, minutes, seconds)} $appendable"
}

fun Context.getClosestEnabledAlarmString(callback: (result: String) -> Unit) {
    getEnabledAlarms { enabledAlarms ->
        if (enabledAlarms.isNullOrEmpty()) {
            callback("")
            return@getEnabledAlarms
        }

        val nextAlarmList = enabledAlarms
            .mapNotNull { getTimeUntilNextAlarm(it.timeInMinutes, it.days) }

        if (nextAlarmList.isEmpty()) {
            callback("")
        }

        var closestAlarmTime = Int.MAX_VALUE
        nextAlarmList.forEach { time ->
            if (time < closestAlarmTime) {
                closestAlarmTime = time
            }
        }

        if (closestAlarmTime == Int.MAX_VALUE) {
            callback("")
        }

        val calendar = Calendar.getInstance().apply { firstDayOfWeek = Calendar.MONDAY }
        calendar.add(Calendar.MINUTE, closestAlarmTime)
        val dayOfWeekIndex = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7
        val dayOfWeek = resources.getStringArray(org.fossify.commons.R.array.week_days_short)[dayOfWeekIndex]
        val pattern = if (DateFormat.is24HourFormat(this)) {
            "HH:mm"
        } else {
            "h:mm a"
        }

        val formattedTime = SimpleDateFormat(pattern, Locale.getDefault()).format(calendar.time)
        callback("$dayOfWeek $formattedTime")
    }
}

fun Context.getEnabledAlarms(callback: (result: List<Alarm>?) -> Unit) {
    ensureBackgroundThread {
        val alarms = dbHelper.getEnabledAlarms()
        Handler(Looper.getMainLooper()).post {
            callback(alarms)
        }
    }
}

fun Context.rescheduleEnabledAlarms() {
    dbHelper.getEnabledAlarms().forEach {
        if (it.days != TODAY_BIT || it.timeInMinutes > getCurrentDayMinutes()) {
            scheduleNextAlarm(it, false)
        }
    }
}

fun Context.isScreenOn() = (getSystemService(Context.POWER_SERVICE) as PowerManager).isScreenOn

fun getGame(context: Context, gameNum: Int) : Intent{
    return when(gameNum) {
        1 -> Intent(context, Game128Activity::class.java)
        2 -> Intent(context, MathActivity::class.java)
        3 -> Intent(context, MemoryGameActivity::class.java)
        4 -> Intent(context, QuizActivity::class.java)
        5 -> Intent(context, RiddlesActivity::class.java)
        else -> Intent(context, SchulteActivity::class.java)
    }
}

fun Context.showAlarmNotification(alarm: Alarm) {
    val gameNum = Random.nextInt(1, 7)
    val pendingIntent = PendingIntent.getActivity(this, OPEN_ALARMS_TAB_INTENT_ID, getGame(this, gameNum), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    val notification = getAlarmNotification(pendingIntent, alarm)
    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    try {
        notificationManager.notify(alarm.id, notification)
    } catch (e: Exception) {
        showErrorToast(e)
    }

    if (alarm.days > 0) {
        scheduleNextAlarm(alarm, false)
    }
}

fun Context.getTimerNotification(timer: Timer, pendingIntent: PendingIntent, addDeleteIntent: Boolean): Notification {
    var soundUri = timer.soundUri
    if (soundUri == SILENT) {
        soundUri = ""
    } else {
        grantReadUriPermission(soundUri)
    }

    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val channelId = timer.channelId ?: "simple_timer_channel_${soundUri}_${System.currentTimeMillis()}"
    timerHelper.insertOrUpdateTimer(timer.copy(channelId = channelId))

    if (isOreoPlus()) {
        try {
            notificationManager.deleteNotificationChannel(channelId)
        } catch (e: Exception) {
        }

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setLegacyStreamType(STREAM_ALARM)
            .build()

        val name = getString(R.string.timer)
        val importance = NotificationManager.IMPORTANCE_HIGH
        NotificationChannel(channelId, name, importance).apply {
            setBypassDnd(true)
            enableLights(true)
            lightColor = getProperPrimaryColor()
            setSound(Uri.parse(soundUri), audioAttributes)

            if (!timer.vibrate) {
                vibrationPattern = longArrayOf(0L)
            }

            enableVibration(timer.vibrate)
            notificationManager.createNotificationChannel(this)
        }
    }

    val title = if (timer.label.isEmpty()) {
        getString(R.string.timer)
    } else {
        timer.label
    }

    val reminderActivityIntent = getReminderActivityIntent()
    val builder = NotificationCompat.Builder(this)
        .setContentTitle(title)
        .setContentText(getString(R.string.time_expired))
        .setSmallIcon(R.drawable.ic_hourglass_vector)
        .setContentIntent(pendingIntent)
        .setPriority(NotificationCompat.PRIORITY_MAX)
        .setDefaults(Notification.DEFAULT_LIGHTS)
        .setCategory(Notification.CATEGORY_EVENT)
        .setAutoCancel(true)
        .setSound(Uri.parse(soundUri), STREAM_ALARM)
        .setChannelId(channelId)
        .addAction(
            org.fossify.commons.R.drawable.ic_cross_vector,
            getString(org.fossify.commons.R.string.dismiss),
            if (addDeleteIntent) {
                reminderActivityIntent
            } else {
                getHideTimerPendingIntent(timer.id!!)
            }
        )

    if (addDeleteIntent) {
        builder.setDeleteIntent(reminderActivityIntent)
    }

    builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

    if (timer.vibrate) {
        val vibrateArray = LongArray(2) { 500 }
        builder.setVibrate(vibrateArray)
    }

    val notification = builder.build()
    notification.flags = notification.flags or Notification.FLAG_INSISTENT
    return notification
}

fun Context.getHideTimerPendingIntent(timerId: Int): PendingIntent {
    val intent = Intent(this, HideTimerReceiver::class.java)
    intent.putExtra(TIMER_ID, timerId)
    return PendingIntent.getBroadcast(this, timerId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
}

fun Context.getHideAlarmPendingIntent(alarm: Alarm, channelId: String): PendingIntent {
    val intent = getGame(this, Random.nextInt(1, 7))
    return PendingIntent.getBroadcast(this, alarm.id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
}

fun Context.getDismissAlarmPendingIntent(alarmId: Int, notificationId: Int): PendingIntent {
    val intent = Intent(this, DismissAlarmReceiver::class.java).apply {
        putExtra(ALARM_ID, alarmId)
        putExtra(NOTIFICATION_ID, notificationId)
    }
    return PendingIntent.getBroadcast(this, alarmId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
}

fun Context.getAlarmNotification(pendingIntent: PendingIntent, alarm: Alarm): Notification {
    val soundUri = alarm.soundUri
    if (soundUri != SILENT) {
        grantReadUriPermission(soundUri)
    }
    val channelId = "simple_alarm_channel_${soundUri}_${alarm.vibrate}"
    val label = alarm.label.ifEmpty {
        getString(org.fossify.commons.R.string.alarm)
    }

    if (isOreoPlus()) {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setLegacyStreamType(STREAM_ALARM)
            .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val importance = NotificationManager.IMPORTANCE_HIGH
        NotificationChannel(channelId, label, importance).apply {
            setBypassDnd(true)
            enableLights(true)
            lightColor = getProperPrimaryColor()
            enableVibration(alarm.vibrate)
            setSound(Uri.parse(soundUri), audioAttributes)
            notificationManager.createNotificationChannel(this)
        }
    }

    val dismissIntent = getHideAlarmPendingIntent(alarm, channelId)
    val builder = NotificationCompat.Builder(this)
        .setContentTitle(label)
        .setContentText(getFormattedTime(getPassedSeconds(), false, false))
        .setSmallIcon(R.drawable.ic_alarm_vector)
        .setContentIntent(pendingIntent)
        .setPriority(Notification.PRIORITY_HIGH)
        .setDefaults(Notification.DEFAULT_LIGHTS)
        .setAutoCancel(true)
        .setChannelId(channelId)
//        .addAction(
//            org.fossify.commons.R.drawable.ic_snooze_vector,
//            getString(org.fossify.commons.R.string.snooze),
//            getSnoozePendingIntent(alarm)
//        )
//        .addAction(org.fossify.commons.R.drawable.ic_cross_vector, getString(org.fossify.commons.R.string.dismiss), dismissIntent)
        .setDeleteIntent(dismissIntent)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

    if (soundUri != SILENT) {
        builder.setSound(Uri.parse(soundUri), STREAM_ALARM)
    }

    if (alarm.vibrate) {
        val vibrateArray = LongArray(2) { 500 }
        builder.setVibrate(vibrateArray)
    }

    val notification = builder.build()
    notification.flags = notification.flags or Notification.FLAG_INSISTENT
    return notification
}

fun Context.getSnoozePendingIntent(alarm: Alarm): PendingIntent {
    val snoozeClass = if (config.useSameSnooze) SnoozeService::class.java else ClockSnoozeReminderActivity::class.java
    val intent = Intent(this, snoozeClass).setAction("Snooze")
    intent.putExtra(ALARM_ID, alarm.id)
    return if (config.useSameSnooze) {
        PendingIntent.getService(this, alarm.id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    } else {
        PendingIntent.getActivity(this, alarm.id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
}

fun Context.getReminderActivityIntent(): PendingIntent {
    val intent = Intent(this, ClockReminderActivity::class.java)
    return PendingIntent.getActivity(this, REMINDER_ACTIVITY_INTENT_ID, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
}

fun Context.checkAlarmsWithDeletedSoundUri(uri: String) {
    val defaultAlarmSound = getDefaultAlarmSound(RingtoneManager.TYPE_ALARM)
    dbHelper.getAlarmsWithUri(uri).forEach {
        it.soundTitle = defaultAlarmSound.title
        it.soundUri = defaultAlarmSound.uri
        dbHelper.updateAlarm(it)
    }
}

fun Context.getAlarmSelectedDaysString(bitMask: Int): String {
    return when (bitMask) {
        TODAY_BIT -> getString(org.fossify.commons.R.string.today)
        TOMORROW_BIT -> getString(org.fossify.commons.R.string.tomorrow)
        EVERY_DAY_BIT -> getString(org.fossify.commons.R.string.every_day)
        else -> getSelectedDaysString(bitMask)
    }
}

fun Context.firstDayOrder(bitMask: Int): Int {
    if (bitMask == TODAY_BIT) return -2
    if (bitMask == TOMORROW_BIT) return -1

    val dayBits = arrayListOf(MONDAY_BIT, TUESDAY_BIT, WEDNESDAY_BIT, THURSDAY_BIT, FRIDAY_BIT, SATURDAY_BIT, SUNDAY_BIT)

    val sundayFirst = baseConfig.isSundayFirst
    if (sundayFirst) {
        dayBits.moveLastItemToFront()
    }

    dayBits.forEach { bit ->
        if (bitMask and bit != 0) {
            return if (bit == SUNDAY_BIT && sundayFirst) 0 else bit
        }
    }

    return bitMask
}
