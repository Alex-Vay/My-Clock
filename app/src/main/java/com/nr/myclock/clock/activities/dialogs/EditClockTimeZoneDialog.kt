package com.nr.myclock.clock.activities.dialogs

import com.nr.myclock.clock.activities.SimpleActivity
import com.nr.myclock.databinding.DialogEditTimeZoneBinding
import com.nr.myclock.clock.activities.extensions.config
import com.nr.myclock.clock.activities.extensions.getEditedTimeZonesMap
import com.nr.myclock.clock.activities.extensions.getModifiedTimeZoneTitle
import com.nr.myclock.clock.activities.helpers.EDITED_TIME_ZONE_SEPARATOR
import com.nr.myclock.clock.activities.helpers.getDefaultTimeZoneTitle
import com.nr.myclock.clock.activities.models.CurrentTimeZone
import org.fossify.commons.extensions.getAlertDialogBuilder
import org.fossify.commons.extensions.setupDialogStuff
import org.fossify.commons.extensions.showKeyboard
import org.fossify.commons.extensions.value

class EditClockTimeZoneDialog(val activity: SimpleActivity, val myTimeZone: CurrentTimeZone, val callback: () -> Unit) {

    init {
        val binding = DialogEditTimeZoneBinding.inflate(activity.layoutInflater).apply {
            editTimeZoneTitle.setText(activity.getModifiedTimeZoneTitle(myTimeZone.id))
            editTimeZoneLabel.setText(getDefaultTimeZoneTitle(myTimeZone.id))
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(org.fossify.commons.R.string.ok) { dialog, which -> dialogConfirmed(binding.editTimeZoneTitle.value) }
            .setNegativeButton(org.fossify.commons.R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this) { alertDialog ->
                    alertDialog.showKeyboard(binding.editTimeZoneTitle)
                }
            }
    }

    private fun dialogConfirmed(newTitle: String) {
        val editedTitlesMap = activity.getEditedTimeZonesMap()

        if (newTitle.isEmpty()) {
            editedTitlesMap.remove(myTimeZone.id)
        } else {
            editedTitlesMap[myTimeZone.id] = newTitle
        }

        val newTitlesSet = HashSet<String>()
        for ((key, value) in editedTitlesMap) {
            newTitlesSet.add("$key$EDITED_TIME_ZONE_SEPARATOR$value")
        }

        activity.config.editedTimeZoneTitles = newTitlesSet
        callback()
    }
}
