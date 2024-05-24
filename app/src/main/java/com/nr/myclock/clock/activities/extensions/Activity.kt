package com.nr.myclock.clock.activities.extensions

import org.fossify.commons.activities.BaseSimpleActivity

fun BaseSimpleActivity.handleFullScreenNotificationsPermission(
    notificationsCallback: (granted: Boolean) -> Unit,
) {
    handleNotificationPermission { granted ->
                notificationsCallback(true)
    }
}
