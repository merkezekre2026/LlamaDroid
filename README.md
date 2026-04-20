# LlamaDroid

**Run large language models locally on your Android device — no internet, no cloud, no limits.**

LlamaDroid brings on-device AI inference to Android by embedding [llama.cpp](https://github.com/ggml-org/llama.cpp) directly into a native Kotlin app. Load any GGUF model, chat in real time, and keep every conversation completely private.

---

## Features

- **100% offline** — zero `INTERNET` permission; all data stays on your device
- **GGUF model support** — import any compatible model file (Llama, Mistral, Phi, Gemma, Qwen, and more)
- **Real-time streaming** — token-by-token output with cancel support and partial response saving
- **Conversation history** — persistent chats stored locally via Room database
- **Inference profiles** — Balanced, Fast, and Quality presets with fine-grained overrides
- **Advanced sampling** — temperature, top-k, top-p, min-p, repeat penalty
- **Performance tuning** — context size, batch size, CPU threads, GPU layers
- **Benchmark screen** — measure tokens/second and evaluate model performance
- **Markdown rendering** — assistant messages rendered with formatting
- **Theming** — system, light, and dark mode support
- **Haptics & screen-on** — configurable UX preferences

---

## Requirements

| Item | Minimum |
|---|---|
| Android | 8.0 (API 26) |
| Architecture | `arm64-v8a` or `x86_64` |
| Storage | Varies by model (1 GB – 20 GB+) |
| RAM | Varies by model and context size |

---

## Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/<your-username>/LlamaDroid.git
cd LlamaDroid
git submodule update --init --recursive
```

> The llama.cpp submodule lives at `third_party/llama.cpp`. The CMake build will fail immediately if it is missing — run the submodule command before opening the project.

### 2. Open in Android Studio

Open the project root in **Android Studio** (Hedgehog or later). Let Gradle sync finish, then **Build → Make Project**.

**Prerequisites:**
- Android SDK 36
- Android NDK (installed via SDK Manager)
- CMake (installed via SDK Manager)

### 3. Run on a device

Connect a physical device running Android 8.0+ with `arm64-v8a` architecture (most modern phones) and click **Run**. The emulator works too but inference will be CPU-only and slow.

### 4. Import a model

1. Open the **Models** tab
2. Tap **Import GGUF**
3. Pick a `.gguf` file from your device storage
4. LlamaDroid copies it to app-private storage and reads its metadata
5. Set the model as active, then open the **Chat** tab to start a conversation

No model is bundled with the app. Good starting points:
- [Hugging Face GGUF models](https://huggingface.co/models?library=gguf)
- Llama 3.2 3B · Mistral 7B · Phi-3 Mini · Gemma 3 — all available in quantized GGUF form

---

## Project Structure

```
LlamaDroid/
├── app/src/main/
│   ├── java/com/llamadroid/
│   │   ├── data/            # Room DB, DataStore, repositories
│   │   ├── domain/          # Use cases, models, business logic
│   │   ├── nativebridge/    # JNI wrapper + Kotlin coroutine integration
│   │   └── ui/              # Compose screens and ViewModels
│   └── cpp/
│       ├── llama_jni.cpp    # JNI boundary
│       ├── llama_engine.cpp # Native inference logic
│       └── CMakeLists.txt
└── third_party/
    └── llama.cpp/           # Git submodule
```

**Architecture:** Clean Architecture (Domain / Data / UI) with MVVM, Jetpack Compose, and a thin JNI native bridge.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin 2.2, C++17 |
| UI | Jetpack Compose, Material3 |
| Navigation | Navigation Compose |
| Persistence | Room 2.8, DataStore |
| Concurrency | Kotlin Coroutines + Flow |
| Native inference | llama.cpp (via CMake + NDK) |
| Build | AGP 8.13, KSP, Gradle version catalog |

---

## Inference Settings

| Setting | Description |
|---|---|
| **Profile** | Balanced / Fast / Quality preset |
| **Temperature** | Randomness of sampling (0 = deterministic) |
| **Top-K** | Limit vocabulary to top K tokens |
| **Top-P** | Nucleus sampling threshold |
| **Min-P** | Minimum probability threshold |
| **Repeat Penalty** | Penalize recently used tokens |
| **Context Size** | Maximum token context window |
| **Batch Size** | Tokens processed per batch |
| **CPU Threads** | Thread count for inference |
| **GPU Layers** | Layers offloaded to GPU (0 = CPU only) |

---

## GPU Acceleration

Set **GPU Layers** > 0 in Settings to offload model layers to the GPU. Actual acceleration depends on your device's GPU, driver support, and the llama.cpp backend compiled into the build. If inference crashes or produces incorrect output, set GPU Layers to 0 to fall back to CPU-only mode.

---

## llama.cpp Integration

The native wrapper is intentionally thin:

- Kotlin calls `LlamaEngine` through `LlamaNativeEngine`, which serializes load/generate/unload operations and exposes token streaming as `Flow<GenerationEvent>`.
- `llama_jni.cpp` maps Kotlin data classes to native structs and dispatches callbacks.
- `llama_engine.cpp` owns llama.cpp model/context/sampler resources.

llama.cpp APIs evolve quickly. If a future submodule revision renames APIs, only `llama_engine.cpp` needs to change — the Kotlin layer stays untouched.

---

## Privacy

LlamaDroid has no `INTERNET` permission. Prompts, responses, conversations, settings, and imported model metadata all remain on the device.

---

## Known Limitations

- **KV cache reuse** is conservative — the current native wrapper clears the cache and replays the prompt on each turn for correctness. Session-level cache reuse can be added to the C++ layer without changing any UI or repository code.
- **Native crash resistance** depends on llama.cpp behavior for the selected model and available device memory.
- Gradle wrapper files are not committed to this repo; Android Studio will generate them on first sync.

---

## Contributing

Pull requests are welcome. For large changes, please open an issue first to discuss the approach.

1. Fork the repo and create a feature branch
2. Make your changes with clear, focused commits
3. Ensure the project builds and runs on a real device
4. Open a pull request against `main`

---

## Acknowledgements

- [llama.cpp](https://github.com/ggml-org/llama.cpp) by Georgi Gerganov — the inference engine powering LlamaDroid
- The open-source model community on Hugging Face
