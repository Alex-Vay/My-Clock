package com.nr.myclock.clock.activities.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.graphics.Color
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.nr.myclock.clock.activities.MainActivity
import com.nr.myclock.clock.activities.SimpleActivity
import com.nr.myclock.clock.activities.ClocksAdapter
import com.nr.myclock.databinding.FragmentAlarmBinding
import com.nr.myclock.clock.activities.dialogs.ChangeClockAlarmSortDialog
import com.nr.myclock.clock.activities.dialogs.EditClockAlarmDialog
import com.nr.myclock.clock.activities.ToggleAlarmInterface
import com.nr.myclock.clock.activities.extensions.cancelAlarmClock
import com.nr.myclock.clock.activities.extensions.config
import com.nr.myclock.clock.activities.extensions.createNewAlarm
import com.nr.myclock.clock.activities.extensions.dbHelper
import com.nr.myclock.clock.activities.extensions.firstDayOrder
import com.nr.myclock.clock.activities.extensions.getEnabledAlarms
import com.nr.myclock.clock.activities.extensions.handleFullScreenNotificationsPermission
import com.nr.myclock.clock.activities.extensions.scheduleNextAlarm
import com.nr.myclock.clock.activities.extensions.updateWidgets
import com.nr.myclock.clock.activities.helpers.DEFAULT_ALARM_MINUTES
import com.nr.myclock.clock.activities.helpers.SORT_BY_ALARM_TIME
import com.nr.myclock.clock.activities.helpers.SORT_BY_DATE_AND_TIME
import com.nr.myclock.clock.activities.helpers.TODAY_BIT
import com.nr.myclock.clock.activities.helpers.getCurrentDayMinutes
import com.nr.myclock.clock.activities.helpers.getTomorrowBit
import com.nr.myclock.clock.activities.models.Alarm
import com.nr.myclock.clock.activities.models.EventAlarm
import org.fossify.commons.extensions.toast
import org.fossify.commons.extensions.updateTextColors
import org.fossify.commons.helpers.SORT_BY_DATE_CREATED
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.commons.models.AlarmSound
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class ClockAlarmFragment : Fragment(), ToggleAlarmInterface {
    private var alarms = ArrayList<Alarm>()
    private var currentEditAlarmDialog: EditClockAlarmDialog? = null

    private lateinit var binding: FragmentAlarmBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentAlarmBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EventBus.getDefault().register(this)
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        setupViews()
    }

    fun showSortingDialog() {
        ChangeClockAlarmSortDialog(activity as SimpleActivity) {
            setupAlarms()
        }
    }

    private fun setupViews() {
        binding.apply {
            requireContext().updateTextColors(alarmFragment)
            alarmFab.setOnClickListener {
                val newAlarm = root.context.createNewAlarm(DEFAULT_ALARM_MINUTES, 0)
                newAlarm.isEnabled = true
                newAlarm.days = getTomorrowBit()
                openEditAlarm(newAlarm)
            }
        }

        setupAlarms()
    }

    private fun setupAlarms() {
        alarms = context?.dbHelper?.getAlarms() ?: return

        when (requireContext().config.alarmSort) {
            SORT_BY_ALARM_TIME -> alarms.sortBy { it.timeInMinutes }
            SORT_BY_DATE_CREATED -> alarms.sortBy { it.id }
            SORT_BY_DATE_AND_TIME -> alarms.sortWith(compareBy<Alarm> {
                requireContext().firstDayOrder(it.days)
            }.thenBy {
                it.timeInMinutes
            })
        }
        context?.getEnabledAlarms { enabledAlarms ->
            if (enabledAlarms.isNullOrEmpty()) {
                val removedAlarms = mutableListOf<Alarm>()
                alarms.forEach {
                    if (it.days == TODAY_BIT && it.isEnabled && it.timeInMinutes <= getCurrentDayMinutes()) {
                        it.isEnabled = false
                        ensureBackgroundThread {
                            if (it.oneShot) {
                                it.isEnabled = false
                                context?.dbHelper?.deleteAlarms(arrayListOf(it))
                                removedAlarms.add(it)
                            } else {
                                context?.dbHelper?.updateAlarmEnabledState(it.id, false)
                            }
                        }
                    }
                }
                alarms.removeAll(removedAlarms)
            }
        }

        val currAdapter = binding.alarmsList.adapter
        if (currAdapter == null) {
            ClocksAdapter(activity as SimpleActivity, alarms, this, binding.alarmsList) {
                openEditAlarm(it as Alarm)
            }.apply {
                binding.alarmsList.adapter = this
            }
        } else {
            (currAdapter as ClocksAdapter).apply {
                updatePrimaryColor()
                updateBackgroundColor(Color.parseColor("#000000"))
                updateTextColor(Color.parseColor("#000000"))
                updateItems(this@ClockAlarmFragment.alarms)
            }
        }
    }

    private fun openEditAlarm(alarm: Alarm) {
        currentEditAlarmDialog = EditClockAlarmDialog(activity as SimpleActivity, alarm) {
            alarm.id = it
            currentEditAlarmDialog = null
            setupAlarms()
            checkAlarmState(alarm)
        }
    }

    override fun alarmToggled(id: Int, isEnabled: Boolean) {
        (activity as SimpleActivity).handleFullScreenNotificationsPermission { granted ->
            if (granted) {
                if (requireContext().dbHelper.updateAlarmEnabledState(id, isEnabled)) {
                    val alarm = alarms.firstOrNull { it.id == id } ?: return@handleFullScreenNotificationsPermission
                    alarm.isEnabled = isEnabled
                    checkAlarmState(alarm)
                    if (!alarm.isEnabled && alarm.oneShot) {
                        requireContext().dbHelper.deleteAlarms(arrayListOf(alarm))
                        setupAlarms()
                    }
                } else {
                    requireActivity().toast(org.fossify.commons.R.string.unknown_error_occurred)
                }
                requireContext().updateWidgets()
            } else {
                setupAlarms()
            }
        }
    }

    private fun checkAlarmState(alarm: Alarm) {
        if (alarm.isEnabled) {
            context?.scheduleNextAlarm(alarm, true)
        } else {
            context?.cancelAlarmClock(alarm)
        }
        (activity as? MainActivity)?.updateClockTabAlarm()
    }

    fun updateAlarmSound(alarmSound: AlarmSound) {
        currentEditAlarmDialog?.updateSelectedAlarmSound(alarmSound)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: EventAlarm.Refresh) {
        setupAlarms()
    }
}
