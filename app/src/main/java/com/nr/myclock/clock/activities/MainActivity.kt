package com.nr.myclock.clock.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Icon
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.view.WindowManager
import com.nr.myclock.BuildConfig
import com.nr.myclock.R
import com.nr.myclock.clock.activities.helpers.INVALID_TIMER_ID
import com.nr.myclock.clock.activities.helpers.OPEN_TAB
import com.nr.myclock.clock.activities.helpers.PICK_AUDIO_FILE_INTENT_ID
import com.nr.myclock.clock.activities.helpers.STOPWATCH_SHORTCUT_ID
import com.nr.myclock.clock.activities.helpers.STOPWATCH_TOGGLE_ACTION
import com.nr.myclock.clock.activities.helpers.TABS_COUNT
import com.nr.myclock.clock.activities.helpers.TAB_ALARM
import com.nr.myclock.clock.activities.helpers.TAB_STOPWATCH
import com.nr.myclock.clock.activities.helpers.TAB_TIMER
import com.nr.myclock.clock.activities.helpers.TIMER_ID
import com.nr.myclock.clock.activities.helpers.TOGGLE_STOPWATCH
import com.nr.myclock.databinding.ActivityMainBinding
import com.nr.myclock.clock.activities.extensions.config
import com.nr.myclock.clock.activities.extensions.getEnabledAlarms
import com.nr.myclock.clock.activities.extensions.rescheduleEnabledAlarms
import com.nr.myclock.clock.activities.extensions.updateWidgets
import org.fossify.commons.databinding.BottomTablayoutItemBinding
import org.fossify.commons.extensions.*
import org.fossify.commons.helpers.*

class MainActivity : SimpleActivity() {
    private var storedTextColor = 0
    private var storedBackgroundColor = 0
    private var storedPrimaryColor = 0
    private val binding: ActivityMainBinding by viewBinding(ActivityMainBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        isMaterialActivity = true
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        appLaunched(BuildConfig.APPLICATION_ID)
        setupOptionsMenu()
        refreshMenuItems()

        updateMaterialActivityViews(binding.mainCoordinator, binding.mainHolder, useTransparentNavigation = false, useTopSearchMenu = false)

        storeStateVariables()
        initFragments()
        setupTabs()
        updateWidgets()

        getEnabledAlarms { enabledAlarms ->
            if (enabledAlarms.isNullOrEmpty()) {
                ensureBackgroundThread {
                    rescheduleEnabledAlarms()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        setupToolbar(binding.mainToolbar, statusBarColor = getProperBackgroundColor())
        val configTextColor = getProperTextColor()
        if (storedTextColor != configTextColor) {
            getInactiveTabIndexes(binding.viewPager.currentItem).forEach {
                binding.mainTabsHolder.getTabAt(it)?.icon?.applyColorFilter(configTextColor)
            }
        }

        val configBackgroundColor = getProperBackgroundColor()
        if (storedBackgroundColor != configBackgroundColor) {
            binding.mainTabsHolder.background = ColorDrawable(configBackgroundColor)
        }

        val configPrimaryColor = getProperPrimaryColor()
        if (storedPrimaryColor != configPrimaryColor) {
            binding.mainTabsHolder.setSelectedTabIndicatorColor(getProperPrimaryColor())
            binding.mainTabsHolder.getTabAt(binding.viewPager.currentItem)?.icon?.applyColorFilter(getProperPrimaryColor())
        }

        if (config.preventPhoneFromSleeping) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        checkShortcuts()
    }

    @SuppressLint("NewApi")
    private fun checkShortcuts() {
        val appIconColor = config.appIconColor
        if (isNougatMR1Plus() && config.lastHandledShortcutColor != appIconColor) {
            val launchDialpad = getLaunchStopwatchShortcut(appIconColor)

            try {
                shortcutManager.dynamicShortcuts = listOf(launchDialpad)
                config.lastHandledShortcutColor = appIconColor
            } catch (ignored: Exception) {
            }
        }
    }

    @SuppressLint("NewApi")
    private fun getLaunchStopwatchShortcut(appIconColor: Int): ShortcutInfo {
        val newEvent = getString(R.string.start_stopwatch)
        val drawable = resources.getDrawable(R.drawable.shortcut_stopwatch)
        (drawable as LayerDrawable).findDrawableByLayerId(R.id.shortcut_stopwatch_background).applyColorFilter(appIconColor)
        val bmp = drawable.convertToBitmap()

        val intent = Intent(this, ClockplashActivity::class.java).apply {
            putExtra(OPEN_TAB, TAB_STOPWATCH)
            putExtra(TOGGLE_STOPWATCH, true)
            action = STOPWATCH_TOGGLE_ACTION
        }

        return ShortcutInfo.Builder(this, STOPWATCH_SHORTCUT_ID)
            .setShortLabel(newEvent)
            .setLongLabel(newEvent)
            .setIcon(Icon.createWithBitmap(bmp))
            .setIntent(intent)
            .build()
    }

    override fun onPause() {
        super.onPause()
       storeStateVariables()
        if (config.preventPhoneFromSleeping) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        config.lastUsedViewPagerPage = binding.viewPager.currentItem
    }

    private fun setupOptionsMenu() {
        binding.mainToolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.sort -> getViewPagerAdapter()?.showAlarmSortDialog()
                R.id.settings -> launchSettings()
                else -> return@setOnMenuItemClickListener false
            }
            return@setOnMenuItemClickListener true
        }
    }

    private fun refreshMenuItems() {
        binding.mainToolbar.menu.apply {
            findItem(R.id.sort).isVisible = binding.viewPager.currentItem == TAB_ALARM
        }
    }



    private fun storeStateVariables() {
        storedTextColor = getProperTextColor()
        storedBackgroundColor = getProperBackgroundColor()
        storedPrimaryColor = getProperPrimaryColor()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == PICK_AUDIO_FILE_INTENT_ID && resultCode == RESULT_OK && resultData != null) {
            storeNewAlarmSound(resultData)
        }
    }

