package com.readmd.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CrashReportTest {

    private val app = AppInfo(
        versionName = "1.2.3",
        versionCode = 45L,
        androidRelease = "14",
        sdk = 34,
        device = "Google Pixel 8",
    )

    @Test
    fun `content and file addresses are redacted`() {
        assertEquals(
            "Cannot open <uri> now",
            CrashReport.redact("Cannot open content://com.android.externalstorage.documents/tree/primary%3ADocs/document/a.md now"),
        )
        assertEquals("failed: <uri>", CrashReport.redact("failed: file:///storage/emulated/0/secret.md"))
    }

    @Test
    fun `absolute paths are redacted`() {
        assertEquals("No such file: <path>", CrashReport.redact("No such file: /storage/emulated/0/Documents/diary.md"))
        assertEquals("in (<path>", CrashReport.redact("in (/data/user/0/com.readmd.app/files/shared/notes.md)"))
    }

    @Test
    fun `paths containing spaces are redacted whole`() {
        val redacted = CrashReport.redact("No such file: /storage/emulated/0/My Notes/private diary.md (Permission denied)")
        assertEquals("No such file: <path>", redacted)
    }

    @Test
    fun `class names method names and line numbers are kept`() {
        val line = "at com.readmd.app.ui.reader.ReaderViewModel.load(ReaderViewModel.kt:123)"
        assertEquals(line, CrashReport.redact(line))
        assertEquals("java.io.IOException: Cannot read this file", CrashReport.redact("java.io.IOException: Cannot read this file"))
    }

    @Test
    fun `the report lists versions device thread and time`() {
        val report = CrashReport.format(IllegalStateException("boom"), "main", app, nowMillis = 0L)
        assertTrue(report.startsWith("ReadMD crash report\n"))
        assertTrue(report.contains("Time: 1970-01-01T00:00:00Z"))
        assertTrue(report.contains("App: 1.2.3 (45)"))
        assertTrue(report.contains("Android: 14 (API 34)"))
        assertTrue(report.contains("Device: Google Pixel 8"))
        assertTrue(report.contains("Thread: main"))
    }

    @Test
    fun `the stack trace including causes is part of the report`() {
        val error = RuntimeException("outer", IllegalArgumentException("inner"))
        val report = CrashReport.format(error, "worker", app, 0L)
        assertTrue(report.contains("java.lang.RuntimeException: outer"))
        assertTrue(report.contains("Caused by: java.lang.IllegalArgumentException: inner"))
        assertTrue(report.contains("CrashReportTest"))
    }

    @Test
    fun `file names in the error message never reach the report`() {
        val error = java.io.FileNotFoundException("/storage/emulated/0/Documents/private diary.md (Permission denied)")
        val report = CrashReport.format(error, "main", app, 0L)
        assertFalse(report.contains("private diary"))
        assertTrue(report.contains("<path>"))
    }

    @Test
    fun `very long reports are cut`() {
        val error = RuntimeException("x".repeat(CrashReport.MAX_CHARS * 2))
        val report = CrashReport.format(error, "main", app, 0L)
        assertTrue(report.length < CrashReport.MAX_CHARS + 20)
        assertTrue(report.endsWith("(cut)"))
    }
}
