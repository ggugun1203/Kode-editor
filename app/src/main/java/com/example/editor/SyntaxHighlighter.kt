package com.example.editor

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import com.example.ui.theme.SyntaxAttribute
import com.example.ui.theme.SyntaxComment
import com.example.ui.theme.SyntaxControl
import com.example.ui.theme.SyntaxFunction
import com.example.ui.theme.SyntaxKeyword
import com.example.ui.theme.SyntaxNumber
import com.example.ui.theme.SyntaxString
import com.example.ui.theme.SyntaxTag
import com.example.ui.theme.SyntaxType
import com.example.ui.theme.TextPrimary

object SyntaxHighlighter {

    private val KEYWORDS = setOf(
        "abstract", "as", "break", "case", "catch", "class", "const", "continue",
        "debugger", "default", "delete", "do", "else", "enum", "export", "extends",
        "false", "finally", "for", "from", "function", "if", "implements", "import",
        "in", "instanceof", "interface", "let", "new", "null", "package", "private",
        "protected", "public", "return", "super", "switch", "this", "throw", "true",
        "try", "typeof", "var", "void", "while", "with", "yield", "async", "await",
        "def", "elif", "except", "lambda", "pass", "raise", "val", "fun", "object",
        "typealias", "when", "echo", "die", "exit", "require", "include", "SELECT",
        "FROM", "WHERE", "INSERT", "INTO", "UPDATE", "DELETE", "CREATE", "TABLE",
        "PRIMARY", "KEY", "AUTOINCREMENT", "NOT", "NULL", "ORDER", "BY", "GROUP", "JOIN"
    )

    private val TYPES = setOf(
        "int", "float", "double", "string", "boolean", "bool", "void", "any", "never",
        "unknown", "object", "array", "Int", "String", "Boolean", "Float", "Double",
        "Long", "Short", "Byte", "Char", "List", "Map", "Set", "Array", "TEXT", "INTEGER",
        "REAL", "BLOB", "VARCHAR", "DATETIME", "TIMESTAMP"
    )

    fun highlight(code: String, extension: String): AnnotatedString {
        if (code.length > 50_000) {
            // Safety limit for large files to keep UI ultra smooth
            return AnnotatedString(code)
        }

        return buildAnnotatedString {
            append(code)

            val ext = extension.lowercase()

            when (ext) {
                "html", "htm", "xml" -> highlightXmlHtml(code)
                "json" -> highlightJson(code)
                "css" -> highlightCss(code)
                "sql" -> highlightSql(code)
                else -> highlightGeneric(code)
            }
        }
    }

    private fun AnnotatedString.Builder.highlightGeneric(code: String) {
        // 1. Strings: "..." or '...' or `...`
        val stringRegex = Regex("""("([^"\\]|\\.)*"|'([^'\\]|\\.)*'|`([^`\\]|\\.)*`)""")
        for (m in stringRegex.findAll(code)) {
            addStyle(SpanStyle(color = SyntaxString), m.range.first, m.range.last + 1)
        }

        // 2. Comments: // ... or /* ... */ or # ...
        val commentRegex = Regex("""(//.*|/\*[\s\S]*?\*/|#.*)""")
        for (m in commentRegex.findAll(code)) {
            addStyle(SpanStyle(color = SyntaxComment), m.range.first, m.range.last + 1)
        }

        // 3. Numbers: 123, 0.45, 0xFF
        val numberRegex = Regex("""\b(0x[0-9a-fA-F]+|\d+(\.\d+)?)\b""")
        for (m in numberRegex.findAll(code)) {
            addStyle(SpanStyle(color = SyntaxNumber), m.range.first, m.range.last + 1)
        }

        // 4. Function calls: foo(...)
        val funcRegex = Regex("""\b([a-zA-Z_][a-zA-Z0-9_]*)(?=\s*\()""")
        for (m in funcRegex.findAll(code)) {
            val name = m.groupValues[1]
            if (!KEYWORDS.contains(name)) {
                addStyle(SpanStyle(color = SyntaxFunction), m.range.first, m.range.last + 1)
            }
        }

        // 5. Keywords and Types
        val wordRegex = Regex("""\b[a-zA-Z_][a-zA-Z0-9_]*\b""")
        for (m in wordRegex.findAll(code)) {
            val word = m.value
            if (KEYWORDS.contains(word)) {
                addStyle(SpanStyle(color = SyntaxKeyword, fontWeight = FontWeight.SemiBold), m.range.first, m.range.last + 1)
            } else if (TYPES.contains(word)) {
                addStyle(SpanStyle(color = SyntaxType), m.range.first, m.range.last + 1)
            }
        }
    }

