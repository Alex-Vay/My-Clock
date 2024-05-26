package com.nr.myclock.clock.activities

import android.graphics.Color
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapterFactory
import com.nr.myclock.clock.activities.models.StateTimer
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import com.nr.myclock.databinding.ItemAlarmBinding
import com.nr.myclock.clock.activities.helpers.TODAY_BIT
import com.nr.myclock.clock.activities.helpers.TOMORROW_BIT
import com.nr.myclock.clock.activities.helpers.getCurrentDayMinutes
import com.nr.myclock.clock.activities.models.Alarm
import android.widget.TextView
import com.nr.myclock.databinding.ItemLapBinding
import com.nr.myclock.clock.activities.extensions.formatStopwatchTime
import com.nr.myclock.clock.activities.helpers.SORT_BY_LAP
import com.nr.myclock.clock.activities.helpers.SORT_BY_LAP_TIME
import com.nr.myclock.clock.activities.helpers.SORT_BY_TOTAL_TIME
import com.nr.myclock.clock.activities.models.Lap
import org.fossify.commons.adapters.MyRecyclerViewAdapter
import org.fossify.commons.views.MyRecyclerView
import org.fossify.commons.dialogs.ConfirmationDialog
import org.fossify.commons.extensions.beVisibleIf
import org.fossify.commons.extensions.toast
import androidx.recyclerview.widget.DiffUtil
import me.grantland.widget.AutofitHelper
import com.nr.myclock.databinding.ItemTimerBinding
import com.nr.myclock.clock.activities.extensions.getFormattedDuration
import com.nr.myclock.clock.activities.extensions.hideTimerNotification
import com.nr.myclock.clock.activities.extensions.secondsToMillis
import com.nr.myclock.clock.activities.models.Timer
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.nr.myclock.clock.activities.fragments.ClockAlarmFragment
import com.nr.myclock.clock.activities.fragments.ClockTimeFragment
import com.nr.myclock.clock.activities.fragments.ClockStopwatchFragment
import com.nr.myclock.clock.activities.fragments.ClockTimerFragment
import org.fossify.commons.models.AlarmSound
import com.nr.myclock.databinding.ItemTimeZoneBinding
import com.nr.myclock.clock.activities.extensions.config
import com.nr.myclock.clock.activities.extensions.getFormattedDate
import com.nr.myclock.clock.activities.extensions.getFormattedTime
import com.nr.myclock.clock.activities.models.CurrentTimeZone
import org.fossify.commons.extensions.beGone
import org.fossify.commons.extensions.beVisible
import java.util.Calendar
import java.util.Date
import java.util.TimeZone
import com.nr.myclock.clock.activities.models.EventTimer
import org.fossify.commons.adapters.MyRecyclerViewListAdapter
import org.fossify.commons.dialogs.PermissionRequiredDialog
import org.fossify.commons.extensions.*
import org.greenrobot.eventbus.EventBus
import androidx.recyclerview.widget.RecyclerView
import com.nr.myclock.R
import com.nr.myclock.clock.activities.extensions.dbHelper
import com.nr.myclock.clock.activities.extensions.getAlarmSelectedDaysString
import com.nr.myclock.clock.activities.extensions.scheduleNextAlarm
import com.nr.myclock.clock.activities.helpers.TABS_COUNT
import com.nr.myclock.clock.activities.helpers.TAB_ALARM
import com.nr.myclock.clock.activities.helpers.TAB_CLOCK
import com.nr.myclock.clock.activities.helpers.TAB_STOPWATCH
import com.nr.myclock.clock.activities.helpers.TAB_TIMER
import com.nr.myclock.databinding.ItemAddTimeZoneBinding
import com.nr.myclock.clock.activities.extensions.gson.RuntimeTypeAdapterFactory
import org.fossify.commons.extensions.getProperBackgroundColor
import org.fossify.commons.extensions.getProperPrimaryColor
import org.fossify.commons.extensions.getProperTextColor


