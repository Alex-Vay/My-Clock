package com.nr.myclock.clock.activities

import com.nr.myclock.R
import org.fossify.commons.activities.BaseSimpleActivity

open class SimpleActivity : BaseSimpleActivity() {
    override fun getAppIconIDs() = arrayListOf(
        R.mipmap.ic_launcher,
    )
    override fun getAppLauncherName() = getString(R.string.app_launcher_name)
}
