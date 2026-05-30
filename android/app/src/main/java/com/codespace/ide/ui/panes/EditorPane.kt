package com.codespace.ide.ui.panes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SplitscreenVertical
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.codespace.ide.domain.EditorTab
import com.codespace.ide.domain.Language
import com.codespace.ide.editor.CodeEditor

/**
 * Multi-tab editor with optional vertical split. Each tab carries its own content,
 * language, and dirty state. This pane wires the [CodeEditor] composable into tabs.
 */
@Composable
fun EditorPane() {
    val tabs = remember {
        mutableStateListOf(
            EditorTab(
                id = "t1", path = "src/index.ts", name = "index.ts",
                content = SAMPLE_TS, language = Language.TYPESCRIPT,
            ),
            EditorTab(
                id = "t2", path = "main.py", name = "main.py",
                content = SAMPLE_PY, language = Language.PYTHON,
            ),
        )
    }
    var activeId by remember { mutableStateOf("t1") }
    var splitId by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize()) {
        // Tab bar
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .background(MaterialTheme.colorScheme.surface),
        ) {
            tabs.forEach { tab ->
                Row(
                    Modifier
                        .clickable { activeId = tab.id }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Text(
                        (if (tab.isDirty) "● " else "") + tab.name,
                        fontWeight = if (tab.id == activeId) FontWeight.Bold else FontWeight.Normal,
                    )
                    IconButton(onClick = { tabs.remove(tab) }, modifier = Modifier.padding(0.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.padding(2.dp))
                    }
                }
            }
            IconButton(onClick = { splitId = if (splitId == null) activeId else null }) {
                Icon(Icons.Default.SplitscreenVertical, contentDescription = "Split")
            }
        }
        HorizontalDivider()

        val active = tabs.firstOrNull { it.id == activeId } ?: tabs.firstOrNull()
        Column(Modifier.fillMaxSize()) {
            active?.let { tab ->
                CodeEditor(
                    content = tab.content,
                    language = tab.language,
                    onContentChange = { newText ->
                        val idx = tabs.indexOfFirst { it.id == tab.id }
                        if (idx >= 0) tabs[idx] = tab.copy(content = newText, isDirty = true)
                    },
                    modifier = Modifier.weight(1f),
                )
                val split = splitId?.let { id -> tabs.firstOrNull { it.id == id } }
                if (split != null) {
                    HorizontalDivider()
                    CodeEditor(
                        content = split.content,
                        language = split.language,
                        onContentChange = {},
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

private val SAMPLE_TS = """
// CodeSpace IDE — TypeScript sample
interface User {
  id: string;
  name: string;
}

async function greet(user: User): Promise<string> {
  const message = `Hello, ${'$'}{user.name}!`; // template string
  return message;
}

greet({ id: "1", name: "Ada" }).then(console.log);
""".trimIndent()

private val SAMPLE_PY = """
# CodeSpace IDE — Python sample
def fibonacci(n: int) -> list[int]:
    seq = [0, 1]
    while len(seq) < n:
        seq.append(seq[-1] + seq[-2])
    return seq[:n]

if __name__ == "__main__":
    print(fibonacci(10))
""".trimIndent()
