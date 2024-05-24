package com.nr.myclock.clock.activities.dialogs

import com.nr.myclock.clock.activities.SimpleActivity
import com.nr.myclock.databinding.DialogMyTimePickerBinding
import org.fossify.commons.extensions.getAlertDialogBuilder
import org.fossify.commons.extensions.setupDialogStuff

class ClockTimePickerDialog(val activity: SimpleActivity, val initialSeconds: Int, val callback: (result: Int) -> Unit) {
    private val binding = DialogMyTimePickerBinding.inflate(activity.layoutInflater)

    init {
        binding.apply {
            myTimePickerHours.value = initialSeconds / 3600
            myTimePickerMinutes.value = (initialSeconds) / 60 % 60
            myTimePickerSeconds.value = initialSeconds % 60
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(org.fossify.commons.R.string.ok) { dialog, which -> dialogConfirmed() }
            .setNegativeButton(org.fossify.commons.R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this)
            }
    }

    private fun dialogConfirmed() {
        binding.apply {
            val hours = myTimePickerHours.value
            val minutes = myTimePickerMinutes.value
            val seconds = myTimePickerSeconds.value
            callback(hours * 3600 + minutes * 60 + seconds)
        }
    }
}
