package com.codespace.ide.editor

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.codespace.ide.domain.Language
import com.codespace.ide.ui.EditorColors

/** Applies syntax highlighting as a Compose VisualTransformation (offsets preserved). */
class SyntaxTransformation(
    private val language: Language,
    private val colors: EditorColors,
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val highlighted = SyntaxHighlighter.highlight(text.text, language, colors)
        return TransformedText(highlighted, OffsetMapping.Identity)
    }
}
