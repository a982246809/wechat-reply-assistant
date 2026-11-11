package io.github.a982246809.replyassistant.data.provider

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidKeystoreAiSecretStoreTest {
    private lateinit var context: Context
    private lateinit var store: AndroidKeystoreAiSecretStore

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        store = AndroidKeystoreAiSecretStore(context)
        runBlocking { store.clearApiKey() }
    }

    @After
    fun tearDown() {
        runBlocking { store.clearApiKey() }
    }

    @Test
    fun roundTripReplaceAndRestartRead() = runBlocking {
        store.writeApiKey("first-key")
        assertEquals("first-key", store.readApiKey())

        store.writeApiKey("second-key")
        assertEquals("second-key", store.readApiKey())

        val restartedStore = AndroidKeystoreAiSecretStore(context)
        assertEquals("second-key", restartedStore.readApiKey())

        restartedStore.clearApiKey()
        assertNull(store.readApiKey())
    }
}
