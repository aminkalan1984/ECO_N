package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.HourlyPowerPoint
import com.example.domain.PersianFormat
import com.example.domain.TelemetryCalc
import com.example.domain.TelemetryPoint
import com.example.ui.theme.EcoTheme
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

enum class GenerationTrendMode(val label: String) {
    HOURLY("روند ساعتی امروز"),
    WEEKLY_DAILY("روند روزانه هفته")
}

private val FULL_DAY_NAMES = mapOf(
    "ش" to "شنبه",
    "ی" to "یکشنبه",
    "د" to "دوشنبه",
    "س" to "سه‌شنبه",
    "چ" to "چهارشنبه",
    "پ" to "پنج‌شنبه",
    "ج" to "جمعه"
)

@Composable
fun DailyGenerationTrendChart(
    peakKw: Double,
    todayKwh: Double,
    weekSeries: List<TelemetryPoint>,
    performanceRatioPct: Int,
    nowHour: Double = 14.5,
    modifier: Modifier = Modifier
) {
    val colors = EcoTheme.colors
    val cardColor = MaterialTheme.colorScheme.surface
    var mode by remember { mutableStateOf(GenerationTrendMode.HOURLY) }
    var showOptimalCurve by remember { mutableStateOf(true) }
    var selectedHourlyIndex by remember { mutableStateOf<Int?>(6) } // Default peak noon 12:00
    var selectedDailyIndex by remember { mutableStateOf<Int?>(weekSeries.lastIndex.takeIf { it >= 0 }) }

    val hourlyPoints = remember(peakKw, nowHour) {
        TelemetryCalc.buildHourlyPowerSeries(peakKw = peakKw, nowHour = nowHour)
    }
    val dailyAvgKwh = remember(weekSeries) {
        if (weekSeries.isEmpty()) 0.0 else weekSeries.sumOf { it.value } / weekSeries.size.toDouble()
    }

    val revealProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 650),
        label = "trendReveal"
    )

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = cardColor,
        shadowElevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "نمودار روند تولید روزانه نیروگاه خورشیدی"
            }
            .testTag("daily_generation_trend_chart")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header & Trend Mode Switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(colors.sunSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ShowChart,
                            contentDescription = null,
                            tint = colors.sunDeep,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "روند تولید روزانه برق خورشیدی",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "لمس یا کشیدن روی نمودار برای مشاهده جزئیات",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.mutedText
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Mode Pills Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GenerationTrendMode.entries.forEach { m ->
                    val active = mode == m
                    Surface(
                        onClick = { mode = m },
                        shape = RoundedCornerShape(50),
                        color = if (active) colors.ink else MaterialTheme.colorScheme.background,
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .testTag("trend_mode_${m.name.lowercase()}")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = m.label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                color = if (active) colors.inkForeground else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Active Inspection Tooltip Pill (Recharts-style interactive tooltip)
            if (mode == GenerationTrendMode.HOURLY) {
                val idx = (selectedHourlyIndex ?: 6).coerceIn(0, hourlyPoints.lastIndex.coerceAtLeast(0))
                val pt = hourlyPoints.getOrNull(idx)
                if (pt != null) {
                    val primaryPower = pt.actualKw ?: pt.forecastKw ?: 0.0
                    val kindLabel = if (pt.actualKw != null) "تولید واقعی" else "پیش‌بینی"
                    val pctOfPeak = if (peakKw > 0.0) ((primaryPower / peakKw) * 100.0).roundToInt() else 0
                    TrendTooltipBanner(
                        leading = "ساعت ${pt.label}",
                        primary = "$kindLabel: ${PersianFormat.fa(primaryPower, 1)} kW",
                        secondary = "${PersianFormat.fa(pctOfPeak)}٪ اوج توان",
                        badgeColor = if (pt.actualKw != null) colors.sunDeep else colors.ink
                    )
                }
            } else if (weekSeries.isNotEmpty()) {
                val idx = (selectedDailyIndex ?: weekSeries.lastIndex).coerceIn(0, weekSeries.lastIndex)
                val dayPt = weekSeries[idx]
                val fullDay = FULL_DAY_NAMES[dayPt.label] ?: dayPt.label
                val diffPct = if (dailyAvgKwh > 0.0) {
                    (((dayPt.value - dailyAvgKwh) / dailyAvgKwh) * 100.0).roundToInt()
                } else 0
                val diffLabel = if (diffPct >= 0) "+${PersianFormat.fa(diffPct)}٪ از میانگین" else "${PersianFormat.fa(diffPct)}٪ از میانگین"
                TrendTooltipBanner(
                    leading = fullDay,
                    primary = "${PersianFormat.fa(dayPt.value, 1)} kWh",
                    secondary = diffLabel,
                    badgeColor = if (diffPct >= 0) colors.success else colors.warn
                )
            }

            Spacer(Modifier.height(10.dp))

            // Interactive Canvas Visualization
            if (mode == GenerationTrendMode.HOURLY) {
                HourlyPowerAreaCanvas(
                    points = hourlyPoints,
                    peakKw = peakKw,
                    showOptimalCurve = showOptimalCurve,
                    selectedIndex = selectedHourlyIndex,
                    revealProgress = revealProgress,
                    onSelectIndex = { selectedHourlyIndex = it }
                )
            } else {
                WeeklyDailyTrendCanvas(
                    series = weekSeries,
                    avgTargetKwh = dailyAvgKwh,
                    selectedIndex = selectedDailyIndex,
                    revealProgress = revealProgress,
                    onSelectIndex = { selectedDailyIndex = it }
                )
            }

            Spacer(Modifier.height(10.dp))

            // Interactive Legend Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TrendLegendItem(
                        color = colors.sunDeep,
                        label = if (mode == GenerationTrendMode.HOURLY) "تولید واقعی" else "تولید روزانه"
                    )
                    TrendLegendItem(
                        color = colors.ink.copy(alpha = 0.65f),
                        label = if (mode == GenerationTrendMode.HOURLY) "پیش‌بینی" else "میانگین هدف",
                        dashed = true
                    )
                }

                if (mode == GenerationTrendMode.HOURLY) {
                    Surface(
                        onClick = { showOptimalCurve = !showOptimalCurve },
                        shape = RoundedCornerShape(50),
                        color = if (showOptimalCurve) colors.sunSoft else MaterialTheme.colorScheme.background,
                        modifier = Modifier.testTag("toggle_optimal_curve_chip")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.WbSunny,
                                contentDescription = null,
                                tint = if (showOptimalCurve) colors.sunDeep else colors.mutedText,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "تابش بهینه",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (showOptimalCurve) MaterialTheme.colorScheme.onBackground else colors.mutedText
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Compact KPI Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TrendMiniKpi("اوج لحظه‌ای", "${PersianFormat.fa(peakKw, 1)} kW")
                TrendMiniKpi("تولید امروز", "${PersianFormat.fa(todayKwh, 1)} kWh")
                TrendMiniKpi("میانگین هفته", "${PersianFormat.fa(dailyAvgKwh, 1)} kWh")
                TrendMiniKpi("بازده PR", "${PersianFormat.fa(performanceRatioPct)}٪")
            }
        }
    }
}

