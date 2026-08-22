package com.studentos.core.notifications.scheduler

interface NotificationRescheduler {
    suspend fun rescheduleAll()
    suspend fun rescheduleClassReminders()
    suspend fun rescheduleAssignmentReminders()
    suspend fun rescheduleContestReminders()
    suspend fun schedulePeriodicProjectInactivityCheck()
    suspend fun schedulePeriodicDailyBrief()
}
