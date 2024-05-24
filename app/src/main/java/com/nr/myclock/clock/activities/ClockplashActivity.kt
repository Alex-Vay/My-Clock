package com.nr.myclock.clock.activities

import android.content.Intent
import com.nr.myclock.clock.activities.helpers.INVALID_TIMER_ID
import com.nr.myclock.clock.activities.helpers.OPEN_TAB
import com.nr.myclock.clock.activities.helpers.STOPWATCH_TOGGLE_ACTION
import com.nr.myclock.clock.activities.helpers.TAB_ALARM
import com.nr.myclock.clock.activities.helpers.TAB_CLOCK
import com.nr.myclock.clock.activities.helpers.TAB_STOPWATCH
import com.nr.myclock.clock.activities.helpers.TAB_TIMER
import com.nr.myclock.clock.activities.helpers.TIMER_ID
import com.nr.myclock.clock.activities.helpers.TOGGLE_STOPWATCH
import org.fossify.commons.activities.BaseSplashActivity

class ClockplashActivity : BaseSplashActivity() {
    override fun initActivity() {
        when {
            intent?.action == "android.intent.action.SHOW_ALARMS" -> {
                Intent(this, MainActivity::class.java).apply {
                    putExtra(OPEN_TAB, TAB_ALARM)
                    startActivity(this)
                }
            }

            intent?.action == "android.intent.action.SHOW_TIMERS" -> {
                Intent(this, MainActivity::class.java).apply {
                    putExtra(OPEN_TAB, TAB_TIMER)
                    startActivity(this)
                }
            }

            intent?.action == STOPWATCH_TOGGLE_ACTION -> {
                Intent(this, MainActivity::class.java).apply {
                    putExtra(OPEN_TAB, TAB_STOPWATCH)
                    putExtra(TOGGLE_STOPWATCH, intent.getBooleanExtra(TOGGLE_STOPWATCH, false))
                    startActivity(this)
                }
            }

            intent.extras?.containsKey(OPEN_TAB) == true -> {
                Intent(this, MainActivity::class.java).apply {
                    putExtra(OPEN_TAB, intent.getIntExtra(OPEN_TAB, TAB_CLOCK))
                    putExtra(TIMER_ID, intent.getIntExtra(TIMER_ID, INVALID_TIMER_ID))
                    startActivity(this)
                }
            }

            ClockHandlerActivity.HANDLED_ACTIONS.contains(intent?.action) -> {
                Intent(intent).apply {
                    setClass(this@ClockplashActivity, ClockHandlerActivity::class.java)
                    startActivity(this)
                }
            }

            else -> startActivity(Intent(this, MainActivity::class.java))
        }
        finish()
    }
}
