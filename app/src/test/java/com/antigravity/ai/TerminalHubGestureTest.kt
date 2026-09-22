package com.antigravity.ai

import com.antigravity.ai.ui.screens.shouldOpenHub
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TerminalHubGestureTest {
    @Test fun opensOnlyAtThreshold() {
        assertFalse(shouldOpenHub(0.349f))
        assertTrue(shouldOpenHub(0.35f))
    }
}
