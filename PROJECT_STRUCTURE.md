# Project structure — master

This document maps the general "how to organise an AI project" layout onto this
Android app, file by file. Items that do not apply to a pure Android client are
marked **N/A** with the reason.

Package: `com.vasudev.ai.master`

```
master/
├── README.md                     # project intro + setup
├── PROJECT_STRUCTURE.md          # this file
├── .env.example                  # reference template for keys (see note below)
├── .gitignore                    # keeps build junk / secrets out of git
├── build.gradle.kts              # project-level Gradle config
├── settings.gradle.kts           # module list + repos
├── gradle/                       # wrapper + version catalog
│   ├── libs.versions.toml        # all dependency versions in one place
│   └── wrapper/
└── app/
    ├── build.gradle.kts          # module Gradle config (namespace, deps)
    ├── proguard-rules.pro        # release shrink/obfuscation rules
    └── src/main/
        ├── AndroidManifest.xml   # permissions + app/activity registration
        ├── java/com/vasudev/ai/master/
        │   ├── MainActivity.kt   # single Activity host (Compose entry point)
        │   ├── MyAiApp.kt        # Application class (@HiltAndroidApp)
        │   ├── core/             # models + config + helpers  (the "brain glue")
        │   │   ├── Models.kt     # domain data classes (ChatMessage, SearchResult)
        │   │   ├── Config.kt     # central config  (reference layout: config.py)
        │   │   └── Utils.kt      # shared helpers  (reference layout: utils.py)
        │   ├── data/             # everything that touches storage or the network
        │   │   ├── db/Db.kt          # Room entities, DAO, database
        │   │   ├── remote/           # network layer (reference: AiApiService)
        │   │   │   ├── LlmApi.kt     # streaming LLM client (Ktor)
        │   │   │   ├── SearchApi.kt  # web-search grounding client
        │   │   │   └── Prompts.kt    # system prompts + local-answer builder
        │   │   ├── speech/           # STT + TTS engines
        │   │   ├── mlkit/VisionEngine.kt  # on-device OCR + image labels + face count
        │   │   ├── ondevice/         # on-device (offline) story generation
        │   │   │   ├── OnDeviceModel.kt    # model file location + metadata
        │   │   │   ├── ModelRepository.kt  # download / import the .task model
        │   │   │   └── OnDeviceEngine.kt   # MediaPipe LLM Inference wrapper
        │   │   ├── SecurePrefs.kt    # encrypted API-key storage
        │   │   ├── SettingsStore.kt  # DataStore-backed UI settings
        │   │   └── ImageVault.kt     # app-private image storage
        │   ├── di/AppModule.kt   # Hilt bindings (HttpClient, DB, Json)
        │   └── ui/               # Jetpack Compose screens
        │       ├── AppRoot.kt        # navigation host
        │       ├── chat/             # ChatScreen + ChatViewModel
        │       ├── settings/         # SettingsScreen + SettingsViewModel
        │       ├── about/            # AboutScreen
        │       └── theme/            # Material 3 colour scheme
        └── res/                  # strings (6 languages), drawables, themes
```

## Mapping to the reference layout

| Reference item | Here | Notes |
|---|---|---|
| `main.py` / `app.py` | `MainActivity.kt` + `MyAiApp.kt` | Entry points of the app. |
| `model.py` | `data/remote/LlmApi.kt` + `data/mlkit/VisionEngine.kt` | Model calls (LLM streaming) and on-device ML. |
| `dataset.py` / `data_loader.py` | **N/A** | No local training set; the app calls a hosted model. |
| `train.py` | **N/A** | Training happens on the provider side. |
| `predict.py` / `inference.py` | `LlmApi.completeOnce` / `streamChat` | Inference logic. |
| `config.py` | `core/Config.kt` | Central constants. |
| `requirements.txt` | `gradle/libs.versions.toml` | Dependency list + versions. |
| `MainActivity.kt` | `MainActivity.kt` | Present. |
| `AiApiService` / `RetrofitClient` | `data/remote/` | Uses Ktor instead of Retrofit — same role. |
| `activity_main.xml` | **N/A** | UI is Jetpack Compose, not XML layouts. |
| `AndroidManifest.xml` | `AndroidManifest.xml` | Present. |
| `build.gradle` | `build.gradle.kts` (root + app) | Present. |
| `model.tflite` | `data/ondevice/` | On-device text model (Gemma-3 1B, MediaPipe `.task`) is downloaded at runtime, not bundled. Vision uses ML Kit. |
| `utils.py` / `helpers.py` | `core/Utils.kt` | Shared helpers. |
| `pipeline.py` | **N/A** | No offline data pipeline. |
| `.env` | `.env.example` | Keys are entered at runtime, not bundled. |
| `.gitignore` | `.gitignore` | Present. |
| `README.md` | `README.md` | Present. |
| `Dockerfile` | **N/A** | An Android app is not containerised. |

## About secrets

Nothing secret ships in the APK. API keys are typed by the user in **Settings**
and stored with `EncryptedSharedPreferences` backed by the Android Keystore.
`.env.example` is a reference template only — the app does not read a `.env` file.
