package com.nr.myclock.clock.activities.helpers

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import com.nr.myclock.R
import com.nr.myclock.clock.activities.ClockplashActivity
import com.nr.myclock.clock.activities.extensions.getClosestEnabledAlarmString
import org.fossify.commons.extensions.getLaunchIntent
import org.fossify.commons.extensions.setText
import org.fossify.commons.extensions.setVisibleIf

class DigitalTimeWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        performUpdate(context)
    }

    private fun performUpdate(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
        context.getClosestEnabledAlarmString { nextAlarm ->
            appWidgetManager.getAppWidgetIds(getComponentName(context)).forEach {
                RemoteViews(context.packageName, R.layout.widget_digital).apply {
                    updateTexts(context, this, nextAlarm)
                    setupAppOpenIntent(context, this)
                    appWidgetManager.updateAppWidget(it, this)
                }
            }
        }
    }

    private fun updateTexts(context: Context, views: RemoteViews, nextAlarm: String) {
        views.apply {
            setText(R.id.widget_next_alarm, nextAlarm)
            setVisibleIf(R.id.widget_alarm_holder, nextAlarm.isNotEmpty())
        }
    }



    private fun getComponentName(context: Context) = ComponentName(context, this::class.java)

    private fun setupAppOpenIntent(context: Context, views: RemoteViews) {
        (context.getLaunchIntent() ?: Intent(context, ClockplashActivity::class.java)).apply {
            putExtra(OPEN_TAB, TAB_CLOCK)
            val pendingIntent = PendingIntent.getActivity(context, OPEN_APP_INTENT_ID, this, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_date_time_holder, pendingIntent)
        }
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle?) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        performUpdate(context)
    }
}
