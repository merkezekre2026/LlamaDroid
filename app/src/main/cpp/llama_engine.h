#pragma once

#include "llama_types.h"
#include "llama.h"

#include <atomic>
#include <functional>
#include <memory>
#include <mutex>
#include <string>

class LlamaEngineNative {
public:
    using TokenCallback = std::function<void(const std::string &)>;

    LlamaEngineNative();
    ~LlamaEngineNative();

    std::string load_model(const std::string &path, const LoadConfig &config);
    std::string generate(const std::string &prompt, const GenerationParams &params, TokenCallback on_token, GenerationMetrics &metrics);
    void cancel();
    void unload();

private:
    std::mutex mutex_;
    std::atomic_bool cancel_requested_{false};
    llama_model *model_ = nullptr;
    const llama_vocab *vocab_ = nullptr;
    llama_context *ctx_ = nullptr;

    std::string token_to_piece(llama_token token) const;
};
