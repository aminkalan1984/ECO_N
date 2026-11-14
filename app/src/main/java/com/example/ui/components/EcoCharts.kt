package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.PersianFormat
import com.example.domain.TelemetryCalc
import com.example.ui.theme.EcoTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ProductionAreaChart(
    peakKw: Double = 3.6,
    nowHour: Double = 14.6,
    modifier: Modifier = Modifier
) {
    val colors = EcoTheme.colors
    val cardColor = MaterialTheme.colorScheme.surface

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .height(210.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val left = 16.dp.toPx()
                val right = size.width - 36.dp.toPx()
                val top = 42.dp.toPx()
                val bottom = size.height - 26.dp.toPx()
                val hMin = 5.5
                val hMax = 19.0
                val yMax = 4.0

                fun x(h: Double): Float =
                    left + ((h - hMin) / (hMax - hMin)).toFloat() * (right - left)

                fun y(v: Double): Float =
                    bottom - (v / yMax).toFloat() * (bottom - top)

                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 10f), 0f)
                listOf(0.0, 2.0, 4.0).forEach { tick ->
                    drawLine(
                        color = colors.trackStrong,
                        start = Offset(left, y(tick)),
                        end = Offset(right, y(tick)),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = dashEffect
                    )
                }

                val actualPts = mutableListOf<Offset>()
                var h = hMin
                while (h <= nowHour + 0.001) {
                    actualPts.add(Offset(x(h), y(TelemetryCalc.productionAt(h, peakKw))))
                    h += 0.2
                }
                actualPts.add(Offset(x(nowHour), y(TelemetryCalc.productionAt(nowHour, peakKw))))

                val areaPath = Path().apply {
                    actualPts.forEachIndexed { idx, pt ->
                        if (idx == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
                    }
                    lineTo(x(nowHour), bottom)
                    lineTo(x(hMin), bottom)
                    close()
                }
                drawPath(path = areaPath, color = colors.sunSoft)

                val actualLinePath = Path().apply {
                    actualPts.forEachIndexed { idx, pt ->
                        if (idx == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
                    }
                }
                drawPath(
                    path = actualLinePath,
                    color = colors.sunDeep,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                val forecastPts = mutableListOf<Offset>()
                val bandTop = mutableListOf<Offset>()
                val bandBottom = mutableListOf<Offset>()
                var fh = nowHour
                while (fh <= 18.6) {
                    val v = TelemetryCalc.productionAt(fh, peakKw) * 0.94
                    val spread = (fh - nowHour) * 0.14
                    forecastPts.add(Offset(x(fh), y(v)))
                    bandTop.add(Offset(x(fh), y((v + spread).coerceAtMost(yMax))))
                    bandBottom.add(Offset(x(fh), y((v - spread).coerceAtLeast(0.0))))
                    fh += 0.2
                }
                if (bandTop.isNotEmpty()) {
                    val bandPath = Path().apply {
                        bandTop.forEachIndexed { idx, pt ->
                            if (idx == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
                        }
                        bandBottom.asReversed().forEach { pt -> lineTo(pt.x, pt.y) }
                        close()
                    }
                    drawPath(path = bandPath, color = colors.sunSoft.copy(alpha = 0.8f))
                }
                if (forecastPts.isNotEmpty()) {
                    val forecastPath = Path().apply {
                        forecastPts.forEachIndexed { idx, pt ->
                            if (idx == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
                        }
                    }
                    drawPath(
                        path = forecastPath,
                        color = colors.sunDeep,
                        style = Stroke(
                            width = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                    )
                }

                // Vertical peak guide and now indicator
                drawLine(
                    color = colors.sunDeep,
                    start = Offset(x(12.0), top - 6.dp.toPx()),
                    end = Offset(x(12.0), y(peakKw)),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = dashEffect
                )
                drawLine(
                    color = colors.trackStrong,
                    start = Offset(x(nowHour), top - 6.dp.toPx()),
                    end = Offset(x(nowHour), bottom),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = dashEffect
                )
                val nowCenter = Offset(x(nowHour), y(TelemetryCalc.productionAt(nowHour, peakKw)))
                drawCircle(color = cardColor, radius = 6.dp.toPx(), center = nowCenter)
                drawCircle(
                    color = colors.sunDeep,
                    radius = 6.dp.toPx(),
                    center = nowCenter,
                    style = Stroke(width = 2.5.dp.toPx())
                )
            }

            // Y ticks & unit label
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .padding(top = 18.dp, bottom = 22.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                Text("kW", style = MaterialTheme.typography.labelSmall, color = colors.mutedText)
                Text(PersianFormat.faDigits(4), style = MaterialTheme.typography.labelSmall, color = colors.mutedText)
                Text(PersianFormat.faDigits(2), style = MaterialTheme.typography.labelSmall, color = colors.mutedText)
                Text(PersianFormat.faDigits(0), style = MaterialTheme.typography.labelSmall, color = colors.mutedText)
            }

            // Peak pill
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(end = 18.dp),
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 3.dp
            ) {
                Text(
                    text = "اوج تولید ${PersianFormat.fa(peakKw, 1)} kW",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                )
            }

            // Forecast badge
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 48.dp, end = 38.dp),
                shape = RoundedCornerShape(50),
                color = colors.sunSoft
            ) {
                Text(
                    text = "پیش‌بینی | سناریو",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                )
            }

            // X labels
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 36.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("۶ صبح", style = MaterialTheme.typography.labelSmall, color = colors.mutedText)
                Text("۱۲ ظهر", style = MaterialTheme.typography.labelSmall, color = colors.mutedText)
                Text("۶ عصر", style = MaterialTheme.typography.labelSmall, color = colors.mutedText)
            }
        }
    }
}

