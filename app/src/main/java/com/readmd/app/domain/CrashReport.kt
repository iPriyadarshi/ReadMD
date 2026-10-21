package com.readmd.app.domain

import java.time.Instant

/** Facts about the app and device that go into a crash report; nothing about the user or their files. */
data class AppInfo(
    val versionName: String,
    val versionCode: Long,
    val androidRelease: String,
    val sdk: Int,
    val device: String,
)

/**
 * Builds the text of a crash report. Only technical details are included: app and Android
 * versions, the device model, the thread and the stack trace. Anything in the trace that could
 * identify a file (a content address or a path) is replaced before the report is written.
 */
object CrashReport {

    /** Reports longer than this are cut; the top of a stack trace is what matters. */
    const val MAX_CHARS = 30_000

    private val URI = Regex("(?i)\\b(?:content|file)://\\S+")
    // A path may contain spaces, so everything after its first character up to the end of the line goes.
    private val PATH = Regex("(?m)(?<![\\w.:])/\\S.*$")

    fun redact(text: String): String = text.replace(URI, "<uri>").replace(PATH, "<path>")

    fun format(error: Throwable, threadName: String, app: AppInfo, nowMillis: Long): String {
        val report = buildString {
            appendLine("ReadMD crash report")
            appendLine("Time: ${Instant.ofEpochMilli(nowMillis)}")
            appendLine("App: ${app.versionName} (${app.versionCode})")
            appendLine("Android: ${app.androidRelease} (API ${app.sdk})")
            appendLine("Device: ${app.device}")
            appendLine("Thread: $threadName")
            appendLine()
            append(redact(error.stackTraceToString()))
        }
        return if (report.length <= MAX_CHARS) report else report.take(MAX_CHARS) + "\n… (cut)"
    }
}
