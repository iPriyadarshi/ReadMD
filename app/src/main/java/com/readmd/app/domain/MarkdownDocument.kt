package com.readmd.app.domain

/**
 * A heading extracted from a markdown document, used to build the
 * table of contents. Level is 1..6.
 */
data class HeadingNode(
    val level: Int,
    val text: String,
)

data class MarkdownDocument(
    val id: String,
    val title: String,
    val raw: String,
    val headings: List<HeadingNode>,
)
