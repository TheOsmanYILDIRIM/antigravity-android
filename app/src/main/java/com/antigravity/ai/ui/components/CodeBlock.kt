package com.antigravity.ai.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.ai.ui.theme.*

@Composable
fun CodeBlock(
    code: String,
    language: String = "code",
    fontSizeSp: Float = 12f
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, CodeBlockBorder, RoundedCornerShape(10.dp))
            .background(CodeBlockBackground)
    ) {
        // Code Block Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF161B22))
                .padding(horizontal = 12.dp, vertical = 5.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Colored dot indicator
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(PrimaryIndigo)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = language.ifEmpty { "code" }.lowercase(),
                    fontFamily = FontFamily.Monospace,
                    fontSize = (fontSizeSp * 0.9f).sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary
                )
            }

            IconButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("code", code)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Kod kopyalandı", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Kopyala",
                    tint = TextSecondary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        // Code content with syntax highlighting
        val highlightedCode = highlightCode(code, language)
        Text(
            text = highlightedCode,
            fontFamily = FontFamily.Monospace,
            fontSize = fontSizeSp.sp,
            lineHeight = (fontSizeSp * 1.4f).sp,
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        )
    }
}

/**
 * Lightweight, fast regex-based syntax highlighter for CLI and common languages
 */
fun highlightCode(code: String, language: String): AnnotatedString {
    return buildAnnotatedString {
        append(code)

        val fullText = code

        // 1. Comments (# ... or // ...)
        val commentRegex = Regex("(#.*|//.*|/\\*[\\s\\S]*?\\*/)")
        for (match in commentRegex.findAll(fullText)) {
            addStyle(
                SpanStyle(color = Color(0xFF8B949E), fontStyle = FontStyle.Italic),
                match.range.first,
                match.range.last + 1
            )
        }

        // 2. Strings ("..." or '...' or `...`)
        val stringRegex = Regex("(\"[^\"]*\"|'[^']*'|`[^`]*`)")
        for (match in stringRegex.findAll(fullText)) {
            addStyle(
                SpanStyle(color = Color(0xFFA5D6FF)), // Light Cyan / String
                match.range.first,
                match.range.last + 1
            )
        }

        // 3. Keywords & Commands
        val keywords = listOf(
            "export", "alias", "echo", "cat", "cpulimit", "nohup", "pkill", "killall",
            "val", "var", "fun", "class", "import", "package", "data", "object", "interface",
            "if", "else", "then", "fi", "case", "esac", "for", "in", "while", "return",
            "const", "let", "function", "async", "await", "import", "from",
            "pkg", "apt", "git", "bash", "tmux", "node", "python", "agy"
        )
        val keywordPattern = Regex("\\b(" + keywords.joinToString("|") + ")\\b")
        for (match in keywordPattern.findAll(fullText)) {
            // Do not override comments/strings
            addStyle(
                SpanStyle(color = Color(0xFFFF7B72), fontWeight = FontWeight.SemiBold), // Coral / Keyword
                match.range.first,
                match.range.last + 1
            )
        }

        // 4. Variables & Parameters ($VAR, --flag, -f)
        val varPattern = Regex("(\\$[a-zA-Z0-9_]+|--[a-zA-Z0-9_-]+|-[a-zA-Z0-9])")
        for (match in varPattern.findAll(fullText)) {
            addStyle(
                SpanStyle(color = Color(0xFFFFA657)), // Orange / Var & Flag
                match.range.first,
                match.range.last + 1
            )
        }

        // 5. Numbers & Booleans
        val numPattern = Regex("\\b(\\d+|true|false|null)\\b")
        for (match in numPattern.findAll(fullText)) {
            addStyle(
                SpanStyle(color = Color(0xFF79C0FF)), // Sky Blue / Number
                match.range.first,
                match.range.last + 1
            )
        }
    }
}