class ClocksAdapter(
    activity: SimpleActivity, var alarms: ArrayList<Alarm>, val toggleAlarmInterface: ToggleAlarmInterface,
    recyclerView: MyRecyclerView, itemClick: (Any) -> Unit,
) : MyRecyclerViewAdapter(activity, recyclerView, itemClick) {

    init {
        setupDragListener(true)
    }

    override fun getActionMenuId() = R.menu.cab_alarms

    override fun prepareActionMode(menu: Menu) {}

    override fun actionItemPressed(id: Int) {
        if (selectedKeys.isEmpty()) {
            return
        }

        when (id) {
            R.id.cab_delete -> deleteItems()
        }
    }

    override fun getSelectableItemCount() = alarms.size

    override fun getIsItemSelectable(position: Int) = true

    override fun getItemSelectionKey(position: Int) = alarms.getOrNull(position)?.id

    override fun getItemKeyPosition(key: Int) = alarms.indexOfFirst { it.id == key }

    override fun onActionModeCreated() {}

    override fun onActionModeDestroyed() {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return createViewHolder(ItemAlarmBinding.inflate(layoutInflater, parent, false).root)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val alarm = alarms[position]
        holder.bindView(alarm, true, true) { itemView, layoutPosition ->
            setupView(itemView, alarm)
        }
        bindViewHolder(holder)
    }

    override fun getItemCount() = alarms.size

    fun updateItems(newItems: ArrayList<Alarm>) {
        alarms = newItems
        notifyDataSetChanged()
        finishActMode()
    }

    private fun deleteItems() {
        val alarmsToRemove = ArrayList<Alarm>()
        val positions = getSelectedItemPositions()
        getSelectedItems().forEach {
            alarmsToRemove.add(it)
        }

        alarms.removeAll(alarmsToRemove)
        removeSelectedItems(positions)
        activity.dbHelper.deleteAlarms(alarmsToRemove)
    }

    private fun getSelectedItems() = alarms.filter { selectedKeys.contains(it.id) } as ArrayList<Alarm>

    private fun setupView(view: View, alarm: Alarm) {
        val isSelected = selectedKeys.contains(alarm.id)
        ItemAlarmBinding.bind(view).apply {
            alarmHolder.isSelected = isSelected
            alarmTime.text = activity.getFormattedTime(alarm.timeInMinutes * 60, false, true)
            alarmTime.setTextColor(Color.parseColor("#000000"))

            alarmDays.text = activity.getAlarmSelectedDaysString(alarm.days)
            alarmDays.setTextColor(Color.parseColor("#000000"))

            alarmLabel.text = alarm.label
            alarmLabel.setTextColor(Color.parseColor("#000000"))
            alarmLabel.beVisibleIf(alarm.label.isNotEmpty())

            alarmSwitch.isChecked = alarm.isEnabled
            alarmSwitch.setColors(Color.parseColor("#000000"), properPrimaryColor, backgroundColor)
            alarmSwitch.setOnClickListener {
                if (alarm.days > 0) {
                    if (activity.config.wasAlarmWarningShown) {
                        toggleAlarmInterface.alarmToggled(alarm.id, alarmSwitch.isChecked)
                    } else {
                        ConfirmationDialog(
                            activity,
                            messageId = org.fossify.commons.R.string.alarm_warning,
                            positive = org.fossify.commons.R.string.ok,
                            negative = 0
                        ) {
                            activity.config.wasAlarmWarningShown = true
                            toggleAlarmInterface.alarmToggled(alarm.id, alarmSwitch.isChecked)
                        }
                    }
                } else if (alarm.days == TODAY_BIT) {
                    if (alarm.timeInMinutes <= getCurrentDayMinutes()) {
                        alarm.days = TOMORROW_BIT
                        alarmDays.text = resources.getString(org.fossify.commons.R.string.tomorrow)
                    }
                    activity.dbHelper.updateAlarm(alarm)
                    root.context.scheduleNextAlarm(alarm, true)
                    toggleAlarmInterface.alarmToggled(alarm.id, alarmSwitch.isChecked)
                } else if (alarm.days == TOMORROW_BIT) {
                    toggleAlarmInterface.alarmToggled(alarm.id, alarmSwitch.isChecked)
                } else if (alarmSwitch.isChecked) {
                    activity.toast(R.string.no_days_selected)
                    alarmSwitch.isChecked = false
                } else {
                    toggleAlarmInterface.alarmToggled(alarm.id, alarmSwitch.isChecked)
                }
            }
        }
    }
}

