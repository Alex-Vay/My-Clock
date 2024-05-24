package com.nr.myclock.clock.activities.extensions.gson

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapterFactory
import com.nr.myclock.clock.activities.models.StateTimer

val timerStates = valueOf<StateTimer>()
    .registerSubtype(StateTimer.Idle::class.java)
    .registerSubtype(StateTimer.Running::class.java)
    .registerSubtype(StateTimer.Paused::class.java)
    .registerSubtype(StateTimer.Finished::class.java)

inline fun <reified T : Any> valueOf(): RuntimeTypeAdapterFactory<T> =
    RuntimeTypeAdapterFactory.of(T::class.java)

fun GsonBuilder.registerTypes(vararg types: TypeAdapterFactory) = apply {
    types.forEach { registerTypeAdapterFactory(it) }
}

val gson: Gson = GsonBuilder().registerTypes(timerStates).create()