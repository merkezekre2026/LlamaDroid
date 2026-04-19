package com.llamadroid.ui.markdown

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

@Composable
fun MarkdownText(text: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        var inCode = false
        val code = StringBuilder()
        text.lines().forEach { line ->
            if (line.trim().startsWith("```")) {
                if (inCode) {
                    CodeBlock(code.toString().trimEnd())
                    code.clear()
                }
                inCode = !inCode
            } else if (inCode) {
                code.appendLine(line)
            } else {
                val trimmed = line.trim()
                if (trimmed.isBlank()) {
                    Text("")
                } else {
                    Text(
                        text = if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) "* ${trimmed.drop(2)}" else trimmed,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        if (code.isNotBlank()) CodeBlock(code.toString().trimEnd())
    }
}

@Composable
private fun CodeBlock(code: String) {
    Text(
        text = code,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp),
        style = MaterialTheme.typography.bodySmall,
        fontFamily = FontFamily.Monospace,
    )
}
