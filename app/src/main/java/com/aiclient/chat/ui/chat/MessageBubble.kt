package com.aiclient.chat.ui.chat

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.aiclient.chat.R
import com.aiclient.chat.data.model.ChatMessage
import com.aiclient.chat.data.model.Role
import com.aiclient.chat.data.util.ImageEncoder
import com.aiclient.chat.ui.common.MessageContent
import com.aiclient.chat.ui.theme.Clay500

@Composable
fun MessageBubble(
    message: ChatMessage,
    fontScale: Float,
    onEdit: (ChatMessage) -> Unit,
    onRegenerate: () -> Unit,
    onPreviewArtifact: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val clipboard = LocalClipboardManager.current
    val isUser = message.role == Role.USER

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
            modifier = Modifier.fillMaxWidth(if (isUser) 0.86f else 1f),
        ) {
            if (isUser) {
                Surface(
                    color = Clay500.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        if (message.images.isNotEmpty()) {
                            AttachmentThumbnails(message.images)
                        }
                        if (message.text.isNotBlank()) {
                            MessageContent(message.text, fontScale = fontScale, onPreviewArtifact = onPreviewArtifact)
                        }
                    }
                }
                MessageActions {
                    IconButton(onClick = { clipboard.setText(AnnotatedString(message.text)) }) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = stringResource(R.string.copy), modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = { onEdit(message) }) {
                        Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.edit_message), modifier = Modifier.size(16.dp))
                    }
                }
            } else {
                if (message.isStreaming && message.text.isBlank()) {
                    TypingIndicator()
                } else {
                    if (message.isError) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp),
                            )
                            androidx.compose.foundation.layout.Spacer(Modifier.size(6.dp))
                            Text(
                                message.text,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    } else {
                        MessageContent(message.text, fontScale = fontScale, onPreviewArtifact = onPreviewArtifact)
                    }
                }
                if (!message.isStreaming && message.text.isNotBlank()) {
                    MessageActions {
                        IconButton(onClick = { clipboard.setText(AnnotatedString(message.text)) }) {
                            Icon(Icons.Outlined.ContentCopy, contentDescription = stringResource(R.string.copy), modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = onRegenerate) {
                            Icon(Icons.Outlined.Refresh, contentDescription = stringResource(R.string.regenerate), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageActions(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.padding(top = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(0.dp),
        content = content,
    )
}

@Composable
private fun AttachmentThumbnails(images: List<String>) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        images.take(4).forEach { dataUri ->
            val bitmap = remember(dataUri) { ImageEncoder.decodeDataUri(dataUri)?.asImageBitmap() }
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                bitmap?.let {
                    Image(
                        bitmap = it,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
private fun TypingIndicator() {
    val transition = rememberInfiniteTransition(label = "typing")
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(vertical = 6.dp)) {
        repeat(3) { index ->
            val alpha by transition.animateFloat(
                initialValue = 0.25f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, delayMillis = index * 150, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "dot$index",
            )
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(Clay500.copy(alpha = alpha)),
            )
        }
    }
}