val statesTimerStates = valueOf<StateTimer>()
    .registerSubtype(StateTimer.Idle::class.java)
    .registerSubtype(StateTimer.Running::class.java)
    .registerSubtype(StateTimer.Paused::class.java)
    .registerSubtype(StateTimer.Finished::class.java)

inline fun <reified T : Any> valueOf(): RuntimeTypeAdapterFactory<T> = RuntimeTypeAdapterFactory.of(T::class.java)

fun GsonBuilder.registerTypes(vararg types: TypeAdapterFactory) = apply {
    types.forEach { registerTypeAdapterFactory(it) }
}

val gson: Gson = GsonBuilder().registerTypes(statesTimerStates).create()



class StopwatchAdapter(activity: SimpleActivity, var laps: ArrayList<Lap>, recyclerView: MyRecyclerView, itemClick: (Any) -> Unit) :
    MyRecyclerViewAdapter(activity, recyclerView, itemClick) {
    private var lastLapTimeView: TextView? = null
    private var lastTotalTimeView: TextView? = null
    private var lastLapId = 0

    override fun getActionMenuId() = 0

    override fun prepareActionMode(menu: Menu) {}

    override fun actionItemPressed(id: Int) {}

    override fun getSelectableItemCount() = laps.size

    override fun getIsItemSelectable(position: Int) = false

    override fun getItemSelectionKey(position: Int) = laps.getOrNull(position)?.id

    override fun getItemKeyPosition(key: Int) = laps.indexOfFirst { it.id == key }

    override fun onActionModeCreated() {}

    override fun onActionModeDestroyed() {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return createViewHolder(ItemLapBinding.inflate(layoutInflater, parent, false).root)
    }

    override fun onBindViewHolder(holder: MyRecyclerViewAdapter.ViewHolder, position: Int) {
        val lap = laps[position]
        holder.bindView(lap, false, false) { itemView, layoutPosition ->
            setupView(itemView, lap)
        }
        bindViewHolder(holder)
    }

    override fun getItemCount() = laps.size

    fun updateItems(newItems: ArrayList<Lap>) {
        lastLapId = 0
        laps = newItems.clone() as ArrayList<Lap>
        laps.sort()
        notifyDataSetChanged()
        finishActMode()
    }

    fun updateLastField(lapTime: Long, totalTime: Long) {
        lastLapTimeView?.text = lapTime.formatStopwatchTime(false)
        lastTotalTimeView?.text = totalTime.formatStopwatchTime(false)
    }

    private fun setupView(view: View, lap: Lap) {
        ItemLapBinding.bind(view).apply {
            lapOrder.text = lap.id.toString()
            lapOrder.setTextColor(textColor)
            lapOrder.setOnClickListener {
                itemClick(SORT_BY_LAP)
            }

            lapLapTime.text = lap.lapTime.formatStopwatchTime(false)
            lapLapTime.setTextColor(textColor)
            lapLapTime.setOnClickListener {
                itemClick(SORT_BY_LAP_TIME)
            }

            lapTotalTime.text = lap.totalTime.formatStopwatchTime(false)
            lapTotalTime.setTextColor(textColor)
            lapTotalTime.setOnClickListener {
                itemClick(SORT_BY_TOTAL_TIME)
            }

            if (lap.id > lastLapId) {
                lastLapTimeView = lapLapTime
                lastTotalTimeView = lapTotalTime
                lastLapId = lap.id
            }
        }
    }
}

