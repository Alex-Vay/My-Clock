package com.nr.myclock.fragments

import android.os.Bundle
import android.os.Handler
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.nr.myclock.activities.SimpleActivity
import com.nr.myclock.models.MyTimeZone
import java.util.Calendar

class ClockFragment : Fragment() {
    private val ONE_SECOND = 1000L

    private var passedSeconds = 0
    private var calendar = Calendar.getInstance()
    private val updateHandler = Handler()

    private lateinit var binding: FragmentClockBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentClockBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        setupDateTime()

        binding.clockDate.setTextColor(requireContext().getProperTextColor())
    }

    override fun onPause() {
        super.onPause()
        updateHandler.removeCallbacksAndMessages(null)
    }

    private fun setupDateTime() {
        calendar = Calendar.getInstance()
        passedSeconds = getPassedSeconds()
        updateCurrentTime()
        updateDate()
        updateAlarm()
        setupViews()
    }

    private fun setupViews() {
        binding.apply {
            requireContext().updateTextColors(clockFragment)
            clockTime.setTextColor(requireContext().getProperTextColor())
            clockFab.setOnClickListener {
                fabClicked()
            }

            updateTimeZones()
        }
    }

    private fun updateCurrentTime() {
        val hours = (passedSeconds / 3600) % 24
        val minutes = (passedSeconds / 60) % 60
        val seconds = passedSeconds % 60

        if (!DateFormat.is24HourFormat(requireContext())) {
            binding.clockTime.textSize = resources.getDimension(R.dimen.clock_text_size_smaller) / resources.displayMetrics.density
        }

        if (seconds == 0) {
            if (hours == 0 && minutes == 0) {
                updateDate()
            }

            (binding.timeZonesList.adapter as? TimeZonesAdapter)?.updateTimes()
        }

        updateHandler.postDelayed({
            passedSeconds++
            updateCurrentTime()
        }, ONE_SECOND)
    }

    private fun updateDate() {
        calendar = Calendar.getInstance()
        val formattedDate = requireContext().getFormattedDate(calendar)
        (binding.timeZonesList.adapter as? TimeZonesAdapter)?.todayDateString = formattedDate
    }

    fun updateAlarm() {
        context?.getClosestEnabledAlarmString { nextAlarm ->
            binding.apply {
                clockAlarm.beVisibleIf(nextAlarm.isNotEmpty())
                clockAlarm.text = nextAlarm

            }
        }
    }

    private fun updateTimeZones() {
        val selectedTimeZones = context?.config?.selectedTimeZones ?: return
        binding.timeZonesList.beVisibleIf(selectedTimeZones.isNotEmpty())
        if (selectedTimeZones.isEmpty()) {
            return
        }

        val selectedTimeZoneIDs = selectedTimeZones.map { it.toInt() }
        val timeZones = requireContext().getAllTimeZonesModified().filter { selectedTimeZoneIDs.contains(it.id) } as ArrayList<MyTimeZone>
        val currAdapter = binding.timeZonesList.adapter
        if (currAdapter == null) {
            TimeZonesAdapter(activity as SimpleActivity, timeZones, binding.timeZonesList) {
                EditTimeZoneDialog(activity as SimpleActivity, it as MyTimeZone) {
                    updateTimeZones()
                }
            }.apply {
                this@ClockFragment.binding.timeZonesList.adapter = this
            }
        } else {
            (currAdapter as TimeZonesAdapter).apply {
                updatePrimaryColor()
                updateBackgroundColor(requireContext().getProperBackgroundColor())
                updateTextColor(requireContext().getProperTextColor())
                updateItems(timeZones)
            }
        }
    }

    private fun fabClicked() {
        AddTimeZonesDialog(activity as SimpleActivity) {
            updateTimeZones()
        }
    }
}