@Composable
private fun TrendTooltipBanner(
    leading: String,
    primary: String,
    secondary: String,
    badgeColor: Color
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(badgeColor)
                )
                Text(
                    text = leading,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = primary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = secondary,
                style = MaterialTheme.typography.labelSmall,
                color = EcoTheme.colors.mutedText
            )
        }
    }
}

@Composable
private fun HourlyPowerAreaCanvas(
    points: List<HourlyPowerPoint>,
    peakKw: Double,
    showOptimalCurve: Boolean,
    selectedIndex: Int?,
    revealProgress: Float,
    onSelectIndex: (Int) -> Unit
) {
    val colors = EcoTheme.colors
    val surfaceColor = MaterialTheme.colorScheme.surface
    val yMax = max(4.0, ceil(peakKw * 1.15))

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(196.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(points) {
                        detectTapGestures { offset ->
                            if (points.isNotEmpty() && size.width > 0) {
                                val left = 12.dp.toPx()
                                val right = size.width - 34.dp.toPx()
                                val ratio = ((offset.x - left) / (right - left)).coerceIn(0f, 1f)
                                val idx = (ratio * (points.size - 1)).roundToInt().coerceIn(0, points.lastIndex)
                                onSelectIndex(idx)
                            }
                        }
                    }
                    .pointerInput(points) {
                        detectHorizontalDragGestures { change, _ ->
                            change.consume()
                            if (points.isNotEmpty() && size.width > 0) {
                                val left = 12.dp.toPx()
                                val right = size.width - 34.dp.toPx()
                                val ratio = ((change.position.x - left) / (right - left)).coerceIn(0f, 1f)
                                val idx = (ratio * (points.size - 1)).roundToInt().coerceIn(0, points.lastIndex)
                                onSelectIndex(idx)
                            }
                        }
                    }
            ) {
                if (points.size < 2) return@Canvas
                val left = 12.dp.toPx()
                val right = size.width - 34.dp.toPx()
                val top = 14.dp.toPx()
                val bottom = size.height - 24.dp.toPx()
                val chartW = right - left
                val chartH = bottom - top

                fun xForIdx(i: Int): Float =
                    left + (i.toFloat() / (points.size - 1).toFloat()) * chartW

                fun yForVal(v: Double): Float =
                    bottom - ((v / yMax).toFloat().coerceIn(0f, 1f) * chartH)

                val dash = PathEffect.dashPathEffect(floatArrayOf(6f, 8f), 0f)
                listOf(0.0, yMax / 2.0, yMax).forEach { tick ->
                    drawLine(
                        color = colors.trackStrong,
                        start = Offset(left, yForVal(tick)),
                        end = Offset(right, yForVal(tick)),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = dash
                    )
                }

                // Clear-sky optimal reference curve
                if (showOptimalCurve) {
                    val optOffsets = points.mapIndexed { i, p -> Offset(xForIdx(i), yForVal(p.optimalKw)) }
                    val optPath = buildSmoothBezierPath(optOffsets)
                    drawPath(
                        path = optPath,
                        color = colors.mutedText.copy(alpha = 0.45f),
                        style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 6f), 0f))
                    )
                }

                // Actual power curve + gradient area
                val actualPairs = points.mapIndexedNotNull { i, p ->
                    p.actualKw?.let { Offset(xForIdx(i), yForVal(it * revealProgress)) }
                }
                if (actualPairs.size >= 2) {
                    val linePath = buildSmoothBezierPath(actualPairs)
                    val areaPath = Path().apply {
                        addPath(linePath)
                        lineTo(actualPairs.last().x, bottom)
                        lineTo(actualPairs.first().x, bottom)
                        close()
                    }
                    drawPath(
                        path = areaPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(colors.sun.copy(alpha = 0.45f), colors.sunSoft.copy(alpha = 0.05f)),
                            startY = top,
                            endY = bottom
                        )
                    )
                    drawPath(
                        path = linePath,
                        color = colors.sunDeep,
                        style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }

                // Forecast dashed curve
                val forecastPairs = points.mapIndexedNotNull { i, p ->
                    p.forecastKw?.let { Offset(xForIdx(i), yForVal(it * revealProgress)) }
                }
                if (forecastPairs.size >= 2) {
                    val fPath = buildSmoothBezierPath(forecastPairs)
                    drawPath(
                        path = fPath,
                        color = colors.ink.copy(alpha = 0.65f),
                        style = Stroke(
                            width = 2.2.dp.toPx(),
                            cap = StrokeCap.Round,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                        )
                    )
                }

                // Active interactive cursor + point marker
                val activeIdx = (selectedIndex ?: 6).coerceIn(0, points.lastIndex)
                val activePt = points[activeIdx]
                val activePower = activePt.actualKw ?: activePt.forecastKw ?: activePt.optimalKw
                val activeX = xForIdx(activeIdx)
                val activeY = yForVal(activePower * revealProgress)
                val markerColor = if (activePt.actualKw != null) colors.sunDeep else colors.ink

                drawLine(
                    color = colors.ink.copy(alpha = 0.25f),
                    start = Offset(activeX, top),
                    end = Offset(activeX, bottom),
                    strokeWidth = 1.2.dp.toPx(),
                    pathEffect = dash
                )
                drawCircle(color = markerColor.copy(alpha = 0.2f), radius = 11.dp.toPx(), center = Offset(activeX, activeY))
                drawCircle(color = surfaceColor, radius = 6.dp.toPx(), center = Offset(activeX, activeY))
                drawCircle(
                    color = markerColor,
                    radius = 6.dp.toPx(),
                    center = Offset(activeX, activeY),
                    style = Stroke(width = 2.5.dp.toPx())
                )
            }

            // Y-axis ticks
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .padding(top = 6.dp, bottom = 22.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                Text("kW", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = colors.mutedText)
                Text(PersianFormat.fa(yMax.toInt()), style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = colors.mutedText)
                Text(PersianFormat.fa((yMax / 2).toInt()), style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = colors.mutedText)
                Text("۰", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = colors.mutedText)
            }

            // X-axis labels
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 34.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("۰۶:۰۰", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = colors.mutedText)
                Text("۱۰:۰۰", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = colors.mutedText)
                Text("۱۲:۰۰", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = colors.mutedText)
                Text("۱۵:۰۰", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = colors.mutedText)
                Text("۱۸:۰۰", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = colors.mutedText)
            }
        }
    }
}

