#pragma once

#include <cstdint>
#include <string>

struct LoadConfig {
    int32_t context_size = 4096;
    int32_t batch_size = 512;
    int32_t cpu_threads = 4;
    int32_t gpu_layers = 0;
};

struct GenerationParams {
    float temperature = 0.8f;
    int32_t top_k = 40;
    float top_p = 0.95f;
    float min_p = 0.05f;
    float repeat_penalty = 1.1f;
    int32_t max_tokens = 512;
    int32_t cpu_threads = 4;
};

struct GenerationMetrics {
    int64_t prompt_ms = 0;
    int64_t generation_ms = 0;
    int32_t output_tokens = 0;
    float tokens_per_second = 0.0f;
    bool cancelled = false;
};
