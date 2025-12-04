package com.monitor.ui

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monitor.viewmodel.MonitorState
import com.monitor.viewmodel.MonitorViewModel

val DarkBackground = Color(0xFF1E1E1E)
val CyanChart = Color(0xFF00FFFF)
val MagentaChart = Color(0xFFFF00FF)
val CardBackground = Color(0xFF2D2D2D)

@Composable
fun MainScreen(viewModel: MonitorViewModel) {
    val state by viewModel.state.collectAsState()

    MaterialTheme(
        colors = MaterialTheme.colors.copy(
            background = DarkBackground,
            surface = CardBackground,
            onBackground = Color.White,
            onSurface = Color.White
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colors.background
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text(
                    "System Monitor",
                    style = MaterialTheme.typography.h4,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // CPU Chart Section
                    Column(
                        modifier = Modifier.weight(2f)
                            .background(CardBackground, shape = MaterialTheme.shapes.medium)
                            .padding(16.dp)
                            .fillMaxHeight()
                    ) {
                        Text("CPU History (60s)", style = MaterialTheme.typography.h6)
                        Spacer(modifier = Modifier.height(16.dp))
                        CpuLineChart(state.cpuHistory, modifier = Modifier.fillMaxSize())
                    }

                    // Memory Chart Section
                    Column(
                        modifier = Modifier.weight(1f)
                            .background(CardBackground, shape = MaterialTheme.shapes.medium)
                            .padding(16.dp)
                            .fillMaxHeight()
                    ) {
                        Text("Memory Usage", style = MaterialTheme.typography.h6)
                        Spacer(modifier = Modifier.height(16.dp))
                        MemoryPieChart(state.memoryUsed, state.memoryTotal, modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }
    }
}

@Composable
fun CpuLineChart(data: List<Double>, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val maxVal = 100.0
        val points = data.mapIndexed { index, value ->
            Offset(
                x = index * (width / (data.size - 1).coerceAtLeast(1)),
                y = height - (value / maxVal * height).toFloat()
            )
        }

        if (points.isEmpty()) return@Canvas

        val path = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 0 until points.size - 1) {
                val p0 = points[i]
                val p1 = points[i + 1]
                val controlPoint1 = Offset((p0.x + p1.x) / 2, p0.y)
                val controlPoint2 = Offset((p0.x + p1.x) / 2, p1.y)
                cubicTo(controlPoint1.x, controlPoint1.y, controlPoint2.x, controlPoint2.y, p1.x, p1.y)
            }
        }

        // Draw Line
        drawPath(
            path = path,
            color = CyanChart,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        // Draw Gradient Fill
        val fillPath = Path().apply {
            addPath(path)
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(CyanChart.copy(alpha = 0.5f), Color.Transparent),
                startY = 0f,
                endY = height
            )
        )
    }
}

@Composable
fun MemoryPieChart(used: Long, total: Long, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        val usedGb = String.format("%.1f", used / 1024.0 / 1024.0 / 1024.0)
        val totalGb = String.format("%.1f", total / 1024.0 / 1024.0 / 1024.0)
        val percentage = if (total > 0) used.toFloat() / total.toFloat() else 0f

        Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            val diameter = size.minDimension
            val strokeWidth = 30.dp.toPx()
            val topLeft = Offset((size.width - diameter) / 2 + strokeWidth / 2, (size.height - diameter) / 2 + strokeWidth / 2)
            val arcSize = Size(diameter - strokeWidth, diameter - strokeWidth)

            // Background Circle
            drawArc(
                color = Color.DarkGray,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth)
            )

            // Foreground Arc
            drawArc(
                color = MagentaChart,
                startAngle = -90f,
                sweepAngle = percentage * 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${(percentage * 100).toInt()}%",
                style = MaterialTheme.typography.h3,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "$usedGb / $totalGb GB",
                style = MaterialTheme.typography.body1,
                color = Color.LightGray
            )
        }
    }
}
