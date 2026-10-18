package com.readmd.app.ui.reader

import io.noties.prism4j.annotations.PrismBundle

/**
 * Triggers prism4j-bundler (kapt) to generate [Prism4jLocator], a
 * [io.noties.prism4j.GrammarLocator] for the grammars listed below.
 * The bundler pulls in each grammar's dependencies automatically
 * (e.g. `clike` for `c`/`cpp`/`java`/`javascript`).
 *
 * Language set is limited to what prism4j-bundler 2.0.0 ships
 * (`languages/Prism_*.java` in its jar); code fences with other
 * languages render as plain code blocks.
 */
@PrismBundle(
    grammarLocatorClassName = ".Prism4jLocator",
    include = [
        "c",
        "cpp",
        "csharp",
        "css",
        "css_extras",
        "dart",
        "git",
        "go",
        "groovy",
        "java",
        "javascript",
        "json",
        "kotlin",
        "latex",
        "makefile",
        "markdown",
        "markup",
        "python",
        "scala",
        "sql",
        "swift",
        "yaml",
    ],
)
internal class PrismBundleInfo
