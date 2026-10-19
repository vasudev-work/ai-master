package com.vasudev.ai.master.core

/**
 * The way the assistant answers. Selected in the chat screen and used to pick
 * the right system prompt / workflow on the backend call.
 */
enum class ChatMode(val label: String) {
    /** Normal chat. */
    ASK("Ask"),

    /** Force a web search and cite the sources. */
    WEB("Web"),

    /** Tutor style: explain step by step, ask questions, give practice. */
    STUDY("Study"),

    /** Multi-step research across several searches, producing a cited report. */
    RESEARCH("Research")
}
