package com.nr.myclock.helpers

import com.nr.myclock.models.MyTimeZone
import java.util.Calendar
import java.util.Date
import java.util.TimeZone
import kotlin.math.pow


const val SELECTED_TIME_ZONES = "selected_time_zones"
const val EDITED_TIME_ZONE_TITLES = "edited_time_zone_titles"
const val TIMER_SECONDS = "timer_seconds"
const val TIMER_VIBRATE = "timer_vibrate"
const val TIMER_SOUND_URI = "timer_sound_uri"
const val TIMER_SOUND_TITLE = "timer_sound_title"
const val TIMER_CHANNEL_ID = "timer_channel_id"
const val TIMER_LABEL = "timer_label"
const val TOGGLE_STOPWATCH = "toggle_stopwatch"
const val TIMER_MAX_REMINDER_SECS = "timer_max_reminder_secs"
const val ALARM_MAX_REMINDER_SECS = "alarm_max_reminder_secs"
const val ALARM_LAST_CONFIG = "alarm_last_config"
const val TIMER_LAST_CONFIG = "timer_last_config"
const val INCREASE_VOLUME_GRADUALLY = "increase_volume_gradually"
const val ALARMS_SORT_BY = "alarms_sort_by"
const val STOPWATCH_LAPS_SORT_BY = "stopwatch_laps_sort_by"


const val TABS_COUNT = 4
const val EDITED_TIME_ZONE_SEPARATOR = ":"
const val ALARM_ID = "alarm_id"
const val NOTIFICATION_ID = "notification_id"
const val DEFAULT_ALARM_MINUTES = 480
const val DEFAULT_MAX_ALARM_REMINDER_SECS = 300
const val DEFAULT_MAX_TIMER_REMINDER_SECS = 60
const val ALARM_NOTIFICATION_CHANNEL_ID = "Alarm_Channel"
const val EARLY_ALARM_DISMISSAL_CHANNEL_ID = "Early Alarm Dismissal"

const val OPEN_STOPWATCH_TAB_INTENT_ID = 9993
const val PICK_AUDIO_FILE_INTENT_ID = 9994
const val REMINDER_ACTIVITY_INTENT_ID = 9995
const val OPEN_ALARMS_TAB_INTENT_ID = 9996
const val OPEN_APP_INTENT_ID = 9997
const val ALARM_NOTIF_ID = 9998
const val TIMER_RUNNING_NOTIF_ID = 10000
const val STOPWATCH_RUNNING_NOTIF_ID = 10001
const val EARLY_ALARM_DISMISSAL_INTENT_ID = 10002
const val EARLY_ALARM_NOTIF_ID = 10003

const val OPEN_TAB = "open_tab"
const val TAB_CLOCK = 0
const val TAB_ALARM = 1
const val TAB_STOPWATCH = 2
const val TAB_TIMER = 3
const val TIMER_ID = "timer_id"
const val INVALID_TIMER_ID = -1


const val SORT_BY_LAP = 1
const val SORT_BY_LAP_TIME = 2
const val SORT_BY_TOTAL_TIME = 4


const val SORT_BY_CREATION_ORDER = 0
const val SORT_BY_ALARM_TIME = 1
const val SORT_BY_DATE_AND_TIME = 2

const val TODAY_BIT = -1
const val TOMORROW_BIT = -2


const val STOPWATCH_SHORTCUT_ID = "stopwatch_shortcut_id"
const val STOPWATCH_TOGGLE_ACTION = "org.fossify.clock.TOGGLE_STOPWATCH"

val DAY_BIT_MAP = mapOf(
    Calendar.SUNDAY to SUNDAY_BIT,
    Calendar.MONDAY to MONDAY_BIT,
    Calendar.TUESDAY to TUESDAY_BIT,
    Calendar.WEDNESDAY to WEDNESDAY_BIT,
    Calendar.THURSDAY to THURSDAY_BIT,
    Calendar.FRIDAY to FRIDAY_BIT,
    Calendar.SATURDAY to SATURDAY_BIT,
)

fun getDefaultTimeZoneTitle(id: Int) = getAllTimeZones().firstOrNull { it.id == id }?.title ?: ""

fun getPassedSeconds(): Int {
    val calendar = Calendar.getInstance()
    val isDaylightSavingActive = TimeZone.getDefault().inDaylightTime(Date())
    var offset = calendar.timeZone.rawOffset
    if (isDaylightSavingActive) {
        offset += TimeZone.getDefault().dstSavings
    }
    return ((calendar.timeInMillis + offset) / 1000).toInt()
}

fun formatTime(showSeconds: Boolean, use24HourFormat: Boolean, hours: Int, minutes: Int, seconds: Int): String {
    val hoursFormat = if (use24HourFormat) "%02d" else "%01d"
    var format = "$hoursFormat:%02d"

    return if (showSeconds) {
        format += ":%02d"
        String.format(format, hours, minutes, seconds)
    } else {
        String.format(format, hours, minutes)
    }
}

