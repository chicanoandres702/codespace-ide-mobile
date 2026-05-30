# 04 — Android Frontend

Native Android, **Kotlin + Jetpack Compose + Material 3**. Min SDK 26 (Android 8.0),
target/compile SDK 34. The concrete source lives in `android/`. This document explains
how the UI is structured and how it maps to features.

---

## 1. Tech choices
| Concern | Choice | Why |
|---------|--------|-----|
| UI toolkit | Jetpack Compose + Material 3 | Modern, less memory than View system, dynamic color |
| Navigation | Navigation-Compose | Single-activity, type-safe routes |
| DI | Hilt | Standard, multi-module friendly |
| Async | Coroutines + Flow | Structured concurrency, backpressure |
| Local DB | Room | SQLite with compile-time safety |
| Prefs | DataStore (Proto) | Async, type-safe settings |
| Network | Retrofit + OkHttp + kotlinx.serialization | REST + SSE + WS |
| Git | JGit | Full on-device git |
| SSH/SFTP | SSHJ | Pure-Kotlin/Java SSH |
| Editor highlighting | tm4e-style TextMate tokenizer | 12+ languages, pluggable grammars |
| Background work | WorkManager + Foreground Service | Sync, backup, terminal keep-alive |
| Security | Jetpack Security / Keystore | Encrypted tokens & AI keys |

---

## 2. Screen map (navigation graph)

```
Splash
 └─ Auth (login / GitHub OAuth)
     └─ Home (workspaces + recent projects)
         ├─ Project Shell  ───────────────────────────┐
         │   ├─ Explorer (file tree, search/replace)   │
         │   ├─ Editor (multi-tab, split, diff)        │
         │   ├─ Terminal (docked + floating)           │
         │   ├─ Source Control (status, commit, PR)    │
         │   ├─ AI Assistant (chat + actions)          │
         │   └─ Tools (preview, DB, JSON, API tester)  │
         ├─ Plugins (marketplace + installed)          │
         └─ Settings (theme, providers, accounts)      ┘
```

The **Project Shell** is a single scaffold with a bottom navigation bar (phone) that
swaps the main pane; a `ModalNavigationDrawer` holds the file explorer; the terminal can
detach into a floating, draggable window.

## 3. Key composables / responsibilities
- `CodeSpaceApp()` — root, theme, nav host.
- `ProjectShellScreen()` — orchestrates panes, handles split-screen state.
- `CodeEditor()` — virtualized editor with `EditorState`, gutter, syntax layer.
- `EditorTabBar()` — scrollable tabs, dirty markers, close/reorder.
- `FileTree()` — lazy, paged, expandable nodes; long-press context menu.
- `TerminalView()` — ANSI renderer over a `TerminalSession` flow; soft + hardware kbd.
- `DiffViewer()` — unified/side-by-side, syntax-aware.
- `AiAssistantPane()` — streaming chat, code-action chips, apply-as-diff.
- `FloatingTerminal()` — `Popup`/overlay window, draggable, resizable, snap-to-edge.

## 4. State management
Each feature has a `ViewModel` exposing an immutable `UiState` via `StateFlow` and a
`onEvent(UiEvent)` reducer. Example:
```kotlin
data class EditorUiState(
    val tabs: List<EditorTab> = emptyList(),
    val activeTabId: String? = null,
    val splitTabId: String? = null,
    val isDark: Boolean = true,
)
sealed interface EditorEvent {
    data class Open(val path: String): EditorEvent
    data class Edit(val tabId: String, val text: String): EditorEvent
    data class Close(val tabId: String): EditorEvent
    data object ToggleSplit: EditorEvent
    data object Save: EditorEvent
}
```

## 5. Mobile optimization
- **Memory:** lazy lists everywhere; file tree pages 200 nodes at a time; editor uses a
  piece-table buffer and only tokenizes the visible viewport + small overscan.
- **3–8 GB tuning:** image/asset downsampling, `largeHeap=false`, manual disposal of
  editor sessions when tabs close, `StrictMode` in debug to catch leaks.
- **Gestures:** swipe between tabs, two-finger tap = command palette, long-press gutter =
  bookmark, pinch = font size, edge-swipe = drawer.
- **Hardware keyboard:** full key-event handling — Ctrl+S save, Ctrl+P quick-open,
  Ctrl+F find, Ctrl+/ comment, Tab/Shift-Tab indent, arrow/Home/End navigation.
- **Offline editing:** all edits autosave to local FS + Room; a `pending_change` queue
  syncs when connectivity returns.
- **Responsive:** `WindowSizeClass` switches phone (bottom nav, single pane) vs.
  large/foldable/landscape (rail + side-by-side panes).

## 6. Theming
`Theme.kt` defines hand-tuned IDE dark & light palettes plus Material 3 dynamic color
(Android 12+). A `LocalEditorColors` provides token colors for the highlighter that
follow the active theme. Toggle persisted in DataStore; respects system setting.

## 7. Accessibility & i18n
- All interactive elements have content descriptions.
- Minimum 48dp touch targets; scalable type.
- Strings externalized to `res/values/strings.xml` for translation.

See `android/app/src/main/java/com/codespace/ide/` for the implemented composables and
ViewModels.
