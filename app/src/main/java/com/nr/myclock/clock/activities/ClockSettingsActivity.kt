package com.nr.myclock.clock.activities


import android.os.Bundle
import com.nr.myclock.databinding.ActivitySettingsBinding
import com.nr.myclock.clock.activities.extensions.config
import com.nr.myclock.clock.activities.helpers.DEFAULT_MAX_ALARM_REMINDER_SECS
import com.nr.myclock.clock.activities.helpers.DEFAULT_MAX_TIMER_REMINDER_SECS
import org.fossify.commons.extensions.*
import org.fossify.commons.helpers.MINUTE_SECONDS
import org.fossify.commons.helpers.NavigationIcon
import org.fossify.commons.helpers.isTiramisuPlus
import java.util.Locale
import kotlin.system.exitProcess

class ClockSettingsActivity : SimpleActivity() {
    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun onResume() {
        super.onResume()
        setupToolbar(binding.settingsToolbar, NavigationIcon.Arrow)
        setupUseEnglish()
        setupLanguage()
        setupAlarmMaxReminder()
        setupUseSameSnooze()
        setupSnoozeTime()
        setupTimerMaxReminder()
        updateTextColors(binding.settingsHolder)

        arrayOf(
            binding.settingsGeneralSettingsLabel,
            binding.settingsAlarmTabLabel,
            binding.settingsTimerTabLabel,
        ).forEach {
            it.setTextColor(getProperPrimaryColor())
        }
    }


    private fun setupUseEnglish() {
        binding.settingsUseEnglishHolder.beVisibleIf((config.wasUseEnglishToggled || Locale.getDefault().language != "en") && !isTiramisuPlus())
        binding.settingsUseEnglish.isChecked = config.useEnglish
        binding.settingsUseEnglishHolder.setOnClickListener {
            binding.settingsUseEnglish.toggle()
            config.useEnglish = binding.settingsUseEnglish.isChecked
            exitProcess(0)
        }
    }

    private fun setupLanguage() {
        binding.settingsLanguage.text = Locale.getDefault().displayLanguage
        binding.settingsLanguageHolder.beVisibleIf(isTiramisuPlus())
        binding.settingsLanguageHolder.setOnClickListener {
            launchChangeAppLanguageIntent()
        }
    }

    private fun setupAlarmMaxReminder() {
        updateAlarmMaxReminderText()
        binding.settingsAlarmMaxReminderHolder.setOnClickListener {
            showPickSecondsDialog(config.alarmMaxReminderSecs, true, true) {
                config.alarmMaxReminderSecs = if (it != 0) it else DEFAULT_MAX_ALARM_REMINDER_SECS
                updateAlarmMaxReminderText()
            }
        }
    }

    private fun setupUseSameSnooze() {
        binding.settingsSnoozeTimeHolder.beVisibleIf(config.useSameSnooze)
        binding.settingsUseSameSnooze.isChecked = config.useSameSnooze
        binding.settingsUseSameSnoozeHolder.setOnClickListener {
            binding.settingsUseSameSnooze.toggle()
            config.useSameSnooze = binding.settingsUseSameSnooze.isChecked
            binding.settingsSnoozeTimeHolder.beVisibleIf(config.useSameSnooze)
        }
    }

    private fun setupSnoozeTime() {
        updateSnoozeText()
        binding.settingsSnoozeTimeHolder.setOnClickListener {
            showPickSecondsDialog(config.snoozeTime * MINUTE_SECONDS, true) {
                config.snoozeTime = it / MINUTE_SECONDS
                updateSnoozeText()
            }
        }
    }

    private fun setupTimerMaxReminder() {
        updateTimerMaxReminderText()
        binding.settingsTimerMaxReminderHolder.setOnClickListener {
            showPickSecondsDialog(config.timerMaxReminderSecs, true, true) {
                config.timerMaxReminderSecs = if (it != 0) it else DEFAULT_MAX_TIMER_REMINDER_SECS
                updateTimerMaxReminderText()
            }
        }
    }
    private fun updateSnoozeText() {
        binding.settingsSnoozeTime.text = formatMinutesToTimeString(config.snoozeTime)
    }

    private fun updateAlarmMaxReminderText() {
        binding.settingsAlarmMaxReminder.text = formatSecondsToTimeString(config.alarmMaxReminderSecs)
    }

    private fun updateTimerMaxReminderText() {
        binding.settingsTimerMaxReminder.text = formatSecondsToTimeString(config.timerMaxReminderSecs)
    }
}
