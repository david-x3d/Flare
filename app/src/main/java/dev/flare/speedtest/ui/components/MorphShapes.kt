package dev.flare.speedtest.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.toPath
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import kotlin.math.floor

/**
 * A [Shape] that lives on a chain of polygons: position 0.0 is the first polygon, 1.0 the second,
 * 2.0 the third and so on, with morphs in between. [position] is read while drawing, so animating
 * it never triggers recomposition. Springs may overshoot; the morph simply extrapolates, which
 * gives the expressive "wobble".
 */
class ChainedMorphShape(
    private val morphs: List<Morph>,
    private val position: State<Float>,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val p = position.value
        val segment = floor(p).toInt().coerceIn(0, morphs.lastIndex)
        val path = morphs[segment].toPath(progress = p - segment, path = Path())
        // MaterialShapes are normalised to the unit square.
        path.transform(Matrix().apply { scale(size.width, size.height) })
        return Outline.Generic(path)
    }
}

/** Morphs through [polygons] to the one at [index] with the theme's expressive spatial spring. */
@Composable
fun rememberChainedMorphShape(polygons: List<RoundedPolygon>, index: Int): Shape {
    val morphs = remember(polygons) { polygons.zipWithNext { a, b -> Morph(a, b) } }
    val position = animateFloatAsState(
        targetValue = index.toFloat(),
        animationSpec = MaterialTheme.motionScheme.slowSpatialSpec(),
        label = "morphPosition",
    )
    return remember(morphs) { ChainedMorphShape(morphs, position) }
}
