package com.aiclient.chat.ui.common

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString

/**
 * A small, dependency-free syntax highlighter. It won't be as precise as a
 * full grammar-based highlighter (Prism/TextMate), but it covers the common
 * cases (keywords, strings, comments, numbers) across the languages people
 * paste into chat, which is enough to make code blocks readable.
 */
object SyntaxHighlighter {

    private val KEYWORDS = setOf(
        "fun", "val", "var", "class", "object", "interface", "if", "else", "when", "for", "while", "do",
        "return", "break", "continue", "import", "package", "public", "private", "protected", "internal",
        "override", "companion", "suspend", "in", "is", "as", "null", "true", "false", "try", "catch", "finally",
        "throw", "def", "elif", "lambda", "yield", "with", "async", "await", "from", "None", "True", "False",
        "function", "const", "let", "new", "this", "super", "extends", "implements", "static", "void", "typeof",
        "export", "default", "case", "switch", "struct", "enum", "namespace", "using", "include", "template",
        "int", "long", "double", "float", "string", "bool", "char", "byte", "self",
    )

    private val PUNCT_KEYWORD_BOUNDARY = Regex("[A-Za-z_][A-Za-z0-9_]*")
    private val STRING_REGEX = Regex("(\"(?:\\\\.|[^\"\\\\])*\")|('(?:\\\\.|[^'\\\\])*')|(`(?:\\\\.|[^`\\\\])*`)")
    private val NUMBER_REGEX = Regex("\\b\\d+(\\.\\d+)?[fFLl]?\\b")
    private val LINE_COMMENT_REGEX = Regex("(//|#).*")
    private val BLOCK_COMMENT_REGEX = Regex("/\\*[\\s\\S]*?\\*/")

    fun highlight(code: String, keywordColor: Color, stringColor: Color, commentColor: Color, numberColor: Color): AnnotatedString {
        // Collect non-overlapping ranges to color, in priority order: comments > strings > numbers > keywords
        data class Span(val range: IntRange, val color: Color)
        val spans = mutableListOf<Span>()
        val taken = BooleanArray(code.length)

        fun markTaken(range: IntRange) {
            for (i in range) if (i in code.indices) taken[i] = true
        }

        BLOCK_COMMENT_REGEX.findAll(code).forEach { m ->
            spans += Span(m.range, commentColor); markTaken(m.range)
        }
        LINE_COMMENT_REGEX.findAll(code).forEach { m ->
            if (m.range.first < code.length && !taken[m.range.first]) {
                spans += Span(m.range, commentColor); markTaken(m.range)
            }
        }
        STRING_REGEX.findAll(code).forEach { m ->
            if (m.range.first < code.length && !taken[m.range.first]) {
                spans += Span(m.range, stringColor); markTaken(m.range)
            }
        }
        NUMBER_REGEX.findAll(code).forEach { m ->
            if (m.range.first < code.length && !taken[m.range.first]) {
                spans += Span(m.range, numberColor); markTaken(m.range)
            }
        }
        PUNCT_KEYWORD_BOUNDARY.findAll(code).forEach { m ->
            if (m.value in KEYWORDS && m.range.first < code.length && !taken[m.range.first]) {
                spans += Span(m.range, keywordColor); markTaken(m.range)
            }
        }

        return buildAnnotatedString {
            append(code)
            for (span in spans) {
                addStyle(SpanStyle(color = span.color), span.range.first, (span.range.last + 1).coerceAtMost(code.length))
            }
        }
    }
}
