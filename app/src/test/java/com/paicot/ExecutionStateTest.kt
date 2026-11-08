package com.paicot

import com.paicot.core.ExecutionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExecutionStateTest {

    @Test fun enumNamesAreUppercase() {
        ExecutionState.values().forEach { state ->
            assertEquals(state.name, state.name.uppercase())
            assertTrue(state.name.all { it in 'A'..'Z' || it == '_' })
        }
        assertEquals(6, ExecutionState.values().size)
    }

    @Test fun valueOfRoundTrips() {
        ExecutionState.values().forEach { state ->
            assertEquals(state, ExecutionState.valueOf(state.name))
        }
        // Explicit round-trip for all 6 values.
        assertEquals(ExecutionState.IDLE, ExecutionState.valueOf("IDLE"))
        assertEquals(ExecutionState.RUNNING, ExecutionState.valueOf("RUNNING"))
        assertEquals(ExecutionState.COMPLETED, ExecutionState.valueOf("COMPLETED"))
        assertEquals(ExecutionState.FAILED, ExecutionState.valueOf("FAILED"))
        assertEquals(ExecutionState.CANCELLED, ExecutionState.valueOf("CANCELLED"))
        assertEquals(ExecutionState.TIMED_OUT, ExecutionState.valueOf("TIMED_OUT"))
    }
}
