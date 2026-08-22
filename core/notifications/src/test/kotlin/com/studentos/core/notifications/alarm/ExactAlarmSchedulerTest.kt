package com.studentos.core.notifications.alarm

import android.app.AlarmManager
import android.content.Context
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Test

class ExactAlarmSchedulerTest {

    private val context: Context = mockk(relaxed = true)
    private val alarmManager: AlarmManager = mockk(relaxed = true)
    private lateinit var scheduler: ExactAlarmSchedulerImpl

    @Before
    fun setup() {
        every { context.getSystemService(Context.ALARM_SERVICE) } returns alarmManager
        every { context.packageName } returns "com.studentos"
        scheduler = ExactAlarmSchedulerImpl(context)
    }

    @Test
    fun scheduleExactAlarm_futureTime_executesCleanly() {
        val futureTime = System.currentTimeMillis() + 900_000L
        scheduler.scheduleExactAlarm(
            requestCode = 101,
            triggerEpochMs = futureTime,
            channelId = "CLASS_REMINDER",
            notificationId = 1,
            title = "Class",
            message = "Starts in 15m",
            route = "weekly"
        )
    }

    @Test
    fun scheduleExactAlarm_pastTime_doesNotCrash() {
        val pastTime = System.currentTimeMillis() - 900_000L
        scheduler.scheduleExactAlarm(
            requestCode = 102,
            triggerEpochMs = pastTime,
            channelId = "CLASS_REMINDER",
            notificationId = 2,
            title = "Class",
            message = "Started",
            route = "weekly"
        )
    }

    @Test
    fun cancelExactAlarm_executesCleanly() {
        scheduler.cancelExactAlarm(101)
    }
}
