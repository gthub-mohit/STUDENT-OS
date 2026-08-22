package com.studentos.core.notifications.alarm

interface ExactAlarmScheduler {
    /**
     * Schedules an exact alarm with AlarmManager to fire at [triggerEpochMs],
     * waking the device from Doze mode if needed.
     */
    fun scheduleExactAlarm(
        requestCode: Int,
        triggerEpochMs: Long,
        channelId: String,
        notificationId: Int,
        title: String,
        message: String,
        route: String? = null
    )

    /**
     * Cancels a previously scheduled exact alarm by its [requestCode].
     */
    fun cancelExactAlarm(requestCode: Int)
}
