package com.aiclient.chat.ui.chat

import android.annotation.SuppressLint
import android.webkit.WebView
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.aiclient.chat.R
import com.aiclient.chat.ui.common.SyntaxHighlighter

@Composable
fun ArtifactSheet(artifact: ArtifactPreview, onDismiss: () -> Unit) {
    var showingCode by remember(artifact) { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.close))
                    }
                    Row {
                        TextButton(onClick = { showingCode = false }) { Text(stringResource(R.string.artifact_preview)) }
                        TextButton(onClick = { showingCode = true }) { Text(stringResource(R.string.artifact_code)) }
                    }
                    IconButton(onClick = { clipboard.setText(AnnotatedString(artifact.code)) }) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = stringResource(R.string.copy))
                    }
                }
                if (showingCode) {
                    val keywordColor = MaterialTheme.colorScheme.primary
                    val stringColor = MaterialTheme.colorScheme.tertiary
                    val commentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    val numberColor = MaterialTheme.colorScheme.secondary
                    val highlighted = remember(artifact, keywordColor, stringColor, commentColor, numberColor) {
                        SyntaxHighlighter.highlight(artifact.code, keywordColor, stringColor, commentColor, numberColor)
                    }
                    Text(
                        text = highlighted,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .horizontalScroll(rememberScrollState())
                            .padding(16.dp),
                    )
                } else {
                    ArtifactWebView(html = wrapAsHtml(artifact.language, artifact.code), modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}

private fun wrapAsHtml(language: String, code: String): String = when (language.lowercase()) {
    "svg" -> "<html><body style=\"margin:0;display:flex;align-items:center;justify-content:center;height:100vh;\">$code</body></html>"
    else -> code
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun ArtifactWebView(html: String, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
        },
    )
}