    private fun storeNewAlarmSound(resultData: Intent) {
        val newAlarmSound = storeNewYourAlarmSound(resultData)

        when (binding.viewPager.currentItem) {
            TAB_ALARM -> getViewPagerAdapter()?.updateAlarmTabAlarmSound(newAlarmSound)
            TAB_TIMER -> getViewPagerAdapter()?.updateTimerTabAlarmSound(newAlarmSound)
        }
    }

    fun updateClockTabAlarm() {
        getViewPagerAdapter()?.updateClockTabAlarm()
    }

    private fun getViewPagerAdapter() = binding.viewPager.adapter as? ViewPagerAdapter

    private fun initFragments() {
        val viewPagerAdapter = ViewPagerAdapter(supportFragmentManager)
        binding.viewPager.adapter = viewPagerAdapter
        binding.viewPager.onPageChangeListener {
            binding.mainTabsHolder.getTabAt(it)?.select()
            refreshMenuItems()
        }

        val tabToOpen = intent.getIntExtra(OPEN_TAB, config.lastUsedViewPagerPage)
        intent.removeExtra(OPEN_TAB)
        if (tabToOpen == TAB_TIMER) {
            val timerId = intent.getIntExtra(TIMER_ID, INVALID_TIMER_ID)
            viewPagerAdapter.updateTimerPosition(timerId)
        }

        if (tabToOpen == TAB_STOPWATCH) {
            config.toggleStopwatch = intent.getBooleanExtra(TOGGLE_STOPWATCH, false)
        }

        binding.viewPager.offscreenPageLimit = TABS_COUNT - 1
        binding.viewPager.currentItem = tabToOpen
    }

    private fun setupTabs() {
        binding.mainTabsHolder.removeAllTabs()
        val tabDrawables =getTabDrawableIds()
        val tabLabels = arrayOf(R.string.clock, org.fossify.commons.R.string.alarm, R.string.stopwatch, R.string.timer)

        tabDrawables.forEachIndexed { i, drawableId ->
            binding.mainTabsHolder.newTab().setCustomView(org.fossify.commons.R.layout.bottom_tablayout_item).apply tab@{
                customView?.let { BottomTablayoutItemBinding.bind(it) }?.apply {
                    val tab = getDrawable(drawableId)
                    tab?.setColorFilter(Color.parseColor("#000000"), PorterDuff.Mode.SRC_IN)
                    tabItemIcon.setImageDrawable(tab)
                    tabItemLabel.setText(tabLabels[i])
                    tabItemLabel.setTextColor (Color.parseColor("#111111"))
                    binding.mainTabsHolder.addTab(this@tab)
                }
            }
        }

        binding.mainTabsHolder.onTabSelectionChanged(
            tabSelectedAction = {
                binding.viewPager.currentItem = it.position
            }
        )
    }

    private fun getInactiveTabIndexes(activeIndex: Int) = arrayListOf(0, 1, 2, 3).filter { it != activeIndex }

    private fun getTabDrawableIds() = arrayOf(
        org.fossify.commons.R.drawable.ic_clock_filled_vector,
        R.drawable.ic_alarm_filled_vector,
        R.drawable.ic_stopwatch_filled_vector,
        R.drawable.ic_hourglass_filled_vector
    )

    private fun launchSettings() {
        startActivity(Intent(applicationContext, ClockSettingsActivity::class.java))
    }
}
