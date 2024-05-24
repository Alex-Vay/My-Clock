package com.nr.myclock.clock.activities.models

import androidx.annotation.Keep

@Keep
sealed class StateTimer {
    @Keep
    object Idle : StateTimer()

    @Keep
    data class Running(val duration: Long, val tick: Long) : StateTimer()

    @Keep
    data class Paused(val duration: Long, val tick: Long) : StateTimer()

    @Keep
    object Finished : StateTimer()
}
