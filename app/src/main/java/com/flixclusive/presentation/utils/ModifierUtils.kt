package com.flixclusive.presentation.utils

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.flixclusive.presentation.theme.lightGray

object ModifierUtils {
    fun Modifier.fadingEdge(brush: Brush) = this
        .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
        .drawWithContent {
            drawContent()
            drawRect(brush = brush, blendMode = BlendMode.DstIn)
        }

    fun Modifier.placeholderEffect(
        shape: Shape = RoundedCornerShape(5.dp),
        color: Color = lightGray,
    ) = graphicsLayer {
        this.shape = shape
        clip = true
    }.drawBehind {
        drawRect(color)
    }

    /**
     * Used to apply modifiers conditionally.
     */
    fun Modifier.ifElse(
        condition: () -> Boolean,
        ifTrueModifier: Modifier,
        ifFalseModifier: Modifier = Modifier
    ): Modifier = then(if (condition()) ifTrueModifier else ifFalseModifier)

    /**
     * Used to apply modifiers conditionally.
     */
    fun Modifier.ifElse(
        condition: Boolean,
        ifTrueModifier: Modifier,
        ifFalseModifier: Modifier = Modifier
    ): Modifier = ifElse({ condition }, ifTrueModifier, ifFalseModifier)
}