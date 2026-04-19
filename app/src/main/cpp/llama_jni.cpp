#include "llama_engine.h"

#include <jni.h>
#include <memory>
#include <string>

namespace {
LlamaEngineNative *from_handle(jlong handle) {
    return reinterpret_cast<LlamaEngineNative *>(handle);
}

std::string jstring_to_string(JNIEnv *env, jstring value) {
    const char *chars = env->GetStringUTFChars(value, nullptr);
    std::string result(chars ? chars : "");
    env->ReleaseStringUTFChars(value, chars);
    return result;
}

jint int_field(JNIEnv *env, jobject obj, const char *name) {
    jclass cls = env->GetObjectClass(obj);
    jfieldID field = env->GetFieldID(cls, name, "I");
    return env->GetIntField(obj, field);
}

jfloat float_field(JNIEnv *env, jobject obj, const char *name) {
    jclass cls = env->GetObjectClass(obj);
    jfieldID field = env->GetFieldID(cls, name, "F");
    return env->GetFloatField(obj, field);
}

LoadConfig read_load_config(JNIEnv *env, jobject obj) {
    LoadConfig config;
    config.context_size = int_field(env, obj, "contextSize");
    config.batch_size = int_field(env, obj, "batchSize");
    config.cpu_threads = int_field(env, obj, "cpuThreads");
    config.gpu_layers = int_field(env, obj, "gpuLayers");
    return config;
}

GenerationParams read_generation_params(JNIEnv *env, jobject obj) {
    GenerationParams params;
    params.temperature = float_field(env, obj, "temperature");
    params.top_k = int_field(env, obj, "topK");
    params.top_p = float_field(env, obj, "topP");
    params.min_p = float_field(env, obj, "minP");
    params.repeat_penalty = float_field(env, obj, "repeatPenalty");
    params.max_tokens = int_field(env, obj, "maxTokens");
    params.cpu_threads = int_field(env, obj, "cpuThreads");
    return params;
}

jstring nullable_error(JNIEnv *env, const std::string &error) {
    if (error.empty()) return nullptr;
    return env->NewStringUTF(error.c_str());
}
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_llamadroid_nativebridge_LlamaNativeBindings_createEngine(JNIEnv *, jobject) {
    return reinterpret_cast<jlong>(new LlamaEngineNative());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_llamadroid_nativebridge_LlamaNativeBindings_loadModel(
    JNIEnv *env,
    jobject,
    jlong handle,
    jstring path,
    jobject config
) {
    auto *engine = from_handle(handle);
    if (!engine) return env->NewStringUTF("Invalid native engine handle.");
    std::string error = engine->load_model(jstring_to_string(env, path), read_load_config(env, config));
    return nullable_error(env, error);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_llamadroid_nativebridge_LlamaNativeBindings_generate(
    JNIEnv *env,
    jobject,
    jlong handle,
    jstring prompt,
    jobject params_obj,
    jobject callback
) {
    auto *engine = from_handle(handle);
    if (!engine) return env->NewStringUTF("Invalid native engine handle.");

    jclass callback_class = env->GetObjectClass(callback);
    jmethodID on_token = env->GetMethodID(callback_class, "onToken", "(Ljava/lang/String;)V");
    jmethodID on_metrics = env->GetMethodID(callback_class, "onMetrics", "(JJIF)V");
    jmethodID on_complete = env->GetMethodID(callback_class, "onComplete", "()V");
    jmethodID on_cancelled = env->GetMethodID(callback_class, "onCancelled", "()V");
    jmethodID on_error = env->GetMethodID(callback_class, "onError", "(Ljava/lang/String;)V");

    GenerationMetrics metrics;
    std::string error = engine->generate(
        jstring_to_string(env, prompt),
        read_generation_params(env, params_obj),
        [&](const std::string &piece) {
            jstring token = env->NewStringUTF(piece.c_str());
            env->CallVoidMethod(callback, on_token, token);
            env->DeleteLocalRef(token);
        },
        metrics
    );

    if (!error.empty()) {
        jstring message = env->NewStringUTF(error.c_str());
        env->CallVoidMethod(callback, on_error, message);
        env->DeleteLocalRef(message);
        return env->NewStringUTF(error.c_str());
    }

    env->CallVoidMethod(callback, on_metrics,
        static_cast<jlong>(metrics.prompt_ms),
        static_cast<jlong>(metrics.generation_ms),
        static_cast<jint>(metrics.output_tokens),
        static_cast<jfloat>(metrics.tokens_per_second)
    );
    if (metrics.cancelled) {
        env->CallVoidMethod(callback, on_cancelled);
    } else {
        env->CallVoidMethod(callback, on_complete);
    }
    return nullptr;
}

extern "C" JNIEXPORT void JNICALL
Java_com_llamadroid_nativebridge_LlamaNativeBindings_cancel(JNIEnv *, jobject, jlong handle) {
    if (auto *engine = from_handle(handle)) {
        engine->cancel();
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_llamadroid_nativebridge_LlamaNativeBindings_unloadModel(JNIEnv *, jobject, jlong handle) {
    if (auto *engine = from_handle(handle)) {
        engine->unload();
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_llamadroid_nativebridge_LlamaNativeBindings_release(JNIEnv *, jobject, jlong handle) {
    delete from_handle(handle);
}
