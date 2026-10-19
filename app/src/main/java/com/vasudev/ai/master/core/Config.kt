package com.vasudev.ai.master.core

/**
 * Central configuration for the whole app.
 * (In the reference layout this is the "config.py" file — here it is Config.kt.)
 *
 * Nothing secret lives here. API keys are entered by the user at runtime and
 * kept in encrypted storage (see data/SecurePrefs.kt).
 */
object Config {

    // --- AI provider defaults (user can override in Settings) ---
    const val DEFAULT_BASE_URL = "https://api.openai.com/v1"
    const val DEFAULT_MODEL = "gpt-4o-mini"

    // --- Web search grounding ---
    const val SEARCH_ENDPOINT = "https://api.search.brave.com/res/v1/web/search"

    // --- Local storage names ---
    const val DB_NAME = "master_vault.db"
    const val SECURE_PREFS_NAME = "master_secure_prefs"
    const val DATASTORE_NAME = "master_settings"
    const val TTS_UTTERANCE_ID = "master_utterance"

    // --- Defaults ---
    const val DEFAULT_LANGUAGE = "en-IN"

    // --- On-device (offline) story mode ---
    const val ON_DEVICE_MAX_TOKENS = 1024
    const val ON_DEVICE_TOP_K = 40
    const val ON_DEVICE_MODEL_FILE = "gemma3-1b-it-int4.task"
    // Public, unmodified re-host of Google's gated Gemma-3 1B (int4) .task bundle,
    // so the app can fetch it without each user needing a Hugging Face account.
    // Official source: https://huggingface.co/litert-community/Gemma3-1B-IT
    const val ON_DEVICE_MODEL_URL =
        "https://huggingface.co/nikhil2024/gemma3-1b-it-litert-mirror/resolve/main/gemma3-1b-it-int4.task"
    const val ON_DEVICE_MIN_VALID_BYTES = 200L * 1024 * 1024

    // --- On-device text-to-image (offline Image mode) ---
    // Public, unmodified SD 1.5 bundle converted for the MediaPipe Image Generator.
    // License: CreativeML OpenRAIL-M (see the model page).
    const val IMAGE_MODEL_DIR = "sd15_bins"
    const val IMAGE_MODEL_URL =
        "https://huggingface.co/cuesetac/sd15-mediapipe/resolve/main/sd15-mediapipe.zip"
    const val IMAGE_MODEL_MIN_BYTES = 200L * 1024 * 1024
    const val IMAGE_ITERATIONS = 20

    // --- Keyless fallback image service (no API key) ---
    const val IMAGE_POLLINATIONS_URL = "https://image.pollinations.ai/prompt/"

    // --- Chat session ---
    const val SESSION_ID = 1L
}