class TimerAdapter(
    private val simpleActivity: SimpleActivity,
    recyclerView: MyRecyclerView,
    onRefresh: () -> Unit,
    onItemClick: (Timer) -> Unit,
) : MyRecyclerViewListAdapter<Timer>(simpleActivity, recyclerView, diffUtil, onItemClick, onRefresh) {

    companion object {
        private val diffUtil = object : DiffUtil.ItemCallback<Timer>() {
            override fun areItemsTheSame(oldItem: Timer, newItem: Timer): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Timer, newItem: Timer): Boolean {
                return oldItem == newItem
            }
        }
    }

    init {
        setupDragListener(true)
    }

    override fun getActionMenuId() = R.menu.cab_alarms

    override fun prepareActionMode(menu: Menu) {}

    override fun actionItemPressed(id: Int) {
        if (selectedKeys.isEmpty()) {
            return
        }

        when (id) {
            R.id.cab_delete -> deleteItems()
        }
    }

    override fun getSelectableItemCount() = itemCount

    override fun getIsItemSelectable(position: Int) = true

    override fun getItemSelectionKey(position: Int) = getItem(position).id

    override fun getItemKeyPosition(key: Int): Int {
        var position = -1
        for (i in 0 until itemCount) {
            if (key == getItem(i).id) {
                position = i
                break
            }
        }
        return position
    }

    override fun onActionModeCreated() {}

    override fun onActionModeDestroyed() {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return createViewHolder(ItemTimerBinding.inflate(layoutInflater, parent, false).root)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindView(getItem(position), true, true) { itemView, _ ->
            setupView(itemView, getItem(position))
        }
        bindViewHolder(holder)
    }

    private fun deleteItems() {
        val positions = getSelectedItemPositions()
        val timersToRemove = positions.map { position ->
            getItem(position)
        }
        removeSelectedItems(positions)
        timersToRemove.forEach(::deleteTimer)
    }

    private fun setupView(view: View, timer: Timer) {
        ItemTimerBinding.bind(view).apply {
            val isSelected = selectedKeys.contains(timer.id)
            timerFrame.isSelected = isSelected

            timerLabel.setTextColor(textColor)
            timerLabel.setHintTextColor(textColor.adjustAlpha(0.7f))
            timerLabel.text = timer.label

            AutofitHelper.create(timerTime)
            timerTime.setTextColor(textColor)
            timerTime.text = when (timer.state) {
                is StateTimer.Finished -> 0.getFormattedDuration()
                is StateTimer.Idle -> timer.seconds.getFormattedDuration()
                is StateTimer.Paused -> timer.state.tick.getFormattedDuration()
                is StateTimer.Running -> timer.state.tick.getFormattedDuration()
            }

            timerReset.applyColorFilter(textColor)
            timerReset.setOnClickListener {
                resetTimer(timer)
            }

            timerPlayPause.applyColorFilter(textColor)
            timerPlayPause.setOnClickListener {
                (activity as SimpleActivity).handleNotificationPermission { granted ->
                    if (granted) {
                        when (val state = timer.state) {
                            is StateTimer.Idle -> EventBus.getDefault().post(EventTimer.Start(timer.id!!, timer.seconds.secondsToMillis))
                            is StateTimer.Paused -> EventBus.getDefault().post(EventTimer.Start(timer.id!!, state.tick))
                            is StateTimer.Running -> EventBus.getDefault().post(EventTimer.Pause(timer.id!!, state.tick))
                            is StateTimer.Finished -> EventBus.getDefault().post(EventTimer.Start(timer.id!!, timer.seconds.secondsToMillis))
                        }
                    } else {
                        PermissionRequiredDialog(
                            activity,
                            org.fossify.commons.R.string.allow_notifications_reminders,
                            { activity.openNotificationSettings() })
                    }
                }
            }

            val state = timer.state
            val resetPossible = state is StateTimer.Running || state is StateTimer.Paused || state is StateTimer.Finished
            timerReset.beInvisibleIf(!resetPossible)
            val drawableId = if (state is StateTimer.Running) {
                org.fossify.commons.R.drawable.ic_pause_vector
            } else {
                org.fossify.commons.R.drawable.ic_play_vector
            }
            timerPlayPause.setImageDrawable(simpleActivity.resources.getColoredDrawableWithColor(drawableId, textColor))
        }
    }

    private fun resetTimer(timer: Timer) {
        EventBus.getDefault().post(EventTimer.Reset(timer.id!!))
        simpleActivity.hideTimerNotification(timer.id!!)
    }

    private fun deleteTimer(timer: Timer) {
        EventBus.getDefault().post(EventTimer.Delete(timer.id!!))
        simpleActivity.hideTimerNotification(timer.id!!)
    }
}