data class BarDatum(
    val label: String,
    val value: Double
)

@Composable
fun EcoBarChart(
    data: List<BarDatum>,
    max: Double,
    ticks: List<Int>,
    unit: String = "kWh",
    selectedIndex: Int? = null,
    onSelect: ((Int) -> Unit)? = null,
    tooltipText: ((Int, BarDatum) -> String)? = null,
    footLabel: String? = null,
    chartHeightDp: Int = 180,
    modifier: Modifier = Modifier
) {
    val colors = EcoTheme.colors
    val safeMax = max.coerceAtLeast(1.0)

    Column(modifier = modifier.fillMaxWidth().padding(top = 14.dp)) {
        Text(
            text = unit,
            style = MaterialTheme.typography.labelSmall,
            color = colors.mutedText
        )
        Spacer(Modifier.height(6.dp))

        if (selectedIndex != null && selectedIndex in data.indices && tooltipText != null) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 3.dp
                ) {
                    Text(
                        text = tooltipText(selectedIndex, data[selectedIndex]),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeightDp.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Tick labels column
            Column(
                modifier = Modifier
                    .width(28.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                ticks.sortedDescending().forEach { tick ->
                    Text(
                        text = PersianFormat.fa(tick),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = colors.mutedText
                    )
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                // Dashed gridlines
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val dash = PathEffect.dashPathEffect(floatArrayOf(6f, 8f), 0f)
                    ticks.forEach { t ->
                        val yPos = size.height * (1f - (t / safeMax).toFloat()).coerceIn(0f, 1f)
                        drawLine(
                            color = colors.trackStrong,
                            start = Offset(0f, yPos),
                            end = Offset(size.width, yPos),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = dash
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    data.forEachIndexed { idx, datum ->
                        val isSel = selectedIndex == idx
                        val fraction = (datum.value / safeMax).toFloat().coerceIn(0.04f, 1f)
                        val animatedFraction by animateFloatAsState(
                            targetValue = fraction,
                            animationSpec = tween(450),
                            label = "bar_$idx"
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable(enabled = onSelect != null) { onSelect?.invoke(idx) },
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(animatedFraction)
                                    .clip(RoundedCornerShape(50))
                                    .background(if (isSel) colors.sunDeep else colors.sun)
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Spacer(Modifier.width(28.dp))
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                data.forEachIndexed { idx, datum ->
                    val isSel = selectedIndex == idx
                    Text(
                        text = datum.label,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = if (isSel) FontWeight.Black else FontWeight.Normal
                        ),
                        color = if (isSel) MaterialTheme.colorScheme.onBackground else colors.mutedText,
                        maxLines = 1
                    )
                }
            }
        }
        if (!footLabel.isNullOrBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = footLabel,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = colors.mutedText
            )
        }
    }
}

data class SetpointStep(
    val from: Double,
    val to: Double,
    val temp: Int
)

@Composable
fun TempPlanChart(
    steps: List<SetpointStep>,
    shift: Double = 0.0,
    preCoolTemp: Int,
    modifier: Modifier = Modifier
) {
    val colors = EcoTheme.colors
    Column(modifier = modifier.fillMaxWidth().padding(top = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendChip(color = colors.sunDeep, label = "دمای پیشنهادی")
                LegendChip(color = colors.trackStrong, label = "دمای بیرون")
            }
            Text("درجه", style = MaterialTheme.typography.labelSmall, color = colors.mutedText)
        }

        Spacer(Modifier.height(8.dp))

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val left = 12.dp.toPx()
                    val right = size.width - 28.dp.toPx()
                    val top = 12.dp.toPx()
                    val bottom = size.height - 24.dp.toPx()
                    val tMin = 20.0
                    val tMax = 40.0

                    fun x(h: Double): Float = left + (h / 24.0).toFloat() * (right - left)
                    fun y(t: Double): Float = bottom - ((t - tMin) / (tMax - tMin)).toFloat() * (bottom - top)

                    // Peak tariff zone (19..23)
                    drawRect(
                        color = colors.dangerSoft,
                        topLeft = Offset(x(19.0), top),
                        size = Size(x(23.0) - x(19.0), bottom - top)
                    )

                    val dash = PathEffect.dashPathEffect(floatArrayOf(6f, 8f), 0f)
                    listOf(20.0, 30.0, 40.0).forEach { tick ->
                        drawLine(
                            color = colors.trackStrong,
                            start = Offset(left, y(tick)),
                            end = Offset(right, y(tick)),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = dash
                        )
                    }

                    // Step fill & line
                    val stepPts = mutableListOf<Offset>()
                    steps.forEach { s ->
                        stepPts.add(Offset(x(s.from), y(s.temp.toDouble())))
                        stepPts.add(Offset(x(s.to), y(s.temp.toDouble())))
                    }
                    if (stepPts.isNotEmpty()) {
                        val fillPath = Path().apply {
                            stepPts.forEachIndexed { idx, pt ->
                                if (idx == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
                            }
                            lineTo(x(24.0), bottom)
                            lineTo(x(0.0), bottom)
                            close()
                        }
                        drawPath(fillPath, color = colors.sunSoft)

                        val linePath = Path().apply {
                            stepPts.forEachIndexed { idx, pt ->
                                if (idx == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
                            }
                        }
                        drawPath(
                            path = linePath,
                            color = colors.sunDeep,
                            style = Stroke(width = 2.5.dp.toPx(), join = StrokeJoin.Round)
                        )
                    }

                    // Outside temperature curve
                    val outsidePath = Path()
                    var h = 0.0
                    var first = true
                    while (h <= 24.0) {
                        val pt = Offset(x(h), y(TelemetryCalc.outsideTempAt(h, shift)))
                        if (first) {
                            outsidePath.moveTo(pt.x, pt.y)
                            first = false
                        } else {
                            outsidePath.lineTo(pt.x, pt.y)
                        }
                        h += 0.25
                    }
                    drawPath(
                        path = outsidePath,
                        color = colors.mutedText,
                        style = Stroke(width = 1.6.dp.toPx())
                    )
                }

                // Peak label
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 18.dp, end = 42.dp),
                    color = Color.Transparent
                ) {
                    Text(
                        text = "اوج تعرفه",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.danger,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Pre-cool badge
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 38.dp, end = 66.dp),
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 3.dp
                ) {
                    Text(
                        text = "پیش‌سرمایش ${PersianFormat.fa(preCoolTemp)} درجه",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                // Y-axis temperature ticks
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .padding(top = 8.dp, bottom = 22.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    Text(PersianFormat.faDigits(40), style = MaterialTheme.typography.labelSmall, color = colors.mutedText)
                    Text(PersianFormat.faDigits(30), style = MaterialTheme.typography.labelSmall, color = colors.mutedText)
                    Text(PersianFormat.faDigits(20), style = MaterialTheme.typography.labelSmall, color = colors.mutedText)
                }

                // X-axis labels
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(start = 10.dp, end = 28.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("ساعت", style = MaterialTheme.typography.labelSmall, color = colors.mutedText)
                    Text("۳", style = MaterialTheme.typography.labelSmall, color = colors.mutedText)
                    Text("۹", style = MaterialTheme.typography.labelSmall, color = colors.mutedText)
                    Text("۱۵", style = MaterialTheme.typography.labelSmall, color = colors.mutedText)
                    Text("۲۱", style = MaterialTheme.typography.labelSmall, color = colors.mutedText)
                }
            }
        }
    }
}

@Composable
private fun LegendChip(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(
            modifier = Modifier
                .width(16.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(50))
                .background(color)
        )
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = EcoTheme.colors.mutedText)
    }
}

