#include "llama_engine.h"

#include <chrono>
#include <vector>

namespace {
int64_t now_ms() {
    return std::chrono::duration_cast<std::chrono::milliseconds>(
        std::chrono::steady_clock::now().time_since_epoch()).count();
}
}

LlamaEngineNative::LlamaEngineNative() {
    llama_backend_init();
}

LlamaEngineNative::~LlamaEngineNative() {
    unload();
    llama_backend_free();
}

std::string LlamaEngineNative::load_model(const std::string &path, const LoadConfig &config) {
    std::lock_guard<std::mutex> lock(mutex_);
    unload();

    llama_model_params model_params = llama_model_default_params();
    model_params.n_gpu_layers = config.gpu_layers;

    model_ = llama_model_load_from_file(path.c_str(), model_params);
    if (!model_) {
        return "Unable to load GGUF model. Check that the file is valid and the device has enough RAM.";
    }

    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = static_cast<uint32_t>(config.context_size);
    ctx_params.n_batch = static_cast<uint32_t>(config.batch_size);
    ctx_params.n_threads = config.cpu_threads;
    ctx_params.n_threads_batch = config.cpu_threads;

    ctx_ = llama_init_from_model(model_, ctx_params);
    if (!ctx_) {
        llama_model_free(model_);
        model_ = nullptr;
        return "Unable to create llama.cpp context. Try a smaller context length or model.";
    }

    cancel_requested_ = false;
    return "";
}

std::string LlamaEngineNative::generate(
    const std::string &prompt,
    const GenerationParams &params,
    TokenCallback on_token,
    GenerationMetrics &metrics
) {
    std::lock_guard<std::mutex> lock(mutex_);
    if (!model_ || !ctx_) {
        return "No model is loaded.";
    }
    cancel_requested_ = false;
    llama_set_n_threads(ctx_, params.cpu_threads, params.cpu_threads);

    std::vector<llama_token> prompt_tokens(prompt.size() + 8);
    int token_count = llama_tokenize(
        model_,
        prompt.c_str(),
        static_cast<int32_t>(prompt.size()),
        prompt_tokens.data(),
        static_cast<int32_t>(prompt_tokens.size()),
        true,
        true
    );
    if (token_count < 0) {
        prompt_tokens.resize(static_cast<size_t>(-token_count));
        token_count = llama_tokenize(
            model_,
            prompt.c_str(),
            static_cast<int32_t>(prompt.size()),
            prompt_tokens.data(),
            static_cast<int32_t>(prompt_tokens.size()),
            true,
            true
        );
    }
    if (token_count <= 0) {
        return "Prompt tokenization failed.";
    }
    prompt_tokens.resize(static_cast<size_t>(token_count));

    llama_kv_cache_clear(ctx_);
    const int64_t prompt_start = now_ms();
    llama_batch batch = llama_batch_get_one(prompt_tokens.data(), token_count);
    if (llama_decode(ctx_, batch) != 0) {
        return "Prompt evaluation failed.";
    }
    metrics.prompt_ms = now_ms() - prompt_start;

    llama_sampler_chain_params chain_params = llama_sampler_chain_default_params();
    llama_sampler *sampler = llama_sampler_chain_init(chain_params);
    llama_sampler_chain_add(sampler, llama_sampler_init_penalties(
        64,
        params.repeat_penalty,
        0.0f,
        0.0f
    ));
    llama_sampler_chain_add(sampler, llama_sampler_init_top_k(params.top_k));
    llama_sampler_chain_add(sampler, llama_sampler_init_top_p(params.top_p, 1));
    llama_sampler_chain_add(sampler, llama_sampler_init_min_p(params.min_p, 1));
    llama_sampler_chain_add(sampler, llama_sampler_init_temp(params.temperature));
    llama_sampler_chain_add(sampler, llama_sampler_init_dist(LLAMA_DEFAULT_SEED));

    const int64_t gen_start = now_ms();
    llama_token last_token = 0;
    for (int i = 0; i < params.max_tokens; ++i) {
        if (cancel_requested_) {
            metrics.cancelled = true;
            break;
        }
        last_token = llama_sampler_sample(sampler, ctx_, -1);
        if (llama_token_is_eog(model_, last_token)) {
            break;
        }
        llama_sampler_accept(sampler, last_token);
        std::string piece = token_to_piece(last_token);
        if (!piece.empty()) {
            on_token(piece);
        }
        metrics.output_tokens++;
        llama_batch next = llama_batch_get_one(&last_token, 1);
        if (llama_decode(ctx_, next) != 0) {
            llama_sampler_free(sampler);
            return "Token evaluation failed.";
        }
    }

    metrics.generation_ms = now_ms() - gen_start;
    if (metrics.generation_ms > 0) {
        metrics.tokens_per_second = static_cast<float>(metrics.output_tokens) * 1000.0f /
            static_cast<float>(metrics.generation_ms);
    }
    llama_sampler_free(sampler);
    return "";
}

void LlamaEngineNative::cancel() {
    cancel_requested_ = true;
}

void LlamaEngineNative::unload() {
    if (ctx_) {
        llama_free(ctx_);
        ctx_ = nullptr;
    }
    if (model_) {
        llama_model_free(model_);
        model_ = nullptr;
    }
    cancel_requested_ = false;
}

std::string LlamaEngineNative::token_to_piece(llama_token token) const {
    std::vector<char> buffer(32);
    int length = llama_token_to_piece(model_, token, buffer.data(), static_cast<int32_t>(buffer.size()), 0, true);
    if (length < 0) {
        buffer.resize(static_cast<size_t>(-length));
        length = llama_token_to_piece(model_, token, buffer.data(), static_cast<int32_t>(buffer.size()), 0, true);
    }
    if (length <= 0) {
        return "";
    }
    return std::string(buffer.data(), static_cast<size_t>(length));
}
