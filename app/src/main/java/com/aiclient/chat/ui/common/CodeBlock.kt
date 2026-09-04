package com.aiclient.chat.ui.common

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aiclient.chat.R
import kotlinx.coroutines.delay

private val PREVIEWABLE_LANGUAGES = setOf("html", "htm", "svg")

@Composable
fun CodeBlock(
    language: String,
    code: String,
    modifier: Modifier = Modifier,
    onPreview: ((String, String) -> Unit)? = null,
) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val keywordColor = if (isDark) Color(0xFFE38C6E) else Color(0xFFA85A40)
    val stringColor = if (isDark) Color(0xFF9FBF8F) else Color(0xFF4B7A3E)
    val commentColor = if (isDark) Color(0xFF8A8781) else Color(0xFF8A8574)
    val numberColor = if (isDark) Color(0xFFC9A86A) else Color(0xFF9C7A2E)

    val highlighted = remember(code, isDark) {
        SyntaxHighlighter.highlight(code, keywordColor, stringColor, commentColor, numberColor)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = language.ifBlank { stringResource(R.string.plain_text) },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (onPreview != null && language.lowercase() in PREVIEWABLE_LANGUAGES) {
                        TextButton(onClick = { onPreview(language, code) }) {
                            Icon(Icons.Outlined.Visibility, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                            Text(stringResource(R.string.artifact_preview), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    IconButton(onClick = {
                        clipboard.setText(AnnotatedString(code))
                        copied = true
                    }) {
                        Icon(
                            if (copied) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                            contentDescription = stringResource(R.string.copy),
                        )
                    }
                }
            }
            Text(
                text = highlighted,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            )
        }
    }

    LaunchedEffect(copied) {
        if (copied) {
            delay(1500)
            copied = false
        }
    }
}

