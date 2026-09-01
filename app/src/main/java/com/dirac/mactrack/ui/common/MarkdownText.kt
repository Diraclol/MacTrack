package com.dirac.mactrack.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

// A tiny Markdown renderer for chat replies: **bold**, *italic* / _italic_, `code`, bullet lines
// (*, -, +, •), and # headings. Not a full parser -- just enough to render model output cleanly
// instead of showing raw asterisks. Tolerates unbalanced markers (e.g. mid-stream) by printing them
// literally.
@Composable
fun MarkdownText(text: String, color: Color, style: TextStyle, modifier: Modifier = Modifier) {
    val lines = text.trim().split("\n")
    Column(modifier = modifier) {
        lines.forEach { raw ->
            val line = raw.trim()
            when {
                line.isEmpty() -> Spacer(Modifier.height(6.dp))
                line.startsWith("### ") ->
                    Text(parseInline(line.removePrefix("### ")), color = color, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                line.startsWith("## ") ->
                    Text(parseInline(line.removePrefix("## ")), color = color, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                line.startsWith("# ") ->
                    Text(parseInline(line.removePrefix("# ")), color = color, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                isBullet(line) -> Row {
                    Text("•  ", color = color, style = style)
                    Text(parseInline(line.drop(2).trim()), color = color, style = style)
                }
                else -> Text(parseInline(line), color = color, style = style)
            }
        }
    }
}

private fun isBullet(line: String): Boolean =
    line.startsWith("* ") || line.startsWith("- ") || line.startsWith("+ ") || line.startsWith("• ")

private fun parseInline(s: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < s.length) {
        when {
            s.startsWith("**", i) -> {
                val end = s.indexOf("**", i + 2)
                if (end > i + 1) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(s.substring(i + 2, end)) }
                    i = end + 2
                } else {
                    append("**"); i += 2
                }
            }
            s[i] == '`' -> {
                val end = s.indexOf('`', i + 1)
                if (end > i) {
                    withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) { append(s.substring(i + 1, end)) }
                    i = end + 1
                } else {
                    append('`'); i++
                }
            }
            s[i] == '*' || s[i] == '_' -> {
                val ch = s[i]
                val end = s.indexOf(ch, i + 1)
                if (end > i + 1) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(s.substring(i + 1, end)) }
                    i = end + 1
                } else {
                    append(ch); i++
                }
            }
            else -> {
                append(s[i]); i++
            }
        }
    }
}
