package com.nr.myclock.clock.activities.models

import com.nr.myclock.clock.activities.helpers.INVALID_TIMER_ID

sealed class EventTimer(open val timerId: Int) {
    data class Delete(override val timerId: Int) : EventTimer(timerId)
    data class Reset(override val timerId: Int) : EventTimer(timerId)
    data class Start(override val timerId: Int, val duration: Long) : EventTimer(timerId)
    data class Pause(override val timerId: Int, val duration: Long) : EventTimer(timerId)
    data class Finish(override val timerId: Int, val duration: Long) : EventTimer(timerId)
    object Refresh : EventTimer(INVALID_TIMER_ID)
}
