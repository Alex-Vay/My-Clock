package com.nr.myclock.clock.activities.dialogs

import com.nr.myclock.R
import com.nr.myclock.databinding.DialogChangeAlarmSortBinding
import com.nr.myclock.clock.activities.extensions.config
import com.nr.myclock.clock.activities.helpers.SORT_BY_ALARM_TIME
import com.nr.myclock.clock.activities.helpers.SORT_BY_CREATION_ORDER
import com.nr.myclock.clock.activities.helpers.SORT_BY_DATE_AND_TIME
import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.extensions.getAlertDialogBuilder
import org.fossify.commons.extensions.setupDialogStuff

class ChangeClockAlarmSortDialog(val activity: BaseSimpleActivity, val callback: () -> Unit) {
    private val binding = DialogChangeAlarmSortBinding.inflate(activity.layoutInflater).apply {
        val activeRadioButton = when (activity.config.alarmSort) {
            SORT_BY_ALARM_TIME -> sortingDialogRadioAlarmTime
            SORT_BY_DATE_AND_TIME -> sortingDialogRadioDayAndTime
            else -> sortingDialogRadioCreationOrder
        }
        activeRadioButton.isChecked = true
    }

    init {
        activity.getAlertDialogBuilder()
            .setPositiveButton(org.fossify.commons.R.string.ok) { _, _ -> dialogConfirmed() }
            .setNegativeButton(org.fossify.commons.R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this, org.fossify.commons.R.string.sort_by)
            }
    }

    private fun dialogConfirmed() {
        val sort = when (binding.sortingDialogRadioSorting.checkedRadioButtonId) {
            R.id.sorting_dialog_radio_alarm_time -> SORT_BY_ALARM_TIME
            R.id.sorting_dialog_radio_day_and_time -> SORT_BY_DATE_AND_TIME
            else -> SORT_BY_CREATION_ORDER
        }

        activity.config.alarmSort = sort
        callback()
    }
}
