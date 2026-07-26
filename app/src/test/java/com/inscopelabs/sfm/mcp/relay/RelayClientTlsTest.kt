package com.inscopelabs.sfm.mcp.relay

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.inscopelabs.sfm.mcp.server.RelayConfig
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RelayClientTlsTest {

    @Test
    fun testEmptyCertificatePinningFailsConnection() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val config = RelayConfig(
            relayUrl = "relay.example.com:443",
            useTls = true,
            certificatePinning = true,
            pinnedCertificates = emptyList()
        )

        val client = RelayClient(context, config)
        val result = client.connect()

        assertTrue("Connection should fail when certificate pinning is enabled without pins", result is RelayClient.ConnectionResult.Error)
        val errorMsg = (result as RelayClient.ConnectionResult.Error).message
        assertTrue(errorMsg.contains("Certificate pinning enabled"))
    }

    @Test
    fun testSerializeMessageWithQuotesAndBackslashes() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val config = RelayConfig(
            relayUrl = "relay.example.com",
            useTls = false
        )

        val client = RelayClient(context, config)
        val specialSessionId = """sess"ion\with/special'chars"""
        val message = RelayMessage(
            type = "test_event",
            payload = "hello world",
            sessionId = specialSessionId,
            encrypted = false
        )

        val serialized = client.serializeMessage(message)

        // Verify valid JSON parsing and exact value preservation
        val parsed = JSONObject(serialized)
        assertEquals("test_event", parsed.getString("type"))
        assertEquals(specialSessionId, parsed.getString("sessionId"))
        assertFalse(parsed.getBoolean("encrypted"))
    }
}