@Composable
private fun WeeklyDailyTrendCanvas(
    series: List<TelemetryPoint>,
    avgTargetKwh: Double,
    selectedIndex: Int?,
    revealProgress: Float,
    onSelectIndex: (Int) -> Unit
) {
    val colors = EcoTheme.colors
    val surfaceColor = MaterialTheme.colorScheme.surface
    val maxVal = max(10.0, (series.maxOfOrNull { it.value } ?: 30.0) * 1.18)

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(172.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(series) {
                        detectTapGestures { offset ->
                            if (series.isNotEmpty() && size.width > 0) {
                                val left = 10.dp.toPx()
                                val right = size.width - 34.dp.toPx()
                                val ratio = ((offset.x - left) / (right - left)).coerceIn(0f, 1f)
                                val idx = (ratio * (series.size - 1)).roundToInt().coerceIn(0, series.lastIndex)
                                onSelectIndex(idx)
                            }
                        }
                    }
                    .pointerInput(series) {
                        detectHorizontalDragGestures { change, _ ->
                            change.consume()
                            if (series.isNotEmpty() && size.width > 0) {
                                val left = 10.dp.toPx()
                                val right = size.width - 34.dp.toPx()
                                val ratio = ((change.position.x - left) / (right - left)).coerceIn(0f, 1f)
                                val idx = (ratio * (series.size - 1)).roundToInt().coerceIn(0, series.lastIndex)
                                onSelectIndex(idx)
                            }
                        }
                    }
            ) {
                if (series.isEmpty()) return@Canvas
                val left = 10.dp.toPx()
                val right = size.width - 34.dp.toPx()
                val top = 14.dp.toPx()
                val bottom = size.height - 8.dp.toPx()
                val chartW = right - left
                val chartH = bottom - top

                fun xForIdx(i: Int): Float =
                    if (series.size == 1) (left + right) / 2f
                    else left + (i.toFloat() / (series.size - 1).toFloat()) * chartW

                fun yForVal(v: Double): Float =
                    bottom - ((v / maxVal).toFloat().coerceIn(0f, 1f) * chartH)

                val dash = PathEffect.dashPathEffect(floatArrayOf(6f, 8f), 0f)
                listOf(0.0, maxVal / 2.0, maxVal).forEach { tick ->
                    drawLine(
                        color = colors.trackStrong,
                        start = Offset(left, yForVal(tick)),
                        end = Offset(right, yForVal(tick)),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = dash
                    )
                }

                // Target reference line
                val avgY = yForVal(avgTargetKwh)
                drawLine(
                    color = colors.ink.copy(alpha = 0.65f),
                    start = Offset(left, avgY),
                    end = Offset(right, avgY),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
                )

                // Soft daily bars behind trend line
                val barWidth = (chartW / series.size.toFloat()) * 0.36f
                val activeIdx = (selectedIndex ?: series.lastIndex).coerceIn(0, series.lastIndex)
                series.forEachIndexed { idx, pt ->
                    val cx = xForIdx(idx)
                    val topY = yForVal(pt.value * revealProgress)
                    drawRoundRect(
                        color = if (idx == activeIdx) colors.sun.copy(alpha = 0.7f) else colors.sunSoft,
                        topLeft = Offset(cx - barWidth / 2f, topY),
                        size = Size(barWidth, (bottom - topY).coerceAtLeast(2f)),
                        cornerRadius = CornerRadius(10f, 10f)
                    )
                }

                // Smooth trend line on top
                val linePts = series.mapIndexed { idx, pt -> Offset(xForIdx(idx), yForVal(pt.value * revealProgress)) }
                if (linePts.size >= 2) {
                    val trendPath = buildSmoothBezierPath(linePts)
                    drawPath(
                        path = trendPath,
                        color = colors.sunDeep,
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }

                linePts.forEachIndexed { idx, pt ->
                    val isSel = idx == activeIdx
                    if (isSel) {
                        drawCircle(color = colors.sunDeep.copy(alpha = 0.22f), radius = 10.dp.toPx(), center = pt)
                    }
                    drawCircle(color = surfaceColor, radius = if (isSel) 5.5.dp.toPx() else 3.5.dp.toPx(), center = pt)
                    drawCircle(
                        color = colors.sunDeep,
                        radius = if (isSel) 5.5.dp.toPx() else 3.5.dp.toPx(),
                        center = pt,
                        style = Stroke(width = if (isSel) 2.5.dp.toPx() else 1.8.dp.toPx())
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .padding(vertical = 6.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                Text("kWh", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = colors.mutedText)
                Text(PersianFormat.fa(maxVal.roundToInt()), style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = colors.mutedText)
                Text(PersianFormat.fa((maxVal / 2.0).roundToInt()), style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = colors.mutedText)
                Text("۰", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = colors.mutedText)
            }
        }

        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 6.dp, end = 34.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            series.forEachIndexed { idx, pt ->
                val isSel = idx == (selectedIndex ?: series.lastIndex)
                Text(
                    text = pt.label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = if (isSel) FontWeight.Black else FontWeight.Normal
                    ),
                    color = if (isSel) MaterialTheme.colorScheme.onBackground else colors.mutedText
                )
            }
        }
    }
}

private fun buildSmoothBezierPath(points: List<Offset>): Path {
    val path = Path()
    if (points.isEmpty()) return path
    path.moveTo(points.first().x, points.first().y)
    for (i in 0 until points.lastIndex) {
        val p0 = points[i]
        val p1 = points[i + 1]
        val midX = (p0.x + p1.x) / 2f
        path.cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
    }
    return path
}

@Composable
private fun TrendLegendItem(
    color: Color,
    label: String,
    dashed: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .width(14.dp)
                .height(if (dashed) 2.5.dp else 4.dp)
                .clip(RoundedCornerShape(50))
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = EcoTheme.colors.mutedText
        )
    }
}

@Composable
private fun TrendMiniKpi(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = EcoTheme.colors.mutedText
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.ExtraBold
        )
    }
}
