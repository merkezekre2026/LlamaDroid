# LlamaDroid

LlamaDroid is a local-only Android chat app for GGUF models powered by llama.cpp. It is a native Kotlin + Jetpack Compose project with a JNI/CMake bridge to llama.cpp, Room persistence for chats and model metadata, and DataStore-backed inference settings.

## Folder Tree

```text
LlamaDroid/
  settings.gradle.kts
  build.gradle.kts
  gradle/libs.versions.toml
  app/
    build.gradle.kts
    src/main/
      AndroidManifest.xml
      java/com/llamadroid/
        app/
        data/
        domain/
        nativebridge/
        ui/
      cpp/
        CMakeLists.txt
        llama_engine.cpp
        llama_engine.h
        llama_jni.cpp
        llama_types.h
      res/
  third_party/
    llama.cpp/
```

## What Is Implemented

- Offline-first Compose chat UI with streaming assistant output.
- Local Room persistence for conversations, messages, and imported models.
- DataStore persistence for inference, performance, haptics, screen, and theme settings.
- SAF-based GGUF import. Imported files are copied into app-owned storage so llama.cpp can load them by file path.
- JNI `LlamaEngine` abstraction with model loading, generation, cancellation, unload, and release.
- CMake integration against `third_party/llama.cpp`.
- Model picker, settings screen, conversation list, benchmark screen, markdown-style chat rendering, copy/edit/regenerate/stop actions.
- No network permission, analytics, login, or backend integration.

## Build Setup

1. Install Android Studio with Gradle 8.13 support, Android SDK 36, Android NDK, and CMake.
2. Initialize llama.cpp:

   ```bash
   git submodule update --init --recursive
   ```

   If the submodule is not present yet:

   ```bash
   git submodule add https://github.com/ggml-org/llama.cpp.git third_party/llama.cpp
   git submodule update --init --recursive
   ```

3. Open the repository in Android Studio.
4. Sync Gradle.
5. Build and run the `app` configuration on an `arm64-v8a` Android device.

The native build intentionally fails fast if `third_party/llama.cpp/CMakeLists.txt` is missing.

## Importing GGUF Models

1. Open the Models tab.
2. Tap `Import GGUF`.
3. Pick a `.gguf` file through Android's system file picker.
4. LlamaDroid persists read permission, copies the file into app storage, derives basic metadata from the filename, and lets you set it active.
5. Tap `Load model`, then return to Chat.

No model is bundled with the app.

## llama.cpp Integration Notes

The native wrapper is intentionally thin:

- Kotlin calls `LlamaEngine`.
- `LlamaNativeEngine` serializes model load/generate/unload operations and exposes token streaming as `Flow<GenerationEvent>`.
- `llama_jni.cpp` maps Kotlin data classes to native structs and dispatches callbacks.
- `llama_engine.cpp` owns llama.cpp model/context/sampler resources.

llama.cpp APIs evolve quickly. If a future submodule revision renames sampler/model APIs, keep Kotlin unchanged and update only `app/src/main/cpp/llama_engine.cpp`.

## GPU Offload

`gpuLayers` is exposed in settings and passed into `llama_model_params.n_gpu_layers`. Actual acceleration depends on the llama.cpp Android backend enabled by the submodule/build and the device GPU/driver. Unsupported devices should use `0` GPU layers.

## Privacy

LlamaDroid has no `INTERNET` permission. Prompts, outputs, chats, settings, and imported model metadata remain on the device.

## Known Limitations

- KV cache reuse is conservative: the current native wrapper clears cache and replays the prompt for correctness. The wrapper is isolated so chat-session cache reuse can be added without changing UI or repositories.
- The UI includes the core management flows, but rename/system-prompt editing can be expanded into richer dialogs.
- Native crash resistance depends on llama.cpp behavior for the selected model and device memory pressure.
- Gradle wrapper files are not included in this scaffold; Android Studio can generate or use its bundled Gradle runtime.
