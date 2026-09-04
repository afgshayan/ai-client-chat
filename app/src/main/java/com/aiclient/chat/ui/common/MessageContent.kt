package com.aiclient.chat.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private sealed interface MdSegment {
    data class Prose(val markdown: String) : MdSegment
    data class Code(val language: String, val code: String) : MdSegment
}

private val CODE_FENCE_REGEX = Regex("```([a-zA-Z0-9_+#-]*)\\n([\\s\\S]*?)```")

private fun splitSegments(text: String): List<MdSegment> {
    val segments = mutableListOf<MdSegment>()
    var lastEnd = 0
    for (match in CODE_FENCE_REGEX.findAll(text)) {
        if (match.range.first > lastEnd) {
            segments += MdSegment.Prose(text.substring(lastEnd, match.range.first))
        }
        val language = match.groupValues[1]
        val code = match.groupValues[2].trimEnd('\n')
        segments += MdSegment.Code(language, code)
        lastEnd = match.range.last + 1
    }
    if (lastEnd < text.length) {
        segments += MdSegment.Prose(text.substring(lastEnd))
    }
    if (segments.isEmpty()) segments += MdSegment.Prose(text)
    return segments
}

/** Renders a chat message's text as a mix of markdown prose and code-block cards. */
@Composable
fun MessageContent(
    text: String,
    modifier: Modifier = Modifier,
    fontScale: Float = 1f,
    onPreviewArtifact: ((String, String) -> Unit)? = null,
) {
    val segments = remember(text) { splitSegments(text) }
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (segment in segments) {
            when (segment) {
                is MdSegment.Prose -> if (segment.markdown.isNotBlank()) {
                    MarkdownText(segment.markdown, fontScale = fontScale)
                }
                is MdSegment.Code -> CodeBlock(
                    language = segment.language,
                    code = segment.code,
                    onPreview = onPreviewArtifact,
                )
            }
        }
    }
}
