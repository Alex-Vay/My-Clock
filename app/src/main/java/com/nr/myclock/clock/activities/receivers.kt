package com.nr.myclock.clock.activities

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.nr.myclock.clock.activities.extensions.updateWidgets
import com.nr.myclock.clock.activities.extensions.dbHelper
import com.nr.myclock.clock.activities.extensions.deleteNotificationChannel
import com.nr.myclock.clock.activities.extensions.hideNotification
import com.nr.myclock.clock.activities.helpers.ALARM_ID
import com.nr.myclock.clock.activities.helpers.ALARM_NOTIFICATION_CHANNEL_ID
import org.fossify.commons.helpers.ensureBackgroundThread
import com.nr.myclock.clock.activities.extensions.getClosestEnabledAlarmString
import com.nr.myclock.clock.activities.extensions.getDismissAlarmPendingIntent
import com.nr.myclock.clock.activities.extensions.getOpenAlarmTabIntent
import com.nr.myclock.clock.activities.helpers.EARLY_ALARM_DISMISSAL_CHANNEL_ID
import com.nr.myclock.clock.activities.helpers.EARLY_ALARM_NOTIF_ID
import org.fossify.commons.helpers.isOreoPlus
import android.app.PendingIntent
import android.os.Build
import android.os.Handler
import androidx.annotation.RequiresApi
import com.nr.myclock.R
import com.nr.myclock.clock.activities.extensions.cancelAlarmClock
import com.nr.myclock.clock.activities.extensions.config
import com.nr.myclock.clock.activities.helpers.ALARM_NOTIF_ID
import org.fossify.commons.extensions.showErrorToast
import com.nr.myclock.clock.activities.helpers.NOTIFICATION_ID
import com.nr.myclock.clock.activities.models.Alarm
import org.fossify.commons.extensions.removeBit
import java.util.Calendar
import kotlin.math.pow
import com.nr.myclock.clock.activities.extensions.rescheduleEnabledAlarms
import com.nr.myclock.clock.activities.extensions.hideTimerNotification
import com.nr.myclock.clock.activities.extensions.isScreenOn
import com.nr.myclock.clock.activities.extensions.scheduleNextAlarm
import com.nr.myclock.clock.activities.extensions.showAlarmNotification
import com.nr.myclock.clock.activities.helpers.INVALID_TIMER_ID
import com.nr.myclock.clock.activities.helpers.TIMER_ID
import com.nr.myclock.clock.activities.models.EventTimer
import org.greenrobot.eventbus.EventBus

class UpdateWidgetReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        context.updateWidgets()
    }
}

class HideAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra(ALARM_ID, -1)
        val channelId = intent.getStringExtra(ALARM_NOTIFICATION_CHANNEL_ID)
        channelId?.let { context.deleteNotificationChannel(channelId) }
        context.hideNotification(id)

        ensureBackgroundThread {
            val alarm = context.dbHelper.getAlarmWithId(id)
            if (alarm != null && alarm.days < 0) {
                if (alarm.oneShot) {
                    alarm.isEnabled = false
                    context.dbHelper.deleteAlarms(arrayListOf(alarm))
                } else {
                    context.dbHelper.updateAlarmEnabledState(alarm.id, false)
                }
                context.updateWidgets()
            }
        }
    }
}

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        context.rescheduleEnabledAlarms()
    }
}


