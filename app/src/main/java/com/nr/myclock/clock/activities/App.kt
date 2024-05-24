package com.nr.myclock.clock.activities

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.facebook.stetho.Stetho
import com.nr.myclock.BuildConfig
import com.nr.myclock.clock.activities.extensions.config
import com.nr.myclock.clock.activities.extensions.getOpenTimerTabIntent
import com.nr.myclock.clock.activities.extensions.getTimerNotification
import com.nr.myclock.clock.activities.extensions.hideNotification
import com.nr.myclock.clock.activities.extensions.timerHelper
import com.nr.myclock.clock.activities.helpers.Stopwatch
import com.nr.myclock.clock.activities.helpers.Stopwatch.State
import com.nr.myclock.clock.activities.models.EventTimer
import com.nr.myclock.clock.activities.models.StateTimer
import org.fossify.commons.extensions.checkUseEnglish
import org.fossify.commons.extensions.showErrorToast
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class App : Application(), LifecycleObserver {

    private var countDownTimers = mutableMapOf<Int, CountDownTimer>()

    override fun onCreate() {
        super.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        EventBus.getDefault().register(this)

        if (BuildConfig.DEBUG) {
            Stetho.initializeWithDefaults(this)
        }

        checkUseEnglish()
    }

    override fun onTerminate() {
        EventBus.getDefault().unregister(this)
        super.onTerminate()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    private fun onAppBackgrounded() {
        timerHelper.getTimers { timers ->
            if (timers.any { it.state is StateTimer.Running }) {
                startTimerService(this)
            }
        }
        if (Stopwatch.state == State.RUNNING) {
            startStopwatchService(this)
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    private fun onAppForegrounded() {
        EventBus.getDefault().post(TimerStopService)
        timerHelper.getTimers { timers ->
            val runningTimers = timers.filter { it.state is StateTimer.Running }
            runningTimers.forEach { timer ->
                if (countDownTimers[timer.id] == null) {
                    EventBus.getDefault().post(EventTimer.Start(timer.id!!, (timer.state as StateTimer.Running).tick))
                }
            }
        }
        if (Stopwatch.state == State.RUNNING) {
            EventBus.getDefault().post(StopwatchStopService)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: EventTimer.Reset) {
        updateTimerState(event.timerId, StateTimer.Idle)
        countDownTimers[event.timerId]?.cancel()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: EventTimer.Delete) {
        countDownTimers[event.timerId]?.cancel()
        timerHelper.deleteTimer(event.timerId) {
            EventBus.getDefault().post(EventTimer.Refresh)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: EventTimer.Start) {
        val countDownTimer = object : CountDownTimer(event.duration, 1000) {
            override fun onTick(tick: Long) {
                updateTimerState(event.timerId, StateTimer.Running(event.duration, tick))
            }

            override fun onFinish() {
                EventBus.getDefault().post(EventTimer.Finish(event.timerId, event.duration))
                EventBus.getDefault().post(TimerStopService)
            }
        }.start()
        countDownTimers[event.timerId] = countDownTimer
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: EventTimer.Finish) {
        timerHelper.getTimer(event.timerId) { timer ->
            val pendingIntent = getOpenTimerTabIntent(event.timerId)
            val notification = getTimerNotification(timer, pendingIntent, false)
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            try {
                notificationManager.notify(event.timerId, notification)
            } catch (e: Exception) {
                showErrorToast(e)
            }

            updateTimerState(event.timerId, StateTimer.Finished)
            Handler(Looper.getMainLooper()).postDelayed({
                hideNotification(event.timerId)
            }, config.timerMaxReminderSecs * 1000L)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: EventTimer.Pause) {
        timerHelper.getTimer(event.timerId) { timer ->
            updateTimerState(event.timerId, StateTimer.Paused(event.duration, (timer.state as StateTimer.Running).tick))
            countDownTimers[event.timerId]?.cancel()
        }
    }

    private fun updateTimerState(timerId: Int, state: StateTimer) {
        timerHelper.getTimer(timerId) { timer ->
            val newTimer = timer.copy(state = state)
            if (newTimer.oneShot && state is StateTimer.Idle) {
                timerHelper.deleteTimer(newTimer.id!!) {
                    EventBus.getDefault().post(EventTimer.Refresh)
                }
            } else {
                timerHelper.insertOrUpdateTimer(newTimer) {
                    EventBus.getDefault().post(EventTimer.Refresh)
                }
            }
        }
    }
}
