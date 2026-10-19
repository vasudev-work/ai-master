package com.vasudev.ai.master.data.remote

import com.vasudev.ai.master.core.domain
import com.vasudev.ai.master.core.SearchResult

fun systemInstruction(languageTag: String): String = when (languageTag) {
    "hi-IN" -> "आप myAi हैं — भारत में बना एक AI सहायक। सरल, स्पष्ट हिंदी में उत्तर दें। अगर उपयोगकर्ता Hinglish लिखे तो natural Hinglish में ही उत्तर दें।"
    "ta-IN" -> "நீங்கள் myAi — இந்தியாவில் உருவான ஒரு AI உதவியாளர். எளிய, தெளிவான தமிழில் பதிலளியுங்கள்."
    "te-IN" -> "మీరు myAi — భారతదేశంలో తయారైన AI సహాయకుడు. సరళమైన, స్పష్టమైన తెలుగులో సమాధానమివ్వండి."
    "mr-IN" -> "तुम्ही myAi आहात — भारतात बनवलेला AI सहाय्यक. सोप्या, स्पष्ट मराठीत उत्तर दा."
    "bn-IN" -> "আপনি myAi — ভারতে তৈরি একটি AI সহায়ক। সহজ, স্পষ্ট বাংলায় উত্তর দিন।"
    else -> "You are myAi, a fast AI assistant made in India. Reply in clear, simple Indian English. If the user writes Hinglish (Hindi-English code-mix), reply in natural Hinglish."
}

fun buildSystemPrompt(
    ocr: String?,
    labels: List<String>,
    results: List<SearchResult>,
    languageTag: String,
    styleExamples: String = "",
    faceCount: Int = 0
): String =
    buildString {
        append(systemInstruction(languageTag))
        if (styleExamples.isNotBlank()) {
            appendLine()
            appendLine()
            appendLine("The user saved these example stories to define the style they want. When asked to write a story, match their tone, structure and voice, but do not copy them:")
            append(styleExamples.trim().take(4000))
        }
        if (!ocr.isNullOrBlank()) {
            appendLine()
            appendLine()
            append("Text extracted from the user's image (on-device OCR): ")
            append(ocr.take(4000))
        }
        if (labels.isNotEmpty()) {
            appendLine()
            appendLine()
            append("On-device image labels: ")
            append(labels.joinToString(", "))
        }
        if (faceCount > 0) {
            appendLine()
            appendLine()
            append("On-device face count: ")
            append(faceCount)
        }
        if (results.isNotEmpty()) {
            appendLine()
            appendLine()
            appendLine("Fresh web results (mention the source domain in brackets when you use them):")
            results.forEach {
                append("[").append(it.domain()).append("] ")
                append(it.title).append(": ").append(it.snippet)
                appendLine()
            }
        }
    }

fun buildOnDeviceStoryPrompt(styleExamples: String, userText: String): String = buildString {
    appendLine("You are a creative story writer. Write in a clear, natural style.")
    if (styleExamples.isNotBlank()) {
        appendLine()
        appendLine("Here are example stories that show the style, tone and structure to follow:")
        appendLine(styleExamples.trim().take(3000))
        appendLine()
        appendLine("Using the same style, write a brand-new story. Do not copy the examples.")
    }
    appendLine()
    append("User request: ")
    append(userText.trim())
}

fun buildLocalAnswer(ocr: String?, labels: List<String>, faceCount: Int = 0): String = buildString {
    appendLine("On-device mode — this reply used no network.")
    if (!ocr.isNullOrBlank()) {
        appendLine()
        appendLine("Text found in your image (on-device OCR):")
        appendLine(ocr.take(3000))
    }
    if (labels.isNotEmpty()) {
        appendLine()
        appendLine("Detected in the image: " + labels.joinToString(", "))
    }
    if (faceCount > 0) {
        appendLine()
        appendLine("Faces detected: " + faceCount)
    }
    if (ocr.isNullOrBlank() && labels.isEmpty()) {
        appendLine("Private Mode is ON, so I can only analyse images on this device. For full AI answers, turn Private Mode off in Settings.")
    }
}
