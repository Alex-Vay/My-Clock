package com.nr.myclock.activities

import com.nr.myclock.activities.BaseSimpleActivity

open class SimpleActivity : BaseSimpleActivity() {
    override fun getAppIconIDs() = arrayListOf(
        R.mipmap.ic_launcher,
    )
    override fun getAppLauncherName() = getString(R.string.app_launcher_name)
}