    private fun AnnotatedString.Builder.highlightXmlHtml(code: String) {
        // HTML Comments
        val commentRegex = Regex("""<!--[\s\S]*?-->""")
        for (m in commentRegex.findAll(code)) {
            addStyle(SpanStyle(color = SyntaxComment), m.range.first, m.range.last + 1)
        }

        // Tag names: <div or </div
        val tagRegex = Regex("""(</?)([a-zA-Z0-9_-]+)""")
        for (m in tagRegex.findAll(code)) {
            val tagMatch = m.groups[2]
            if (tagMatch != null) {
                addStyle(SpanStyle(color = SyntaxTag, fontWeight = FontWeight.SemiBold), tagMatch.range.first, tagMatch.range.last + 1)
            }
        }

        // Attributes: class= or id=
        val attrRegex = Regex("""\b([a-zA-Z_:][a-zA-Z0-9_.:-]*)(?=\s*=)""")
        for (m in attrRegex.findAll(code)) {
            addStyle(SpanStyle(color = SyntaxAttribute), m.range.first, m.range.last + 1)
        }

        // Attribute strings
        val strRegex = Regex(""""([^"\\]|\\.)*"|'([^'\\]|\\.)*'""")
        for (m in strRegex.findAll(code)) {
            addStyle(SpanStyle(color = SyntaxString), m.range.first, m.range.last + 1)
        }
    }

    private fun AnnotatedString.Builder.highlightCss(code: String) {
        // Comments
        val commentRegex = Regex("""/\*[\s\S]*?\*/""")
        for (m in commentRegex.findAll(code)) {
            addStyle(SpanStyle(color = SyntaxComment), m.range.first, m.range.last + 1)
        }

        // Properties: color:, font-size:
        val propRegex = Regex("""\b([a-zA-Z-]+)(?=\s*:)""")
        for (m in propRegex.findAll(code)) {
            addStyle(SpanStyle(color = SyntaxAttribute), m.range.first, m.range.last + 1)
        }

        // Colors: #38bdf8
        val hexRegex = Regex("""#[0-9a-fA-F]{3,8}""")
        for (m in hexRegex.findAll(code)) {
            addStyle(SpanStyle(color = SyntaxNumber), m.range.first, m.range.last + 1)
        }

        // Units: 16px, 1.5rem, 100%
        val unitRegex = Regex("""\b\d+(\.\d+)?(px|rem|em|vh|vw|%|s|ms)\b""")
        for (m in unitRegex.findAll(code)) {
            addStyle(SpanStyle(color = SyntaxNumber), m.range.first, m.range.last + 1)
        }
    }

    private fun AnnotatedString.Builder.highlightJson(code: String) {
        // Keys: "key":
        val keyRegex = Regex(""""([^"\\]|\\.)*"(?=\s*:)""")
        for (m in keyRegex.findAll(code)) {
            addStyle(SpanStyle(color = SyntaxAttribute, fontWeight = FontWeight.Medium), m.range.first, m.range.last + 1)
        }

        // String values
        val strRegex = Regex(""":\s*("([^"\\]|\\.)*")""")
        for (m in strRegex.findAll(code)) {
            val grp = m.groups[1]
            if (grp != null) {
                addStyle(SpanStyle(color = SyntaxString), grp.range.first, grp.range.last + 1)
            }
        }

        // Numbers & Booleans
        val boolRegex = Regex("""\b(true|false|null)\b""")
        for (m in boolRegex.findAll(code)) {
            addStyle(SpanStyle(color = SyntaxKeyword), m.range.first, m.range.last + 1)
        }
        val numRegex = Regex("""\b\d+(\.\d+)?\b""")
        for (m in numRegex.findAll(code)) {
            addStyle(SpanStyle(color = SyntaxNumber), m.range.first, m.range.last + 1)
        }
    }

    private fun AnnotatedString.Builder.highlightSql(code: String) {
        val keywords = Regex("""\b(SELECT|FROM|WHERE|INSERT|INTO|VALUES|UPDATE|SET|DELETE|CREATE|TABLE|DROP|ALTER|ADD|PRIMARY|KEY|AUTOINCREMENT|NOT|NULL|DEFAULT|ORDER|BY|GROUP|HAVING|LIMIT|JOIN|LEFT|RIGHT|INNER|OUTER|ON|AND|OR|IN|LIKE|IS|AS|PRAGMA)\b""", RegexOption.IGNORE_CASE)
        for (m in keywords.findAll(code)) {
            addStyle(SpanStyle(color = SyntaxKeyword, fontWeight = FontWeight.Bold), m.range.first, m.range.last + 1)
        }

        // Strings
        val strRegex = Regex("""'([^'\\]|\\.)*'""")
        for (m in strRegex.findAll(code)) {
            addStyle(SpanStyle(color = SyntaxString), m.range.first, m.range.last + 1)
        }

        // Comments
        val commentRegex = Regex("""--.*""")
        for (m in commentRegex.findAll(code)) {
            addStyle(SpanStyle(color = SyntaxComment), m.range.first, m.range.last + 1)
        }
    }
}