@Composable
fun CoverageGauge(
    ratio: Double,
    label: String,
    modifier: Modifier = Modifier
) {
    val colors = EcoTheme.colors
    val cardColor = MaterialTheme.colorScheme.surface
    val clamped = ratio.coerceIn(0.0, 1.0).toFloat()
    val animatedRatio by animateFloatAsState(
        targetValue = clamped,
        animationSpec = tween(700),
        label = "gaugeRatio"
    )

    Column(
        modifier = modifier.fillMaxWidth().padding(top = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Box(
                modifier = Modifier
                    .width(248.dp)
                    .height(140.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokePx = 18.dp.toPx()
                    val r = (size.width / 2f) - strokePx
                    val cx = size.width / 2f
                    val cy = size.height - 14.dp.toPx()
                    val topLeft = Offset(cx - r, cy - r)
                    val arcSize = Size(r * 2f, r * 2f)

                    drawArc(
                        color = colors.track,
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokePx, cap = StrokeCap.Round)
                    )

                    val sweep = (animatedRatio * 180f).coerceAtLeast(1f)
                    drawArc(
                        color = colors.sunDeep,
                        startAngle = 180f,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokePx, cap = StrokeCap.Round)
                    )

                    val theta = PI * (1.0 - animatedRatio.toDouble())
                    val ex = cx + r * cos(theta).toFloat()
                    val ey = cy - r * sin(theta).toFloat()
                    drawCircle(color = cardColor, radius = 10.dp.toPx(), center = Offset(ex, ey))
                    drawCircle(
                        color = colors.sunDeep,
                        radius = 10.dp.toPx(),
                        center = Offset(ex, ey),
                        style = Stroke(width = 4.dp.toPx())
                    )
                }

                Column(
                    modifier = Modifier.padding(bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${PersianFormat.fa((ratio * 100).toInt())}٪",
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.mutedText
                    )
                }
            }
            Row(
                modifier = Modifier.width(248.dp).padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("۰٪", style = MaterialTheme.typography.labelSmall, color = colors.mutedText)
                Text("۱۰۰٪", style = MaterialTheme.typography.labelSmall, color = colors.mutedText)
            }
        }
    }
}

@Composable
fun HBar(
    ratio: Double,
    fillColor: Color = EcoTheme.colors.sun,
    trackColor: Color = EcoTheme.colors.track,
    modifier: Modifier = Modifier
) {
    val target = ratio.toFloat().coerceIn(0.02f, 1f)
    val anim by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(600),
        label = "hbar"
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(12.dp)
            .clip(RoundedCornerShape(50))
            .background(trackColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(anim)
                .clip(RoundedCornerShape(50))
                .background(fillColor)
        )
    }
}
