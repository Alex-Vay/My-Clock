package com.nr.myclock.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class SnoozeReminderActivity : AppCompatActivity() {
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
