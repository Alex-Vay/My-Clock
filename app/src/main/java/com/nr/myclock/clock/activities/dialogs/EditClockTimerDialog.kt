package com.nr.myclock.clock.activities.dialogs

import android.media.AudioManager
import android.media.RingtoneManager
import androidx.appcompat.app.AlertDialog
import com.nr.myclock.clock.activities.SimpleActivity
import com.nr.myclock.clock.activities.extensions.checkAlarmsWithDeletedSoundUri
import com.nr.myclock.clock.activities.extensions.config
import com.nr.myclock.clock.activities.extensions.getFormattedTime
import com.nr.myclock.clock.activities.extensions.timerHelper
import com.nr.myclock.databinding.DialogEditTimerBinding
import com.nr.myclock.clock.activities.helpers.PICK_AUDIO_FILE_INTENT_ID
import com.nr.myclock.clock.activities.models.Timer
import org.fossify.commons.dialogs.SelectAlarmSoundDialog
import org.fossify.commons.extensions.*
import org.fossify.commons.models.AlarmSound

class EditClockTimerDialog(val activity: SimpleActivity, val timer: Timer, val callback: (id: Long) -> Unit) {
    private val binding = DialogEditTimerBinding.inflate(activity.layoutInflater)
    private val textColor = activity.getProperTextColor()  //цвет текста редакт. таймера

    init {
        restoreLastAlarm()
        updateAlarmTime()

        binding.apply {
            //editTimerInitialTime.colorCompoundDrawable(textColor)
            editTimerInitialTime.text = timer.seconds.getFormattedDuration()
            editTimerInitialTime.setTextColor(textColor)
            editTimerInitialTime.setOnClickListener {
                changeDuration(timer)
            }

            editTimerVibrateIcon.setColorFilter(textColor)
            editTimerVibrate.isChecked = timer.vibrate
            editTimerVibrate.setTextColor(textColor)
            editTimerVibrateHolder.setOnClickListener {
                editTimerVibrate.toggle()
                timer.vibrate = editTimerVibrate.isChecked
                timer.channelId = null
            }


            editTimerSound.text = timer.soundTitle
            editTimerSound.setOnClickListener {
                SelectAlarmSoundDialog(activity, timer.soundUri, AudioManager.STREAM_ALARM, PICK_AUDIO_FILE_INTENT_ID,
                    RingtoneManager.TYPE_ALARM, true,
                    onAlarmPicked = { sound ->
                        if (sound != null) {
                            updateAlarmSound(sound)
                        }
                    },
                    onAlarmSoundDeleted = { sound ->
                        if (timer.soundUri == sound.uri) {
                            val defaultAlarm = root.context.getDefaultAlarmSound(RingtoneManager.TYPE_ALARM)
                            updateAlarmSound(defaultAlarm)
                        }

                        root.context.checkAlarmsWithDeletedSoundUri(sound.uri)
                    })
            }

            editTimerLabelImage.applyColorFilter(textColor)
            editTimer.setText(timer.label)
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(org.fossify.commons.R.string.ok, null)
            .setNegativeButton(org.fossify.commons.R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this) { alertDialog ->
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        timer.label = binding.editTimer.value
                        activity.timerHelper.insertOrUpdateTimer(timer) {
                            activity.config.timerLastConfig = timer
                            callback(it)
                            alertDialog.dismiss()
                        }
                    }
                }
            }
    }

    private fun restoreLastAlarm() {
        if (timer.id == null) {
            activity.config.timerLastConfig?.let { lastConfig ->
                timer.label = lastConfig.label
                timer.seconds = lastConfig.seconds
                timer.soundTitle = lastConfig.soundTitle
                timer.soundUri = lastConfig.soundUri
                timer.vibrate = lastConfig.vibrate
            }
        }
    }

    private fun updateAlarmTime() {
        binding.editTimerInitialTime.text = activity.getFormattedTime(timer.seconds * 60, false, true)
    }

    private fun changeDuration(timer: Timer) {
        ClockTimePickerDialog(activity, timer.seconds) { seconds ->
            val timerSeconds = if (seconds <= 0) 10 else seconds
            timer.seconds = timerSeconds
            binding.editTimerInitialTime.text = timerSeconds.getFormattedDuration()
        }
    }

    fun updateAlarmSound(alarmSound: AlarmSound) {
        timer.soundTitle = alarmSound.title
        timer.soundUri = alarmSound.uri
        timer.channelId = null
        binding.editTimerSound.text = alarmSound.title
    }
}
