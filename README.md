# ReadMD

A simple, focused Markdown reader for Android.

## Features

- **Rendered Markdown** — headings, bold/italic, strikethrough, inline code,
  syntax-highlighted fenced code blocks (prism4j: Kotlin, Java, Python,
  JavaScript, JSON, SQL, YAML, C/C++, C#, Go, Swift, Scala, Markdown,
  HTML/CSS and more — 22 languages total; others render as plain code),
  lists, task lists, tables, links, and images
- **Open from anywhere** — pick a single file or a whole folder
  (Storage Access Framework, no runtime permissions), open `.md` files from
  other apps via *Open with → ReadMD*, or try the bundled samples
- **Recent files** — quick access to what you read before
- **Folder browsing** — recursive scan with a subfolder-count badge and
  sort options (name A–Z/Z–A, newest/oldest first), persisted across launches
- **Table of contents** — jump to any heading in long documents
- **In-document search** — find and cycle through matches with highlights
- **Raw view** — toggle to the original Markdown source
- **Mermaid diagrams** — a "View diagram" link above each ```` ```mermaid ```` block opens
  it rendered (bundled mermaid 11.4.1, MIT licensed, works offline)
- **Themes** — light/dark/system, with dynamic color (Android 12+)

## Tech stack

| Piece | Choice |
| --- | --- |
| Language | Kotlin (AGP 9 built-in Kotlin) |
| UI | Jetpack Compose + Material 3 |
| Markdown | [Markwon](https://github.com/noties/Markwon) (CommonMark-based) |
| Persistence | Preferences DataStore |
| Min SDK | 26 (Android 8.0) |
| Build | Gradle 9.8, AGP 9.4 |

## Project layout

```
app/src/main/java/com/readmd/app/
  data/        DocumentRepository (SAF + bundled), FolderScanner, RecentFilesStore
  domain/      MarkdownOutlineParser (TOC extraction, unit tested)
  di/          AppContainer (manual dependency container)
  ui/
    home/      file/folder pickers, recents, samples, theme menu
    reader/    Markwon rendering bridge, TOC drawer, search, raw view
    theme/     Material 3 theme with light/dark/dynamic color
```

## Building

Open the project in Android Studio (or run from a terminal):

```
gradlew :app:assembleDebug
```

The debug APK lands in `app/build/outputs/apk/debug/`.

## Tests

Unit tests (JVM, no device needed):

```
gradlew :app:testDebugUnitTest
```

UI tests drive the real app with Jetpack Compose test APIs. They open the bundled
"Welcome to ReadMD" sample and cover the home screen, the contents drawer, in-document
search and the raw/rendered toggle. They need a connected device or a running emulator
(API 26 or newer):

```
gradlew :app:connectedDebugAndroidTest
```

They are in `app/src/androidTest` and read their expected text from the app's string
resources, so they pass in any device language. They share the app's saved data (recents,
settings), so run them on a test device or emulator rather than your daily phone.
