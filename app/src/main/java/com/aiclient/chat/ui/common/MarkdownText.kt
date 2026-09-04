package com.aiclient.chat.ui.common

import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import io.noties.markwon.Markwon
import io.noties.markwon.core.MarkwonTheme
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.linkify.LinkifyPlugin

/**
 * Renders inline/prose markdown (bold, italics, links, lists, tables, inline
 * code, block quotes) via Markwon. Fenced code blocks are handled separately
 * by [MessageContent] so they can get their own copy/preview affordances.
 */
@Composable
fun MarkdownText(markdown: String, modifier: Modifier = Modifier, fontScale: Float = 1f) {
    val context = LocalContext.current
    val bodyColor = LocalContentColor.current.toArgb()
    val linkColor = MaterialTheme.colorScheme.primary.toArgb()
    val codeBg = MaterialTheme.colorScheme.surfaceVariant.toArgb()
    val baseSizePx = with(LocalDensity.current) { (16.sp * fontScale).toPx() }

    val markwon = remember(bodyColor, linkColor, codeBg) {
        Markwon.builder(context)
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(TablePlugin.create(context))
            .usePlugin(LinkifyPlugin.create())
            .usePlugin(object : io.noties.markwon.AbstractMarkwonPlugin() {
                override fun configureTheme(builder: MarkwonTheme.Builder) {
                    builder
                        .codeBackgroundColor(codeBg)
                        .codeTextColor(bodyColor)
                        .linkColor(linkColor)
                }
            })
            .build()
    }

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { ctx ->
            TextView(ctx).apply {
                setTextColor(bodyColor)
                textSize = baseSizePx / resources.displayMetrics.scaledDensity
                setLinkTextColor(linkColor)
            }
        },
        update = { textView ->
            textView.setTextColor(bodyColor)
            markwon.setMarkdown(textView, markdown)
        },
    )
}
