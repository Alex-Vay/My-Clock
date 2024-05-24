package com.nr.myclock.clock.activities.extensions

import org.fossify.commons.extensions.getFormattedDuration
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

val Int.secondsToMillis get() = TimeUnit.SECONDS.toMillis(this.toLong())
val Int.millisToSeconds get() = TimeUnit.MILLISECONDS.toSeconds(this.toLong())

fun Int.isBitSet(bit: Int) = (this shr bit and 1) > 0

fun Long.formatStopwatchTime(useLongerMSFormat: Boolean): String {
    val MSFormat = if (useLongerMSFormat) "%03d" else "%01d"
    val hours = TimeUnit.MILLISECONDS.toHours(this)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(this) - TimeUnit.HOURS.toMinutes(hours)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(this) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(this))
    var ms = this % 1000
    if (!useLongerMSFormat) {
        ms /= 100
    }

    return when {
        hours > 0 -> {
            val format = "%02d:%02d:%02d.$MSFormat"
            String.format(format, hours, minutes, seconds, ms)
        }

        minutes > 0 -> {
            val format = "%02d:%02d.$MSFormat"
            String.format(format, minutes, seconds, ms)
        }

        else -> {
            val format = "%d.$MSFormat"
            String.format(format, seconds, ms)
        }
    }
}



fun Long.getFormattedDuration(forceShowHours: Boolean = false): String {
    return this.div(1000F).roundToInt().getFormattedDuration(forceShowHours)
}
