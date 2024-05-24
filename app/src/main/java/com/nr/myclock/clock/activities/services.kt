package com.nr.myclock.clock.activities

import android.app.IntentService
import android.content.Intent
import com.nr.myclock.clock.activities.extensions.config
import com.nr.myclock.clock.activities.extensions.dbHelper
import com.nr.myclock.clock.activities.extensions.hideNotification
import com.nr.myclock.clock.activities.extensions.setupAlarmClock
import com.nr.myclock.clock.activities.helpers.ALARM_ID
import org.fossify.commons.helpers.MINUTE_SECONDS
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.nr.myclock.clock.activities.extensions.getFormattedDuration
import com.nr.myclock.clock.activities.extensions.getOpenStopwatchTabIntent
import com.nr.myclock.clock.activities.helpers.STOPWATCH_RUNNING_NOTIF_ID
import com.nr.myclock.clock.activities.helpers.Stopwatch
import com.nr.myclock.clock.activities.helpers.Stopwatch.State
import com.nr.myclock.clock.activities.helpers.Stopwatch.UpdateListener
import org.fossify.commons.extensions.showErrorToast
import org.fossify.commons.helpers.isOreoPlus
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import android.app.Notification
import com.nr.myclock.R
import com.nr.myclock.clock.activities.extensions.getOpenTimerTabIntent
import com.nr.myclock.clock.activities.extensions.timerHelper
import com.nr.myclock.clock.activities.helpers.INVALID_TIMER_ID
import com.nr.myclock.clock.activities.helpers.TIMER_RUNNING_NOTIF_ID
import com.nr.myclock.clock.activities.models.EventTimer
import com.nr.myclock.clock.activities.models.StateTimer


class SnoozeService : IntentService("Snooze") {
    override fun onHandleIntent(intent: Intent?) {
        val id = intent!!.getIntExtra(ALARM_ID, -1)
        val alarm = dbHelper.getAlarmWithId(id) ?: return
        hideNotification(id)
        setupAlarmClock(alarm, config.snoozeTime * MINUTE_SECONDS)
    }
}

class TimerService : Service() {
    private val bus = EventBus.getDefault()
    private var isStopping = false

    override fun onCreate() {
        super.onCreate()
        bus.register(this)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        isStopping = false
        updateNotification()
        startForeground(
            TIMER_RUNNING_NOTIF_ID, notification(getString(R.string.app_name), getString(
                R.string.timers_notification_msg
            ), INVALID_TIMER_ID
            ))
        return START_NOT_STICKY
    }

    private fun updateNotification() {
        timerHelper.getTimers { timers ->
            val runningTimers = timers.filter { it.state is StateTimer.Running }
            if (runningTimers.isNotEmpty()) {
                val firstTimer = runningTimers.first()
                val formattedDuration = (firstTimer.state as StateTimer.Running).tick.getFormattedDuration()
                val contextText = when {
                    firstTimer.label.isNotEmpty() -> getString(R.string.timer_single_notification_label_msg, firstTimer.label)
                    else -> resources.getQuantityString(R.plurals.timer_notification_msg, runningTimers.size, runningTimers.size)
                }

                Handler(Looper.getMainLooper()).post {
                    try {
                        startForeground(TIMER_RUNNING_NOTIF_ID, notification(formattedDuration, contextText, firstTimer.id!!))
                    } catch (e: Exception) {
                        showErrorToast(e)
                    }
                }
            } else {
                stopService()
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: TimerStopService) {
        stopService()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: EventTimer.Refresh) {
        if (!isStopping) {
            updateNotification()
        }
    }

    private fun stopService() {
        isStopping = true
        if (isOreoPlus()) {
            stopForeground(true)
        } else {
            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bus.unregister(this)
    }

    private fun notification(title: String, contentText: String, firstRunningTimerId: Int): Notification {
        val channelId = "simple_alarm_timer"
        val label = getString(R.string.timer)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (isOreoPlus()) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            NotificationChannel(channelId, label, importance).apply {
                setSound(null, null)
                notificationManager.createNotificationChannel(this)
            }
        }

        val builder = NotificationCompat.Builder(this)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_hourglass_vector)
            .setPriority(Notification.PRIORITY_DEFAULT)
            .setSound(null)
            .setOngoing(true)
            .setAutoCancel(true)
            .setChannelId(channelId)

        if (firstRunningTimerId != INVALID_TIMER_ID) {
            builder.setContentIntent(this.getOpenTimerTabIntent(firstRunningTimerId))
        }

        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        return builder.build()
    }
}

fun startTimerService(context: Context) {
    Handler(Looper.getMainLooper()).post {
        try {
            ContextCompat.startForegroundService(context, Intent(context, TimerService::class.java))
        } catch (e: Exception) {
            context.showErrorToast(e)
        }
    }
}

object TimerStopService

class StopwatchService : Service() {
    private val bus = EventBus.getDefault()
    private lateinit var notificationManager: NotificationManager
    private lateinit var notificationBuilder: NotificationCompat.Builder
    private var isStopping = false

    override fun onCreate() {
        super.onCreate()
        bus.register(this)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationBuilder = getServiceNotificationBuilder(
            getString(R.string.app_name),
            getString(R.string.stopwatch)
        )
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        isStopping = false
        startForeground(
            STOPWATCH_RUNNING_NOTIF_ID,
            notificationBuilder.build()
        )
        Stopwatch.addUpdateListener(updateListener)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        bus.unregister(this)
        Stopwatch.removeUpdateListener(updateListener)
        super.onDestroy()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: StopwatchStopService) {
        isStopping = true
        stopForegroundService()
    }

    private fun getServiceNotificationBuilder(
        title: String,
        contentText: String,
    ): NotificationCompat.Builder {
        val channelId = "simple_alarm_stopwatch"
        val label = getString(R.string.stopwatch)
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        if (isOreoPlus()) {
            NotificationChannel(channelId, label, importance).apply {
                setSound(null, null)
                notificationManager.createNotificationChannel(this)
            }
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_stopwatch_vector)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setSound(null)
            .setOngoing(true)
            .setAutoCancel(true)
            .setContentIntent(getOpenStopwatchTabIntent())
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
    }

    private fun updateNotification(totalTime: Long) {
        val formattedDuration = totalTime.getFormattedDuration()
        notificationBuilder.setContentTitle(formattedDuration).setContentText(getString(R.string.stopwatch))
        notificationManager.notify(STOPWATCH_RUNNING_NOTIF_ID, notificationBuilder.build())
    }

    private val updateListener = object : UpdateListener {
        private val MIN_NOTIFICATION_UPDATE_INTERVAL = 500L
        private var lastUpdateTime = 0L
        override fun onUpdate(totalTime: Long, lapTime: Long, useLongerMSFormat: Boolean) {
            if (!isStopping && shouldNotificationBeUpdated()) {
                lastUpdateTime = System.currentTimeMillis()
                updateNotification(totalTime)
            }
        }

        override fun onStateChanged(state: State) {
            if (state == State.STOPPED) {
                stopForegroundService()
            }
        }

        private fun shouldNotificationBeUpdated(): Boolean {
            return (System.currentTimeMillis() - lastUpdateTime) > MIN_NOTIFICATION_UPDATE_INTERVAL
        }
    }

    private fun stopForegroundService() {
        ServiceCompat.stopForeground(this@StopwatchService, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
}

fun startStopwatchService(context: Context) {
    Handler(Looper.getMainLooper()).post {
        try {
            ContextCompat.startForegroundService(context, Intent(context, StopwatchService::class.java))
        } catch (e: Exception) {
            context.showErrorToast(e)
        }
    }
}

object StopwatchStopService
