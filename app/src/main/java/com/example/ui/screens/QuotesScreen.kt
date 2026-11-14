package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.PersianFormat
import com.example.ui.components.DropdownPill
import com.example.ui.components.EcoPill
import com.example.ui.components.EcoScreenShell
import com.example.ui.components.Footnote
import com.example.ui.components.HeroStat
import com.example.ui.components.InkButton
import com.example.ui.components.PanelTitle
import com.example.ui.components.PillTone
import com.example.ui.components.ScreenHeader
import com.example.ui.theme.EcoTheme
import com.example.ui.viewmodel.EcoViewModel
import kotlinx.coroutines.delay

private data class SupplierQuote(
    val id: String,
    val supplier: String,
    val items: String,
    val priceRial: Long,
    val rating: Double,
    val validSeconds: Int
)

private val SUPPLIER_QUOTES = listOf(
    SupplierQuote(
        id = "a",
        supplier = "تأمین‌کننده الف",
        items = "پنل ۹ عدد، اینورتر ۵ کیلووات، سازه و کابل",
        priceRial = 3_032_000_000L,
        rating = 4.7,
        validSeconds = 5 * 3600 + 32 * 60 + 10
    ),
    SupplierQuote(
        id = "b",
        supplier = "تأمین‌کننده ب",
        items = "پنل ۹ عدد، اینورتر ۵ کیلووات، سازه و کابل",
        priceRial = 3_061_000_000L,
        rating = 4.5,
        validSeconds = 27 * 3600 + 15 * 60
    ),
    SupplierQuote(
        id = "c",
        supplier = "تأمین‌کننده ج",
        items = "پنل ۹ عدد، اینورتر ۵ کیلووات، سازه و کابل",
        priceRial = 3_028_000_000L,
        rating = 4.2,
        validSeconds = 0
    )
)

private enum class QuoteSort(val label: String) {
    DEFAULT("مرتب‌سازی"),
    PRICE("ارزان‌ترین"),
    RATING("بیشترین امتیاز")
}

@Composable
fun QuotesScreen(
    ecoViewModel: EcoViewModel,
    onBack: () -> Unit,
    onOrderPlaced: () -> Unit
) {
    val ecoState by ecoViewModel.ecoState.collectAsStateWithLifecycle()
    val colors = EcoTheme.colors

    var sort by remember { mutableStateOf(QuoteSort.DEFAULT) }
    var selectedId by remember(ecoState.quoteId) { mutableStateOf(ecoState.quoteId) }
    var elapsedSec by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000L)
            elapsedSec++
        }
    }

    val liveQuotes = remember(sort, elapsedSec) {
        val base = SUPPLIER_QUOTES.map { it to (it.validSeconds - elapsedSec).coerceAtLeast(0) }
        when (sort) {
            QuoteSort.DEFAULT -> base
            QuoteSort.PRICE -> base.sortedBy { it.first.priceRial }
            QuoteSort.RATING -> base.sortedByDescending { it.first.rating }
        }
    }
    val validQuotes = liveQuotes.filter { it.second > 0 }
    val lowestPrice = validQuotes.minOfOrNull { it.first.priceRial } ?: 0L
    val activeQuote = liveQuotes.firstOrNull { it.first.id == selectedId && it.second > 0 }?.first

    EcoScreenShell(
        topContent = {
            ScreenHeader(
                title = "پیش‌فاکتورهای تجهیزات",
                subtitle = "استعلام قیمت طرح ۵ کیلووات",
                onStartClick = onBack
            )

            HeroStat(
                label = "کمترین قیمت معتبر",
                value = PersianFormat.faFixed(lowestPrice / 1_000_000_000.0, 2),
                unit = "میلیارد ریال",
                aside = {
                    DropdownPill(
                        value = sort,
                        options = QuoteSort.entries.map { it to it.label },
                        onChange = { sort = it },
                        modifier = Modifier.testTag("quotes_sort_dropdown")
                    )
                }
            )
            Spacer(Modifier.height(10.dp))
            EcoPill(
                text = "${PersianFormat.fa(SUPPLIER_QUOTES.size)} پیش‌فاکتور دریافت شد",
                tone = PillTone.SUN
            )
        },
        sunContent = {
            PanelTitle(
                title = "مقایسه فهرست تجهیزات",
                aside = {
                    Text(
                        text = "اعتبار محدود به دلیل نوسان ارز",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }
            )
            Spacer(Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                liveQuotes.forEach { (quote, leftSeconds) ->
                    val expired = leftSeconds <= 0
                    val isSelected = selectedId == quote.id && !expired
                    Surface(
                        onClick = {
                            if (expired) {
                                ecoViewModel.emitToast("این پیش‌فاکتور منقضی شده و قابل انتخاب نیست")
                            } else {
                                selectedId = quote.id
                            }
                        },
                        shape = RoundedCornerShape(28.dp),
                        color = if (expired) colors.sunWash else MaterialTheme.colorScheme.surface,
                        border = if (isSelected) BorderStroke(2.dp, colors.ink) else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("quote_card_${quote.id}")
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .border(
                                            width = 2.dp,
                                            color = if (expired) colors.trackStrong else colors.ink,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(colors.ink)
                                        )
                                    }
                                }
                                Text(
                                    text = quote.supplier,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (expired) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    else MaterialTheme.colorScheme.onSurface
                                )
                                EcoPill(
                                    text = "گواهی ساتبا",
                                    tone = if (expired) PillTone.MUTED else PillTone.SUCCESS,
                                    small = true
                                )
                                Spacer(Modifier.weight(1f))
                                Text(
                                    text = "امتیاز ${PersianFormat.fa(quote.rating, 1)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (expired) colors.mutedText else MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = quote.items,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.mutedText,
                                modifier = Modifier.padding(start = 32.dp)
                            )

                            Spacer(Modifier.height(10.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 32.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${PersianFormat.fa(quote.priceRial)} ریال",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    textDecoration = if (expired) TextDecoration.LineThrough else null,
                                    color = if (expired) colors.mutedText else MaterialTheme.colorScheme.onSurface
                                )
                                if (expired) {
                                    EcoPill(text = "منقضی شده", tone = PillTone.DANGER, small = true)
                                } else {
                                    EcoPill(
                                        text = "اعتبار تا ${PersianFormat.faTimer(leftSeconds, true)}",
                                        tone = PillTone.WARN,
                                        small = true,
                                        leadingIcon = Icons.Filled.Schedule
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Footnote(
                text = "پرداخت مستقیم از بانک عامل به تأمین‌کننده انجام می‌شود",
                icon = Icons.Filled.Lock
            )

            Spacer(Modifier.height(16.dp))
            InkButton(
                text = "انتخاب و ثبت سفارش",
                enabled = activeQuote != null,
                onClick = {
                    val chosen = activeQuote ?: return@InkButton
                    ecoViewModel.placeQuoteOrder(chosen.id)
                    ecoViewModel.emitToast("سفارش با ${chosen.supplier} ثبت شد")
                    onOrderPlaced()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quotes_place_order_button")
            )
            Footnote(text = "پیش‌فاکتور منقضی به سفارش تبدیل نمی‌شود")
        }
    )
}
