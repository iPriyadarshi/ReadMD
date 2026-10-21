package com.readmd.app.domain

/** Classifies links that leave the document, to decide how to open them. */
object ExternalLinks {

    private val WEB = Regex("^https?://\\S+$", RegexOption.IGNORE_CASE)

    /** True for http and https links, which open in an in-app browser tab. */
    fun isWeb(link: String): Boolean = WEB.matches(link.trim())

    /** Links that stay inside the current document (`#heading`) and need no app to open them. */
    fun isInPage(link: String): Boolean = link.trim().startsWith("#")
}
