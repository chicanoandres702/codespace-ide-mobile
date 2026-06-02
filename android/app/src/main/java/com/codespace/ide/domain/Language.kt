package com.codespace.ide.domain

/** Languages with first-class syntax highlighting support. */
enum class Language(val displayName: String, val extensions: List<String>) {
    JAVASCRIPT("JavaScript", listOf("js", "jsx", "mjs", "cjs")),
    TYPESCRIPT("TypeScript", listOf("ts", "tsx")),
    KOTLIN("Kotlin", listOf("kt", "kts")),
    PYTHON("Python", listOf("py", "pyw")),
    HTML("HTML", listOf("html", "htm")),
    CSS("CSS", listOf("css", "scss", "sass", "less")),
    JSON("JSON", listOf("json", "jsonc")),
    MARKDOWN("Markdown", listOf("md", "markdown")),
    JAVA("Java", listOf("java")),
    CPP("C++", listOf("cpp", "cc", "cxx", "hpp")),
    C("C", listOf("c", "h")),
    GO("Go", listOf("go")),
    RUST("Rust", listOf("rs")),
    PHP("PHP", listOf("php")),
    SHELL("Shell", listOf("sh", "bash", "zsh")),
    XML("XML", listOf("xml", "svg", "plist")),
    PLAINTEXT("Plain Text", emptyList()),
    PLAIN("Plain Text", emptyList());

    companion object {
        fun fromPath(path: String): Language {
            val ext = path.substringAfterLast('.', "").lowercase()
            return entries.firstOrNull { ext in it.extensions } ?: PLAINTEXT
        }
    }
}
