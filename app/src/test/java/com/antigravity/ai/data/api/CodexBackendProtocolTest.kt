package com.antigravity.ai.data.api

import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Test

class CodexBackendProtocolTest {
    @Test
    fun threadStartPayloadsUseCodexOnRequestApprovalPolicy() {
        // Both sendPrompt's new-thread path and newChat use this serialized payload.
        val sendPromptPayload = JsonParser.parseString(codexThreadStartParams().toString()).asJsonObject
        val newChatPayload = JsonParser.parseString(codexThreadStartParams().toString()).asJsonObject

        assertEquals("on-request", sendPromptPayload.get("approvalPolicy").asString)
        assertEquals("on-request", newChatPayload.get("approvalPolicy").asString)
    }
}
