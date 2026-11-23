package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AcademicBlue
import com.example.ui.theme.AcademicBlueLight
import com.example.ui.theme.OutlineStrongLight
import com.example.ui.theme.StatusErrorFg
import com.example.ui.theme.SurfaceVariantLight
import kotlin.math.hypot

@Composable
fun PatternLockView(
    isError: Boolean,
    onPatternComplete: (List<Int>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val selectedNodes = remember { mutableStateListOf<Int>() }
    var activeDragPoint by remember { mutableStateOf<Offset?>(null) }

    val lineColor = if (isError) StatusErrorFg else AcademicBlue
    val ringColor = if (isError) StatusErrorFg.copy(alpha = 0.25f) else AcademicBlueLight
    val idleDotColor = OutlineStrongLight

    Box(
        modifier = modifier
            .sizeIn(maxWidth = 248.dp, maxHeight = 248.dp)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceVariantLight)
            .border(
                width = 1.dp,
                color = if (isError) StatusErrorFg.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(20.dp),
            )
            .testTag("pattern_lock_canvas")
            .semantics {
                contentDescription = "Bảng vẽ mẫu hình xác thực 3 nhân 3"
            },
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    fun nodeCenter(id: Int, width: Float, height: Float): Offset {
                        val index = id - 1
                        val row = index / 3
                        val col = index % 3
                        val stepX = width / 3f
                        val stepY = height / 3f
                        return Offset(
                            x = stepX * col + stepX / 2f,
                            y = stepY * row + stepY / 2f,
                        )
                    }

                    fun hitNode(pos: Offset, width: Float, height: Float): Int? {
                        val hitRadius = minOf(width, height) * 0.135f
                        for (id in 1..9) {
                            if (id in selectedNodes) continue
                            val center = nodeCenter(id, width, height)
                            if (hypot(pos.x - center.x, pos.y - center.y) <= hitRadius) {
                                return id
                            }
                        }
                        return null
                    }

                    detectDragGestures(
                        onDragStart = { startPos ->
                            selectedNodes.clear()
                            activeDragPoint = startPos
                            hitNode(startPos, size.width.toFloat(), size.height.toFloat())?.let { id ->
                                selectedNodes.add(id)
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val pos = change.position
                            activeDragPoint = pos
                            hitNode(pos, size.width.toFloat(), size.height.toFloat())?.let { id ->
                                selectedNodes.add(id)
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        },
                        onDragEnd = {
                            activeDragPoint = null
                            if (selectedNodes.isNotEmpty()) {
                                val result = selectedNodes.toList()
                                selectedNodes.clear()
                                onPatternComplete(result)
                            }
                        },
                        onDragCancel = {
                            activeDragPoint = null
                            selectedNodes.clear()
                        },
                    )
                },
        ) {
            val w = size.width
            val h = size.height
            fun centerFor(id: Int): Offset {
                val idx = id - 1
                val row = idx / 3
                val col = idx % 3
                return Offset(
                    x = (w / 3f) * col + w / 6f,
                    y = (h / 3f) * row + h / 6f,
                )
            }

            // Draw connecting segments between selected dots
            if (selectedNodes.size >= 2) {
                for (i in 0 until selectedNodes.size - 1) {
                    drawLine(
                        color = lineColor,
                        start = centerFor(selectedNodes[i]),
                        end = centerFor(selectedNodes[i + 1]),
                        strokeWidth = 6.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
            }

            // Draw active trailing drag line
            val dragPos = activeDragPoint
            if (dragPos != null && selectedNodes.isNotEmpty()) {
                drawLine(
                    color = lineColor.copy(alpha = 0.45f),
                    start = centerFor(selectedNodes.last()),
                    end = dragPos,
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }

            // Draw 9 grid nodes
            for (id in 1..9) {
                val center = centerFor(id)
                val isSelected = id in selectedNodes
                if (isSelected) {
                    drawCircle(
                        color = ringColor,
                        radius = 20.dp.toPx(),
                        center = center,
                    )
                    drawCircle(
                        color = lineColor,
                        radius = 18.dp.toPx(),
                        center = center,
                        style = Stroke(width = 2.dp.toPx(), join = StrokeJoin.Round),
                    )
                }
                drawCircle(
                    color = if (isSelected) lineColor else idleDotColor,
                    radius = if (isSelected) 9.dp.toPx() else 7.dp.toPx(),
                    center = center,
                )
            }
        }
    }
}
