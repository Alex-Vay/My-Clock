package com.nr.myclock.clock.activities.dialogs

import com.nr.myclock.clock.activities.SimpleActivity
import com.nr.myclock.clock.activities.SelectTimeZonesAdapter
import com.nr.myclock.databinding.DialogSelectTimeZonesBinding
import com.nr.myclock.clock.activities.extensions.config
import com.nr.myclock.clock.activities.helpers.getAllTimeZones
import org.fossify.commons.extensions.getAlertDialogBuilder
import org.fossify.commons.extensions.setupDialogStuff

class AddClockTimeZonesDialog(val activity: SimpleActivity, private val callback: () -> Unit) {
    private val binding = DialogSelectTimeZonesBinding.inflate(activity.layoutInflater)

    init {
        binding.selectTimeZonesList.adapter = SelectTimeZonesAdapter(activity, getAllTimeZones())

        activity.getAlertDialogBuilder()
            .setPositiveButton(org.fossify.commons.R.string.ok) { dialog, which -> dialogConfirmed() }
            .setNegativeButton(org.fossify.commons.R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this)
            }
    }

    private fun dialogConfirmed() {
        val adapter = binding.selectTimeZonesList.adapter as? SelectTimeZonesAdapter
        val selectedTimeZones = adapter?.selectedKeys?.map { it.toString() }?.toHashSet() ?: LinkedHashSet()
        activity.config.selectedTimeZones = selectedTimeZones
        callback()
    }
}