class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra(ALARM_ID, -1)
        val alarm = context.dbHelper.getAlarmWithId(id) ?: return

        context.hideNotification(EARLY_ALARM_NOTIF_ID) // hide early dismissal notification if not already dismissed

        if (context.isScreenOn()) {
            context.showAlarmNotification(alarm)
            Handler().postDelayed({
                context.hideNotification(id)
            }, context.config.alarmMaxReminderSecs * 1000L)
        } else {
            if (isOreoPlus()) {

                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                if (notificationManager.getNotificationChannel(ALARM_NOTIFICATION_CHANNEL_ID) == null) {
                    oldNotificationChannelCleanup(notificationManager) // cleans up previous notification channel that had sound properties
                    NotificationChannel(ALARM_NOTIFICATION_CHANNEL_ID, "Alarm", NotificationManager.IMPORTANCE_HIGH).apply {
                        setBypassDnd(true)
                        setSound(null, null)
                        notificationManager.createNotificationChannel(this)
                    }
                }

                val pendingIntent = PendingIntent.getActivity(context, 0, Intent(context, ClockReminderActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    putExtra(ALARM_ID, id)
                }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

                val builder = NotificationCompat.Builder(context, ALARM_NOTIFICATION_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_alarm_vector)
                    .setContentTitle(context.getString(org.fossify.commons.R.string.alarm))
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setFullScreenIntent(pendingIntent, true)

                try {
                    notificationManager.notify(ALARM_NOTIF_ID, builder.build())
                } catch (e: Exception) {
                    context.showErrorToast(e)
                }
            } else {
                Intent(context, ClockReminderActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    putExtra(ALARM_ID, id)
                    context.startActivity(this)
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun oldNotificationChannelCleanup(notificationManager: NotificationManager) {
        notificationManager.deleteNotificationChannel("Alarm")
    }
}

class DismissAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getIntExtra(ALARM_ID, -1)
        val notificationId = intent.getIntExtra(NOTIFICATION_ID, -1)
        if (alarmId == -1) {
            return
        }

        context.hideNotification(notificationId)

        ensureBackgroundThread {
            context.dbHelper.getAlarmWithId(alarmId)?.let { alarm ->
                context.cancelAlarmClock(alarm)
                scheduleNextAlarm(alarm, context)
                if (alarm.days < 0) {
                    if (alarm.oneShot) {
                        alarm.isEnabled = false
                        context.dbHelper.deleteAlarms(arrayListOf(alarm))
                    } else {
                        context.dbHelper.updateAlarmEnabledState(alarm.id, false)
                    }
                    context.updateWidgets()
                }
            }
        }
    }

    private fun scheduleNextAlarm(alarm: Alarm, context: Context) {
        val oldBitmask = alarm.days
        alarm.days = removeTodayFromBitmask(oldBitmask)
        context.scheduleNextAlarm(alarm, false)
        alarm.days = oldBitmask
    }

    private fun removeTodayFromBitmask(bitmask: Int): Int {
        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = Calendar.MONDAY
        val dayOfWeek = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7
        val todayBitmask = 2.0.pow(dayOfWeek).toInt()
        return bitmask.removeBit(todayBitmask)
    }
}

class EarlyAlarmDismissalReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getIntExtra(ALARM_ID, -1)
        if (alarmId == -1) {
            return
        }

        triggerEarlyDismissalNotification(context, alarmId)
    }

    private fun triggerEarlyDismissalNotification(context: Context, alarmId: Int) {
        context.getClosestEnabledAlarmString { alarmString ->
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (isOreoPlus()) {
                NotificationChannel(
                    EARLY_ALARM_DISMISSAL_CHANNEL_ID,
                    context.getString(R.string.early_alarm_dismissal),
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    setBypassDnd(true)
                    setSound(null, null)
                    notificationManager.createNotificationChannel(this)
                }
            }
            val dismissIntent = context.getDismissAlarmPendingIntent(alarmId, EARLY_ALARM_NOTIF_ID)
            val contentIntent = context.getOpenAlarmTabIntent()
            val notification = NotificationCompat.Builder(context)
                .setContentTitle(context.getString(R.string.upcoming_alarm))
                .setContentText(alarmString)
                .setSmallIcon(R.drawable.ic_alarm_vector)
                .setPriority(Notification.PRIORITY_LOW)
                .addAction(0, context.getString(org.fossify.commons.R.string.dismiss), dismissIntent)
                .setContentIntent(contentIntent)
                .setSound(null)
                .setAutoCancel(true)
                .setChannelId(EARLY_ALARM_DISMISSAL_CHANNEL_ID)
                .build()

            notificationManager.notify(EARLY_ALARM_NOTIF_ID, notification)
        }
    }

}

class HideTimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val timerId = intent.getIntExtra(TIMER_ID, INVALID_TIMER_ID)
        context.hideTimerNotification(timerId)
        EventBus.getDefault().post(EventTimer.Reset(timerId))
    }
}