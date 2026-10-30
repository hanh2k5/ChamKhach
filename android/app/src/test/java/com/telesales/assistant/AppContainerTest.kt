package com.telesales.assistant

import com.telesales.assistant.di.AppContainer
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppContainerTest {
    @Test
    fun appContainer_interface_isDefined() {
        val container = object : AppContainer {}
        assertNotNull(container)
        assertTrue(container is AppContainer)
    }
}