fun getTomorrowBit(): Int {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_WEEK, 1)
    val dayOfWeek = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7
    return 2.0.pow(dayOfWeek).toInt()
}

fun getTodayBit(): Int {
    val calendar = Calendar.getInstance()
    val dayOfWeek = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7
    return 2.0.pow(dayOfWeek).toInt()
}

fun getBitForCalendarDay(day: Int): Int {
    return DAY_BIT_MAP[day] ?: 0
}

fun getCurrentDayMinutes(): Int {
    val calendar = Calendar.getInstance()
    return calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
}

fun getAllTimeZones() = arrayListOf(
    MyTimeZone(1, "GMT-08:00 Los Angeles", "America/Los_Angeles"),
    MyTimeZone(2, "GMT-07:00 Denver", "America/Denver"),
    MyTimeZone(3, "GMT-06:00 Costa Rica", "America/Costa_Rica"),
    MyTimeZone(4, "GMT-06:00 Chicago", "America/Chicago"),
    MyTimeZone(5, "GMT-06:00 Mexico City", "America/Mexico_City"),
    MyTimeZone(6, "GMT-05:00 New York", "America/New_York"),
    MyTimeZone(7, "GMT+00:00 Greenwich Mean Time", "Etc/Greenwich"),
    MyTimeZone(8, "GMT+01:00 Amsterdam", "Europe/Amsterdam"),
    MyTimeZone(9, "GMT+01:00 Belgrade", "Europe/Belgrade"),
    MyTimeZone(10, "GMT+01:00 Brussels", "Europe/Brussels"),
    MyTimeZone(11, "GMT+01:00 Madrid", "Europe/Madrid"),
    MyTimeZone(12, "GMT+02:00 Helsinki", "Europe/Helsinki"),
    MyTimeZone(13, "GMT+02:00 Jerusalem", "Asia/Jerusalem"),
    MyTimeZone(14, "GMT+03:00 Minsk", "Europe/Minsk"),
    MyTimeZone(15, "GMT+03:00 Baghdad", "Asia/Baghdad"),
    MyTimeZone(16, "GMT+03:00 Moscow", "Europe/Moscow"),
    MyTimeZone(17, "GMT+03:30 Tehran", "Asia/Tehran"),
    MyTimeZone(18, "GMT+04:00 Baku", "Asia/Baku"),
    MyTimeZone(19, "GMT+04:00 Tbilisi", "Asia/Tbilisi"),
    MyTimeZone(20, "GMT+04:00 Yerevan", "Asia/Yerevan"),
    MyTimeZone(21, "GMT+04:00 Dubai", "Asia/Dubai"),
    MyTimeZone(22, "GMT+05:00 Yekaterinburg", "Asia/Yekaterinburg"),
    MyTimeZone(23, "GMT+05:30 Colombo", "Asia/Colombo"),
    MyTimeZone(24, "GMT+05:45 Kathmandu", "Asia/Kathmandu"),
    MyTimeZone(25, "GMT+08:00 Hong Kong", "Asia/Hong_Kong"),
    MyTimeZone(26, "GMT+08:00 Irkutsk", "Asia/Irkutsk"),
    MyTimeZone(27, "GMT+10:00 Vladivostok", "Asia/Vladivostok"),
    MyTimeZone(28, "GMT+10:00 Guam", "Pacific/Guam"),
    MyTimeZone(29, "GMT+10:00 Magadan", "Asia/Magadan"),

)

fun getTimeUntilNextAlarm(alarmTimeInMinutes: Int, days: Int): Int? {
    val calendar = Calendar.getInstance()
    calendar.firstDayOfWeek = Calendar.MONDAY
    val currentTimeInMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
    val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY

    var minTimeDifferenceInMinutes = Int.MAX_VALUE

    for (i in 0..6) {
        val alarmDayOfWeek = (currentDayOfWeek + i) % 7
        if (isAlarmEnabledForDay(alarmDayOfWeek, days)) {
            val timeDifferenceInMinutes = getTimeDifferenceInMinutes(currentTimeInMinutes, alarmTimeInMinutes, i)
            if (timeDifferenceInMinutes < minTimeDifferenceInMinutes) {
                minTimeDifferenceInMinutes = timeDifferenceInMinutes
            }
        }
    }

    return if (minTimeDifferenceInMinutes != Int.MAX_VALUE) {
        minTimeDifferenceInMinutes
    } else {
        null
    }
}

fun isAlarmEnabledForDay(day: Int, alarmDays: Int) = alarmDays.isBitSet(day)

fun getTimeDifferenceInMinutes(currentTimeInMinutes: Int, alarmTimeInMinutes: Int, daysUntilAlarm: Int): Int {
    val minutesInADay = 24 * 60
    val minutesUntilAlarm = daysUntilAlarm * minutesInADay + alarmTimeInMinutes
    return if (minutesUntilAlarm > currentTimeInMinutes) {
        minutesUntilAlarm - currentTimeInMinutes
    } else {
        minutesInADay - (currentTimeInMinutes - minutesUntilAlarm)
    }
}
