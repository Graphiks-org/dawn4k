package org.graphiks.dawn4k.toolkit.surface.testing

import org.graphiks.dawn4k.toolkit.*
import org.graphiks.dawn4k.toolkit.surface.*
import org.graphiks.dawn4k.toolkit.testing.*
import kotlin.test.assertTrue

internal class ToolkitSurfaceFixture(
    val gpu: DawnSession,
    val presentation: DawnPresentation,
    val resize: suspend (Int, Int) -> Unit,
)

internal expect fun toolkitSurfaceTest(block: suspend (ToolkitSurfaceFixture) -> Unit)

internal suspend fun assertPresentedTriangle(session: DawnSession, presentation: DawnPresentation) {
    session.run { gpu ->
        assertNoValidationErrors(gpu) {
            assertTrue(presentation.withFrame { frame ->
                renderTriangle(gpu, frame.view, checkNotNull(presentation.configuration.value).format)
                frame.present()
            })
        }
    }
}
