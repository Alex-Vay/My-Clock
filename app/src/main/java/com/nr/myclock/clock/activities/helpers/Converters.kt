package com.nr.myclock.clock.activities.helpers

import androidx.room.TypeConverter
import com.nr.myclock.clock.activities.extensions.gson.gson
import com.nr.myclock.clock.activities.models.StateWrapper
import com.nr.myclock.clock.activities.models.StateTimer

class Converters {

    @TypeConverter
    fun jsonToTimerState(value: String): StateTimer {
        return try {
            gson.fromJson(value, StateWrapper::class.java).state
        } catch (e: Exception) {
            StateTimer.Idle
        }
    }

    @TypeConverter
    fun timerStateToJson(state: StateTimer) = gson.toJson(StateWrapper(state))
}
