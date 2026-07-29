package com.studentos.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Placeholder instrumented test for the :app module.
 *
 * Verifies that the application context is available and that the package
 * name matches the declared applicationId. This is the minimum smoke-test
 * for the scaffolding phase.
 *
 * Expanded in task 10.2 (Espresso / Compose UI end-to-end tests).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.studentos.app", appContext.packageName)
    }
}
