package com.example.fdmanager

import com.example.fdmanager.domain.LoadingState
import com.example.fdmanager.domain.UiState
import org.junit.Assert.*
import org.junit.Test

class LoadingStateTest {

    @Test
    fun loadingStateValues() {
        assertEquals(3, LoadingState.entries.size)
        assertTrue(LoadingState.entries.any { it.name == "IDLE" })
        assertTrue(LoadingState.entries.any { it.name == "LOADING" })
        assertTrue(LoadingState.entries.any { it.name == "ERROR" })
    }

    @Test
    fun fromNameReturnsCorrect() {
        assertEquals(LoadingState.IDLE, LoadingState.fromName("IDLE"))
        assertEquals(LoadingState.LOADING, LoadingState.fromName("LOADING"))
        assertEquals(LoadingState.ERROR, LoadingState.fromName("ERROR"))
        assertEquals(LoadingState.IDLE, LoadingState.fromName("UNKNOWN"))
    }

    @Test
    fun labelsAndDescriptionsPresent() {
        assertEquals("Idle", LoadingState.IDLE.label)
        assertEquals("Loading", LoadingState.LOADING.label)
        assertEquals("Error", LoadingState.ERROR.label)
        assertTrue(LoadingState.IDLE.description.isNotBlank())
        assertTrue(LoadingState.LOADING.description.isNotBlank())
        assertTrue(LoadingState.ERROR.description.isNotBlank())
    }

    @Test
    fun uiStateHelpers() {
        val idle = UiState<String>(LoadingState.IDLE, "data")
        assertTrue(idle.isIdle)
        assertFalse(idle.isLoading)
        assertFalse(idle.isError)

        val loading = UiState<String>(LoadingState.LOADING)
        assertTrue(loading.isLoading)

        val error = UiState<String>(LoadingState.ERROR, errorMessage = "Failed")
        assertTrue(error.isError)
        assertEquals("Failed", error.errorMessage)
    }
}
