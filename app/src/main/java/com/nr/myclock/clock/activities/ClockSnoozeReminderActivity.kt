package com.nr.myclock.clock.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.nr.myclock.clock.activities.extensions.config
import com.nr.myclock.clock.activities.extensions.dbHelper
import com.nr.myclock.clock.activities.extensions.hideNotification
import com.nr.myclock.clock.activities.extensions.setupAlarmClock
import com.nr.myclock.clock.activities.helpers.ALARM_ID
import org.fossify.commons.extensions.showPickSecondsDialog
import org.fossify.commons.helpers.MINUTE_SECONDS

class ClockSnoozeReminderActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent.getIntExtra(ALARM_ID, -1)
        val alarm = dbHelper.getAlarmWithId(id) ?: return
        hideNotification(id)
        showPickSecondsDialog(config.snoozeTime * MINUTE_SECONDS, true, cancelCallback = { dialogCancelled() }) {
            config.snoozeTime = it / MINUTE_SECONDS
            setupAlarmClock(alarm, it)
            finishActivity()
        }
    }

    private fun dialogCancelled() {
        finishActivity()
    }

    private fun finishActivity() {
        finish()
        overridePendingTransition(0, 0)
    }
}
