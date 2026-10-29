package com.htdev.movie.htplay.domain.common

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class ResultTest {
    @Test
    fun tryCatchWrapsFailureAndMapKeepsIt() = runTest {
        val error = IllegalStateException("failure")
        val result = tryCatch<Int> { throw error }.map { it.toString() }
        assertTrue(result is Result.Error)
        assertSame(error, (result as Result.Error).throwable)
    }

    @Test
    fun tryCatchDoesNotSwallowCancellation() = runTest {
        val cancelled = CancellationException("cancelled")
        try {
            tryCatch<Int> { throw cancelled }
            fail("Cancellation must propagate")
        } catch (actual: CancellationException) {
            assertSame(cancelled, actual)
        }
    }
}
