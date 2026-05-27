package me.ashishekka.echo.shared.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PreferenceStorageTest {

    private lateinit var testScope: TestScope
    private lateinit var preferenceStorage: PreferenceStorage

    @BeforeTest
    fun setup() {
        testScope = TestScope(StandardTestDispatcher() + Job())
        
        // Use a unique file name for each test run to avoid state leakage
        val dataStore = PreferenceDataStoreFactory.createWithPath(
            scope = testScope.backgroundScope,
            produceFile = { getTestDataStorePath("test_pref.preferences_pb").toPath() }
        )
        preferenceStorage = DataStorePreferenceStorage(dataStore)
    }

    @Test
    fun testInitialRestoreStatusIsFalse() = testScope.runTest {
        assertFalse(preferenceStorage.isRestoreCompleted.first())
    }

    @Test
    fun testSetRestoreCompleted() = testScope.runTest {
        preferenceStorage.setRestoreCompleted(true)
        assertTrue(preferenceStorage.isRestoreCompleted.first())
        
        preferenceStorage.setRestoreCompleted(false)
        assertFalse(preferenceStorage.isRestoreCompleted.first())
    }
}
