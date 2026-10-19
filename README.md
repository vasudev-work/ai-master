# myAi

Made in India by Vasudev Dangaya.

A fast, multilingual, multimodal AI assistant for Android:
- Streaming AI chat (any OpenAI-compatible API: OpenAI, Groq, OpenRouter, Ollama...)
- Smart Lens: camera/gallery image analysis with on-device OCR (Latin + Devanagari scripts), image labels, and face count
- Voice input and output with Indian language support (en-IN, hi-IN, ta-IN, te-IN, mr-IN, bn-IN)
- Hinglish code-mixing handled naturally
- Web search grounding (Brave Search API - Bing Search API was retired by Microsoft in Aug 2025)
- Local Vault: chat history in a local database, API keys in encrypted storage, Private Mode for 100% offline use
- Four answer modes: Ask, Web (cited), Study (tutor) and Research (multi-source cited report)
- Memory: save what the assistant should remember about you (stored only on-device, editable/deletable)
- Quick action chips: Translate, Summarize, Web
- UPI support link for the developer (vasudevupi@upi)

## Tech stack
Kotlin, Jetpack Compose (Material 3), Hilt, Room, DataStore, Ktor (SSE streaming), ML Kit, Coroutines + Flow.

Application package: `com.vasudev.ai.master`

## Project structure
See [PROJECT_STRUCTURE.md](PROJECT_STRUCTURE.md) for the full file-by-file layout.
Short version:

- `core/` — domain models, central `Config.kt`, shared helpers (`Utils.kt`)
- `data/` — Room database, network layer (`remote/`), speech, ML Kit vision, encrypted prefs, settings, image vault, and the on-device LLM (`ondevice/`)
- `di/` — Hilt dependency-injection bindings
- `ui/` — Jetpack Compose screens (chat, settings, about, theme)
- `res/` — strings for 6 languages, drawables, themes

## Answer modes

Pick a mode above the input box in the chat screen:

- **Ask** — normal chat.
- **Web** — forces a web search and asks the model to cite the source domain for each claim.
- **Study** — a patient tutor: explains in small steps, gives an example, then asks practice questions.
- **Research** — Deep Research: the app plans 3 search queries, runs them, then writes a structured report
  (summary, key findings, uncertainty, sources) with bracketed citations. Needs a Brave Search API key.

## Memory

In **Settings → Memory** you can write anything you want the assistant to remember (for example
"I am a student; reply in simple Hindi"). It is stored only on the device, is added to every prompt,
and you can clear the box and tap Save to forget it.

## On-device story mode (offline)

You can save 2–3 example stories inside the app (Settings → *Story style / examples*)
and have new stories written **on the phone itself, with no internet**.

- Enable it in **Settings → On-device story mode**.
- The first time, download the model (~555 MB) or import a `.task` file you already
  have. It is cached in app-private storage and then runs fully offline.
- The model is Gemma-3 1B (int4) in MediaPipe `.task` format. Official source:
  `https://huggingface.co/litert-community/Gemma3-1B-IT` (gated — by downloading
  you agree to the Gemma Terms of Use). The app's default URL points to a public,
  unmodified re-host so no Hugging Face account is needed.
- Quality and speed depend on the phone. On a mid-range device it runs on the CPU
  and can be slow; a cloud model (GPT/Gemini) still gives better text.

## First run
1. Open the project in Android Studio (Ladybug or newer).
2. Let it sync (Gradle 8.9, AGP 8.7.3, Kotlin 2.0.21 - all download automatically).
3. Run on a device/emulator (minSdk 26, targetSdk 35).
4. In Settings, enter your AI provider API key (base URL defaults to OpenAI; Groq/OpenRouter also work).
5. Optional: add a Brave Search API key for web-grounded answers.

## Build the APK
- Debug: Build > Build APK(s), or `./gradlew assembleDebug`
- Release: Build > Generate Signed Bundle / APK, create a keystore, pick the release variant.
  Keep the keystore outside the repo - losing it means you can never update the app on Play Store.
- No Android Studio? Push the repo to GitHub and run the included workflow
  (`.github/workflows/build-apk.yml`). It builds the debug APK on GitHub's servers
  and hands you a downloadable artifact.

## Notes
- The chat database is local (Room). For full at-rest encryption, add SQLCipher
  (net.zetetic:sqlcipher-android) as described in the companion spec document.
- Icons are Material icons (Apache 2.0) bundled as vector drawables.
- No secrets ship inside the APK: every key is user-entered and stored in
  EncryptedSharedPreferences backed by the Android Keystore.
