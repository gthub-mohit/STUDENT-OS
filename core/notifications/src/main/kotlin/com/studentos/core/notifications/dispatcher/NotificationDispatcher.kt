package com.studentos.core.notifications.dispatcher

interface NotificationDispatcher {
    /**
     * Dispatches a notification to the specified channel with optional deep-link route.
     * Returns true if the notification was posted, false if permission was denied or posting failed.
     */
    fun postNotification(
        channelId: String,
        notificationId: Int,
        title: String,
        message: String,
        route: String? = null
    ): Boolean

    /**
     * Cancels an existing notification by its ID.
     */
    fun cancelNotification(notificationId: Int)
}
