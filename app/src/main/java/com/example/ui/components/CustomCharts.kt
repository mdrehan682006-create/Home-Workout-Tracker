package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// --- Animated Bar Chart ---
@Composable
fun AnimatedBarChart(
    data: List<Float>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    maxVal: Float = (data.maxOrNull() ?: 10f).coerceAtLeast(1f)
) {
    var animateTrigger by remember { mutableStateOf(false) }
    LaunchedEffect(data) {
        animateTrigger = true
    }
    
    val animationScale by animateFloatAsState(
        targetValue = if (animateTrigger) 1f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "BarChart"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        data.forEachIndexed { idx, value ->
            val fraction = (value / maxVal).coerceIn(0f, 1f) * animationScale
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight(0.85f)
                        .width(20.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight(fraction)
                            .fillMaxWidth()
                            .background(
                                brush = Brush.verticalGradient(
                                    listOf(barColor.copy(alpha = 0.6f), barColor)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = labels.getOrElse(idx) { "" },
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                )
            }
        }
    }
}

// --- Animated Line Chart ---
@Composable
fun AnimatedLineChart(
    data: List<Float>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.tertiary,
    maxVal: Float = (data.maxOrNull() ?: 10f).coerceAtLeast(1f),
    minVal: Float = (data.minOrNull() ?: 0f)
) {
    var animateTrigger by remember { mutableStateOf(false) }
    LaunchedEffect(data) {
        animateTrigger = true
    }
    
    val animationScale by animateFloatAsState(
        targetValue = if (animateTrigger) 1f else 0f,
        animationSpec = tween(durationMillis = 1000),
        label = "LineChart"
    )

    Column(modifier = modifier) {
        if (data.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No log data available yet",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                    )
                )
            }
        } else {
            val bgColor = MaterialTheme.colorScheme.background
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .padding(horizontal = 8.dp, vertical = 12.dp)
            ) {
                val width = size.width
                val height = size.height
                val spacing = width / (data.size - 1).coerceAtLeast(1)
                
                val range = (maxVal - minVal).coerceAtLeast(1f)
                
                val points = data.mapIndexed { idx, valItem ->
                    val x = idx * spacing
                    val normY = (valItem - minVal) / range
                    val y = height - (normY * height * animationScale)
                    Offset(x, y)
                }

                // Draw background grid lines
                val gridLines = 4
                for (i in 0..gridLines) {
                    val y = (height / gridLines) * i
                    drawLine(
                        color = lineColor.copy(alpha = 0.08f),
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Draw fill path under line
                val fillPath = Path().apply {
                    if (points.isNotEmpty()) {
                        moveTo(points.first().x, height)
                        points.forEach { point ->
                            lineTo(point.x, point.y)
                        }
                        lineTo(points.last().x, height)
                        close()
                    }
                }
                
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(lineColor.copy(alpha = 0.25f), Color.Transparent)
                    )
                )

                // Draw connecting lines
                val strokePath = Path().apply {
                    if (points.isNotEmpty()) {
                        moveTo(points.first().x, points.first().y)
                        for (i in 1 until points.size) {
                            lineTo(points[i].x, points[i].y)
                        }
                    }
                }

                drawPath(
                    path = strokePath,
                    color = lineColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                // Draw data indicator dots
                points.forEach { point ->
                    drawCircle(
                        color = bgColor,
                        radius = 5.dp.toPx(),
                        center = point
                    )
                    drawCircle(
                        color = lineColor,
                        radius = 3.5.dp.toPx(),
                        center = point
                    )
                }
            }
            
            // Render labels
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                labels.forEach { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                    )
                }
            }
        }
    }
}

// --- Animated Donut Chart ---
@Composable
fun AnimatedDonutChart(
    slices: List<Pair<String, Float>>,
    colors: List<Color>,
    modifier: Modifier = Modifier,
    strokeWidth: Float = 40f
) {
    val total = slices.map { it.second }.sum().coerceAtLeast(1f)
    var animateTrigger by remember { mutableStateOf(false) }
    
    LaunchedEffect(slices) {
        animateTrigger = true
    }

    val animatedSweep by animateFloatAsState(
        targetValue = if (animateTrigger) 360f else 0f,
        animationSpec = tween(durationMillis = 1000),
        label = "DonutChart"
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        Canvas(modifier = Modifier.size(130.dp)) {
            val sizeDim = size.width
            val radius = (sizeDim - strokeWidth) / 2f
            var startAngle = -90f

            slices.forEachIndexed { index, slice ->
                val sweepAngle = (slice.second / total) * animatedSweep
                drawArc(
                    color = colors.getOrElse(index) { Color.Gray },
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    size = Size(radius * 2, radius * 2),
                    topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
                )
                startAngle += (slice.second / total) * 360f
            }
        }

        // Legend list
        Column(
            modifier = Modifier.padding(start = 16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            slices.forEachIndexed { index, slice ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(
                                colors.getOrElse(index) { Color.Gray },
                                RoundedCornerShape(3.dp)
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${slice.first} (${(slice.second / total * 100).toInt()}%)",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
                }
            }
        }
    }
}
