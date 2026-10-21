package com.readmd.app.data

import java.io.File

/**
 * Keeps the last few crash reports as small text files in the app's private storage. Nothing
 * here is sent anywhere; a report leaves the device only if the user chooses to share it.
 */
class CrashStore(private val directory: File, private val keep: Int = MAX_REPORTS) {

    /** Whether new crashes are saved. Read from the crash handler, so it is a plain cached flag. */
    @Volatile
    var enabled: Boolean = true

    /** Saves [report] unless reporting is turned off, then drops the oldest reports beyond the limit. */
    fun save(report: String, nowMillis: Long) {
        if (!enabled) return
        directory.mkdirs()
        File(directory, "crash-$nowMillis.txt").writeText(report, Charsets.UTF_8)
        files().drop(keep).forEach { it.delete() }
    }

    /** The most recent saved report, or null when there is none. */
    fun latest(): String? = files().firstOrNull()?.readText(Charsets.UTF_8)

    /** Deletes every saved report. */
    fun clear() {
        files().forEach { it.delete() }
    }

    /** Report files, newest first. */
    private fun files(): List<File> =
        directory.listFiles { file -> file.isFile && file.name.startsWith("crash-") }
            ?.sortedByDescending { it.name.removePrefix("crash-").removeSuffix(".txt").toLongOrNull() ?: 0L }
            .orEmpty()

    companion object {
        const val MAX_REPORTS = 5
    }
}
