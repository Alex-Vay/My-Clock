package com.nr.myclock.clock.activities.models

sealed interface EventAlarm {
    object Refresh : EventAlarm
}
