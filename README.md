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

## Privacy and crash reports

ReadMD works offline and has no accounts, ads or analytics. Your documents, names and
reading history stay on your device.

If the app crashes, it saves a small report in its private storage: the app and Android
versions, the device model, the thread and the stack trace. File addresses and paths in the
trace are replaced with `<uri>` and `<path>`, and no document text or file names are
included. **Nothing is sent anywhere.** The next time you open the app it offers to share the
report (through the normal share sheet, so you choose where it goes) or to discard it.

**Opting out:** open the theme menu on the home screen and turn off *Save crash reports*.
Nothing is saved while it is off, and turning it off deletes any report already stored.

### Reading a report

Release builds are minified, so stack traces use short names. Each tagged release uploads its
`mapping.txt` as a workflow artifact (`mapping-<tag>`). Download the one for the version in the
report and run Android's `retrace` tool:

```
retrace mapping.txt crash-report.txt
```

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

## Releasing

Push a tag named `v<major>.<minor>.<patch>` (for example `v1.4.2`, or `v1.4.2-rc1` for a
pre-release). The release workflow reads the version from the tag:

| Tag | versionName | versionCode |
| --- | --- | --- |
| `v1.0.0` | `1.0.0` | `10000` |
| `v1.4.2` | `1.4.2` | `10402` |
| `v2.10.15` | `2.10.15` | `21015` |

`versionCode` is `major * 10000 + minor * 100 + patch`, so every new version is an upgrade for
Android. Minor and patch must stay below 100, and a pre-release shares the code of its final
version. A tag that doesn't match is rejected before anything is built. Local builds are named
`dev` with code `1`; you can set either with `-PversionName=` and `-PversionCode=`.
`bash scripts/test-version-from-tag.sh` checks the rules.

### Signing

Release builds are signed with the key in `release.keystore`, using the environment variables
`ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS` and `ANDROID_KEY_PASSWORD`. The release
workflow restores the keystore and these values from repository secrets. When any of them is
missing (a fork, or a local `gradlew assembleRelease`), the build falls back to the debug key,
so you still get an APK you can install. The workflow sets `RELEASE_SIGNING_REQUIRED=true` and
checks the finished APK, so a published release can never be debug-signed.

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