class TimeZonesAdapter(activity: SimpleActivity, var timeZones: ArrayList<CurrentTimeZone>, recyclerView: MyRecyclerView, itemClick: (Any) -> Unit) :
    MyRecyclerViewAdapter(activity, recyclerView, itemClick) {

    var todayDateString = activity.getFormattedDate(Calendar.getInstance())

    init {
        setupDragListener(true)
    }

    override fun getActionMenuId() = R.menu.cab_timezones

    override fun prepareActionMode(menu: Menu) {}

    override fun actionItemPressed(id: Int) {
        if (selectedKeys.isEmpty()) {
            return
        }

        when (id) {
            R.id.cab_delete -> deleteItems()
        }
    }

    override fun getSelectableItemCount() = timeZones.size

    override fun getIsItemSelectable(position: Int) = true

    override fun getItemSelectionKey(position: Int) = timeZones.getOrNull(position)?.id

    override fun getItemKeyPosition(key: Int) = timeZones.indexOfFirst { it.id == key }

    override fun onActionModeCreated() {}

    override fun onActionModeDestroyed() {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return createViewHolder(ItemTimeZoneBinding.inflate(layoutInflater, parent, false).root)
    }

    override fun onBindViewHolder(holder: MyRecyclerViewAdapter.ViewHolder, position: Int) {
        val timeZone = timeZones[position]
        holder.bindView(timeZone, true, true) { itemView, layoutPosition ->
            setupView(itemView, timeZone)
        }
        bindViewHolder(holder)
    }

    override fun getItemCount() = timeZones.size

    fun updateItems(newItems: ArrayList<CurrentTimeZone>) {
        timeZones = newItems
        notifyDataSetChanged()
        finishActMode()
    }

    fun updateTimes() {
        notifyDataSetChanged()
    }

    private fun deleteItems() {
        val timeZonesToRemove = ArrayList<CurrentTimeZone>(selectedKeys.size)
        val timeZoneIDsToRemove = ArrayList<String>(selectedKeys.size)
        val positions = getSelectedItemPositions()
        getSelectedItems().forEach {
            timeZonesToRemove.add(it)
            timeZoneIDsToRemove.add(it.id.toString())
        }

        timeZones.removeAll(timeZonesToRemove)
        removeSelectedItems(positions)

        val selectedTimeZones = activity.config.selectedTimeZones
        val newTimeZones = selectedTimeZones.filter { !timeZoneIDsToRemove.contains(it) }.toHashSet()
        activity.config.selectedTimeZones = newTimeZones
    }

    private fun getSelectedItems() = timeZones.filter { selectedKeys.contains(it.id) } as ArrayList<CurrentTimeZone>

    private fun setupView(view: View, timeZone: CurrentTimeZone) {
        val currTimeZone = TimeZone.getTimeZone(timeZone.zoneName)
        val calendar = Calendar.getInstance(currTimeZone)
        var offset = calendar.timeZone.rawOffset
        val isDaylightSavingActive = currTimeZone.inDaylightTime(Date())
        if (isDaylightSavingActive) {
            offset += currTimeZone.dstSavings
        }
        val passedSeconds = ((calendar.timeInMillis + offset) / 1000).toInt()
        val formattedTime = activity.getFormattedTime(passedSeconds, false, false)
        val formattedDate = activity.getFormattedDate(calendar)

        val isSelected = selectedKeys.contains(timeZone.id)
        ItemTimeZoneBinding.bind(view).apply {
            timeZoneFrame.isSelected = isSelected
            timeZoneTitle.text = timeZone.title
            timeZoneTitle.setTextColor(Color.parseColor("#000000"))

            timeZoneTime.text = formattedTime
            timeZoneTime.setTextColor(Color.parseColor("#000000"))

            if (formattedDate != todayDateString) {
                timeZoneDate.beVisible()
                timeZoneDate.text = formattedDate
                timeZoneDate.setTextColor(textColor)
            } else {
                timeZoneDate.beGone()
            }
        }
    }
}

