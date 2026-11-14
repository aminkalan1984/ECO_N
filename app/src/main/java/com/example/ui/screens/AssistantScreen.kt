package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.domain.RateTables
import com.example.ui.components.EcoPill
import com.example.ui.components.PillTone
import com.example.ui.components.ScreenHeader
import com.example.ui.theme.EcoTheme
import com.example.ui.viewmodel.AssistantViewModel
import com.example.ui.viewmodel.EcoViewModel

private const val GREETING_TEXT =
    "سلام! من دستیار اکوسولار هستم. درباره قبض برق، مصرف لوازم، طراحی نیروگاه، باتری، خرید تضمینی یا خودتأمین و مراحل پرونده بپرسید."

private val QUICK_SUGGESTIONS = listOf(
    "تفاوت خرید تضمینی و خودتأمین چیست؟",
    "برای ۵ کیلووات چند کیلووات‌ساعت باتری لازم است؟",
    "اختلاف کنتور و اینورتر چرا مهم است؟",
    "مدارک پرونده مهرسان چیست؟"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AssistantScreen(
    ecoViewModel: EcoViewModel,
    assistantViewModel: AssistantViewModel,
    onBack: () -> Unit
) {
    val ecoState by ecoViewModel.ecoState.collectAsStateWithLifecycle()
    val draft by ecoViewModel.wizardDraft.collectAsStateWithLifecycle()
    val messages by assistantViewModel.messages.collectAsStateWithLifecycle()
    val chatUi by assistantViewModel.uiState.collectAsStateWithLifecycle()
    val colors = EcoTheme.colors

    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, chatUi.streamingReply) {
        val total = messages.size + 1 + (if (chatUi.isStreaming) 1 else 0)
        if (total > 0) {
            listState.animateScrollToItem(total - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .imePadding()
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
            ScreenHeader(
                title = "دستیار اکوسولار",
                subtitle = "گفتگو درباره نیروگاه، قبض و کانال درآمد",
                onStartClick = onBack
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                EcoPill(
                    text = "مبتنی بر ${RateTables.ACTIVE.label}",
                    tone = PillTone.WHITE,
                    small = true,
                    leadingIcon = Icons.Filled.AutoAwesome
                )
                Row(
                    modifier = Modifier
                        .clickable { assistantViewModel.clearChat() }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .testTag("assistant_clear_button"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Filled.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp), tint = colors.mutedText)
                    Text("پاک کردن گفتگو", style = MaterialTheme.typography.labelSmall, color = colors.mutedText)
                }
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            color = colors.sun
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 16.dp)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item(key = "greeting") {
                        ChatBubble(text = GREETING_TEXT, isUser = false)
                    }
                    items(messages, key = { it.id }) { msg ->
                        ChatBubble(text = msg.content, isUser = msg.role == "user")
                    }
                    if (chatUi.isStreaming) {
                        item(key = "streaming") {
                            ChatBubble(
                                text = chatUi.streamingReply.ifBlank { "دستیار در حال نوشتن…" },
                                isUser = false
                            )
                        }
                    }
                }

                if (!chatUi.error.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = chatUi.error.orEmpty(),
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.warn,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                if (messages.isEmpty() && !chatUi.isStreaming) {
                    Spacer(Modifier.height(10.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QUICK_SUGGESTIONS.forEachIndexed { idx, prompt ->
                            Surface(
                                onClick = {
                                    assistantViewModel.sendMessage(prompt, ecoState, draft)
                                },
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.testTag("assistant_suggestion_$idx")
                            ) {
                                Text(
                                    text = prompt,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it.take(2000) },
                        placeholder = { Text("سؤال خود را بنویسید…") },
                        maxLines = 3,
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = colors.ink,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("assistant_input")
                    )
                    IconButton(
                        onClick = {
                            val prompt = input.trim()
                            if (prompt.isNotEmpty()) {
                                input = ""
                                assistantViewModel.sendMessage(prompt, ecoState, draft)
                            }
                        },
                        enabled = !chatUi.isStreaming && input.trim().isNotEmpty(),
                        modifier = Modifier
                            .size(52.dp)
                            .background(
                                color = if (!chatUi.isStreaming && input.trim().isNotEmpty()) colors.ink
                                else colors.ink.copy(alpha = 0.4f),
                                shape = CircleShape
                            )
                            .testTag("assistant_send_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = stringResource(R.string.cd_send),
                            tint = colors.inkForeground
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    text = "پاسخ‌ها بر پایه ${RateTables.ACTIVE.label} است؛ اطلاعات حساس (کد ملی، شبای کامل، رمز) نفرستید.",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.mutedText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun ChatBubble(text: String, isUser: Boolean) {
    val colors = EcoTheme.colors
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isUser) Alignment.CenterStart else Alignment.CenterEnd
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = if (isUser) colors.ink else MaterialTheme.colorScheme.surface,
            modifier = Modifier.widthIn(max = 310.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isUser) colors.inkForeground else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }
    }
}