class ViewPagerAdapter(fm: FragmentManager) : FragmentStatePagerAdapter(fm) {
    private val fragments = HashMap<Int, Fragment>()

    override fun getItem(position: Int): Fragment {
        return getFragment(position)
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val fragment = super.instantiateItem(container, position)
        if (fragment is Fragment) {
            fragments[position] = fragment
        }
        return fragment
    }

    override fun destroyItem(container: ViewGroup, position: Int, item: Any) {
        fragments.remove(position)
        super.destroyItem(container, position, item)
    }

    override fun getCount() = TABS_COUNT

    private fun getFragment(position: Int) = when (position) {
        0 -> ClockTimeFragment()
        1 -> ClockAlarmFragment()
        2 -> ClockStopwatchFragment()
        3 -> ClockTimerFragment()
        else -> throw RuntimeException("Trying to fetch unknown fragment id $position")
    }

    fun showAlarmSortDialog() {
        (fragments[TAB_ALARM] as? ClockAlarmFragment)?.showSortingDialog()
    }

    fun updateClockTabAlarm() {
        (fragments[TAB_CLOCK] as? ClockTimeFragment)?.updateAlarm()
    }

    fun updateAlarmTabAlarmSound(alarmSound: AlarmSound) {
        (fragments[TAB_ALARM] as? ClockAlarmFragment)?.updateAlarmSound(alarmSound)
    }

    fun updateTimerTabAlarmSound(alarmSound: AlarmSound) {
        (fragments[TAB_TIMER] as? ClockTimerFragment)?.updateAlarmSound(alarmSound)
    }

    fun updateTimerPosition(timerId: Int) {
        (fragments[TAB_TIMER] as? ClockTimerFragment)?.updatePosition(timerId)
    }

    fun startStopWatch() {
        (fragments[TAB_STOPWATCH] as? ClockStopwatchFragment)?.startStopWatch()
    }
}

class SelectTimeZonesAdapter(val activity: SimpleActivity, val timeZones: ArrayList<CurrentTimeZone>) : RecyclerView.Adapter<SelectTimeZonesAdapter.ViewHolder>() {
    private val config = activity.config
    private val textColor = Color.parseColor("#000000")
    private val backgroundColor = Color.parseColor("#FFFFFF")
    private val primaryColor = Color.parseColor("#2196F3")
    var selectedKeys = HashSet<Int>()

    init {
        val selectedTimeZones = config.selectedTimeZones
        timeZones.forEachIndexed { index, myTimeZone ->
            if (selectedTimeZones.contains(myTimeZone.id.toString())) {
                selectedKeys.add(myTimeZone.id)
            }
        }
    }

    private fun toggleItemSelection(select: Boolean, pos: Int) {
        val itemKey = timeZones.getOrNull(pos)?.id ?: return

        if (select) {
            selectedKeys.add(itemKey)
        } else {
            selectedKeys.remove(itemKey)
        }

        notifyItemChanged(pos)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemAddTimeZoneBinding.inflate(activity.layoutInflater, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindView(timeZones[position], textColor, primaryColor, backgroundColor)
    }

    override fun getItemCount() = timeZones.size

    inner class ViewHolder(private val binding: ItemAddTimeZoneBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bindView(myTimeZone: CurrentTimeZone, textColor: Int, primaryColor: Int, backgroundColor: Int): View {
            val isSelected = selectedKeys.contains(myTimeZone.id)
            binding.apply {
                addTimeZoneCheckbox.isChecked = isSelected
                addTimeZoneTitle.text = myTimeZone.title
                addTimeZoneTitle.setTextColor(textColor)
                addTimeZoneCheckbox.setColors(textColor, primaryColor, backgroundColor)
                addTimeZoneHolder.setOnClickListener {
                    viewClicked(myTimeZone)
                }
            }

            return itemView
        }

        private fun viewClicked(myTimeZone: CurrentTimeZone) {
            val isSelected = selectedKeys.contains(myTimeZone.id)
            toggleItemSelection(!isSelected, adapterPosition)
        }
    }
}
